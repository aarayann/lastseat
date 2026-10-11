package com.example.bookingengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.example.bookingengine.domain.BookingStatus;
import com.example.bookingengine.domain.CostCentre;
import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.Direction;
import com.example.bookingengine.domain.Employee;
import com.example.bookingengine.domain.Gender;
import com.example.bookingengine.domain.RuleConfig;
import com.example.bookingengine.domain.RuleType;
import com.example.bookingengine.dto.BookingRequest;
import com.example.bookingengine.repository.BookingRepository;
import com.example.bookingengine.repository.CostCentreRepository;
import com.example.bookingengine.repository.EmployeeRepository;
import com.example.bookingengine.repository.RuleConfigRepository;
import com.example.bookingengine.service.BookingService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class QuotaConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CostCentreRepository costCentres;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private RuleConfigRepository ruleConfigs;

    @Autowired
    private BookingRepository bookings;

    @Test
    void twoPeopleOneSeat() throws Exception {
        race(2);
    }

    @Test
    void twentyPeopleOneSeat() throws Exception {
        race(20);
    }

    private void race(int people) throws Exception {
        CostCentre costCentre = costCentres.save(new CostCentre("acme", "Race " + System.nanoTime()));
        ruleConfigs.save(new RuleConfig("acme", costCentre.getId(), RuleType.COST_CENTRE_QUOTA, true, 50,
                Decision.DENY, "{\"seatsPerDay\": 1}"));

        List<Employee> racers = new ArrayList<>();
        for (int i = 0; i < people; i++) {
            racers.add(employees.save(new Employee("Racer " + i, "racer-" + costCentre.getId() + "-" + i, "acme",
                    "QA", costCentre.getId(), Gender.MALE, LocalTime.of(9, 0), LocalTime.of(18, 0))));
        }
        LocalDate day = LocalDate.now().plusDays(3);

        ExecutorService pool = Executors.newFixedThreadPool(people);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Decision>> results = new ArrayList<>();

        for (Employee racer : racers) {
            BookingRequest request = new BookingRequest(racer.getId(), day, Direction.LOGIN,
                    LocalTime.of(9, 0), "Home", "Office", false, false, false);
            results.add(pool.submit(() -> {
                startSignal.await();
                return bookingService.book(request, "test", true, false).decision();
            }));
        }
        startSignal.countDown();

        int allowed = 0;
        int denied = 0;
        for (Future<Decision> result : results) {
            if (result.get(30, TimeUnit.SECONDS) == Decision.ALLOW) {
                allowed++;
            } else {
                denied++;
            }
        }
        pool.shutdown();

        assertEquals(1, allowed);
        assertEquals(people - 1, denied);
        assertEquals(1, bookings.countByCostCentreIdAndBookingDateAndDirectionAndStatusIn(
                costCentre.getId(), day, Direction.LOGIN, BookingStatus.ACTIVE));
    }
}
