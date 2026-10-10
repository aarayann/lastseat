package com.example.bookingengine.config;

import java.time.LocalTime;

import com.example.bookingengine.domain.ConfigVersion;
import com.example.bookingengine.domain.CostCentre;
import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.Employee;
import com.example.bookingengine.domain.Gender;
import com.example.bookingengine.domain.RuleConfig;
import com.example.bookingengine.domain.RuleType;
import com.example.bookingengine.repository.ConfigVersionRepository;
import com.example.bookingengine.repository.CostCentreRepository;
import com.example.bookingengine.repository.EmployeeRepository;
import com.example.bookingengine.repository.RuleConfigRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String CLIENT = "acme";

    private final CostCentreRepository costCentres;
    private final EmployeeRepository employees;
    private final RuleConfigRepository rules;
    private final ConfigVersionRepository versions;

    public DataSeeder(CostCentreRepository costCentres, EmployeeRepository employees,
                      RuleConfigRepository rules, ConfigVersionRepository versions) {
        this.costCentres = costCentres;
        this.employees = employees;
        this.rules = rules;
        this.versions = versions;
    }

    @Override
    public void run(String... args) {
        if (employees.count() > 0) {
            return;
        }

        CostCentre engineering = costCentres.save(new CostCentre(CLIENT, "Engineering"));
        CostCentre operations = costCentres.save(new CostCentre(CLIENT, "Operations"));

        employees.save(new Employee("Aarav Mehta", "aarav", CLIENT, "Platform", engineering.getId(),
                Gender.MALE, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        employees.save(new Employee("Priya Sharma", "priya", CLIENT, "Platform", engineering.getId(),
                Gender.FEMALE, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        employees.save(new Employee("Meera Iyer", "meera", CLIENT, "Support", engineering.getId(),
                Gender.FEMALE, LocalTime.of(14, 0), LocalTime.of(23, 0)));
        employees.save(new Employee("Rohan Das", "rohan", CLIENT, "Logistics", operations.getId(),
                Gender.MALE, LocalTime.of(22, 0), LocalTime.of(6, 0)));
        employees.save(new Employee("Kavya Nair", "kavya", CLIENT, "Logistics", operations.getId(),
                Gender.FEMALE, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        employees.save(new Employee("Ishaan Gupta", "ishaan", CLIENT, "Logistics", operations.getId(),
                Gender.MALE, LocalTime.of(9, 0), LocalTime.of(18, 0)));

        rules.save(new RuleConfig(CLIENT, null, RuleType.SHIFT_WINDOW, true, 10, Decision.DENY,
                "{\"graceMinutes\": 30}"));
        rules.save(new RuleConfig(CLIENT, null, RuleType.CUTOFF, true, 20, Decision.DENY,
                "{\"minutesBefore\": 60}"));
        rules.save(new RuleConfig(CLIENT, null, RuleType.NIGHT_SAFETY, true, 30, Decision.DENY,
                "{\"nightStart\": \"20:00\", \"nightEnd\": \"06:00\"}"));
        rules.save(new RuleConfig(CLIENT, null, RuleType.DUPLICATE, true, 40, Decision.DENY,
                "{}"));
        rules.save(new RuleConfig(CLIENT, null, RuleType.COST_CENTRE_QUOTA, true, 50, Decision.DENY,
                "{\"seatsPerDay\": 20}"));
        rules.save(new RuleConfig(CLIENT, operations.getId(), RuleType.COST_CENTRE_QUOTA, true, 50, Decision.DENY,
                "{\"seatsPerDay\": 2}"));

        versions.save(new ConfigVersion(CLIENT, 1));
        log.info("Seeded 2 cost-centres, 6 employees and 6 rules for client {}", CLIENT);
    }
}
