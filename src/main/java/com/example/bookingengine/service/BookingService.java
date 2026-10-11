package com.example.bookingengine.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.example.bookingengine.domain.Booking;
import com.example.bookingengine.domain.BookingStatus;
import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.DecisionLog;
import com.example.bookingengine.domain.Employee;
import com.example.bookingengine.dto.BookingRequest;
import com.example.bookingengine.dto.BookingView;
import com.example.bookingengine.dto.DecisionResponse;
import com.example.bookingengine.engine.EvaluationContext;
import com.example.bookingengine.engine.EvaluationOutcome;
import com.example.bookingengine.engine.RuleEngine;
import com.example.bookingengine.engine.RuleSet;
import com.example.bookingengine.engine.TraceEntry;
import com.example.bookingengine.exception.BadRequestException;
import com.example.bookingengine.exception.ForbiddenException;
import com.example.bookingengine.exception.NotFoundException;
import com.example.bookingengine.repository.BookingRepository;
import com.example.bookingengine.repository.CostCentreRepository;
import com.example.bookingengine.repository.DecisionLogRepository;
import com.example.bookingengine.repository.EmployeeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final EmployeeRepository employees;
    private final CostCentreRepository costCentres;
    private final BookingRepository bookings;
    private final DecisionLogRepository decisionLogs;
    private final RuleConfigService ruleConfigService;
    private final RuleEngine engine;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meters;

    public BookingService(EmployeeRepository employees, CostCentreRepository costCentres,
                          BookingRepository bookings, DecisionLogRepository decisionLogs,
                          RuleConfigService ruleConfigService, RuleEngine engine, Clock clock,
                          ObjectMapper objectMapper, MeterRegistry meters) {
        this.employees = employees;
        this.costCentres = costCentres;
        this.bookings = bookings;
        this.decisionLogs = decisionLogs;
        this.ruleConfigService = ruleConfigService;
        this.engine = engine;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.meters = meters;
    }

    @Transactional(readOnly = true)
    public DecisionResponse evaluate(BookingRequest request, String username, boolean isAdmin, boolean explain) {
        Employee employee = loadEmployee(request, username, isAdmin);
        RuleSet ruleSet = ruleConfigService.activeRules(employee.getClientId(), employee.getCostCentreId());
        EvaluationContext context = contextFor(employee, request);
        EvaluationOutcome outcome = runRules(context, ruleSet);
        return DecisionResponse.of(outcome, null, ruleSet.version(), context.now(), explain);
    }

    @Transactional
    public DecisionResponse book(BookingRequest request, String username, boolean isAdmin, boolean explain) {
        Employee employee = loadEmployee(request, username, isAdmin);

        costCentres.lockById(employee.getCostCentreId())
                .orElseThrow(() -> new NotFoundException("Cost-centre", employee.getCostCentreId()));

        RuleSet ruleSet = ruleConfigService.activeRules(employee.getClientId(), employee.getCostCentreId());
        EvaluationContext context = contextFor(employee, request);
        EvaluationOutcome outcome = runRules(context, ruleSet);

        Booking booking = null;
        if (outcome.decision() != Decision.DENY) {
            BookingStatus status = outcome.decision() == Decision.ALLOW
                    ? BookingStatus.CONFIRMED
                    : BookingStatus.PENDING_APPROVAL;
            booking = bookings.save(new Booking(employee, request, status, reasonCodes(outcome),
                    ruleSet.version(), context.now()));
        }
        recordDecision(employee, outcome, ruleSet.version(), context.now());
        return DecisionResponse.of(outcome, booking, ruleSet.version(), context.now(), explain);
    }

    @Transactional(readOnly = true)
    public BookingView get(Long bookingId, String username, boolean isAdmin) {
        Booking booking = findBooking(bookingId);
        checkOwner(booking.getEmployeeId(), username, isAdmin);
        return BookingView.from(booking);
    }

    @Transactional
    public BookingView cancel(Long bookingId, String username, boolean isAdmin) {
        Booking booking = findBooking(bookingId);
        checkOwner(booking.getEmployeeId(), username, isAdmin);
        if (booking.cancel()) {
            meters.counter("booking.cancellations").increment();
            log.info("booking={} cancelled; its seat is free again", bookingId);
        }
        return BookingView.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingView> pendingApprovals() {
        return bookings.findByStatusOrderByCreatedAtAsc(BookingStatus.PENDING_APPROVAL).stream()
                .map(BookingView::from)
                .toList();
    }

    @Transactional
    public BookingView decideApproval(Long bookingId, boolean approve) {
        Booking booking = findBooking(bookingId);
        if (approve) {
            booking.approve();
        } else {
            booking.reject();
        }
        meters.counter("approval.outcomes", "result", approve ? "approved" : "rejected").increment();
        log.info("booking={} {}", bookingId, approve ? "approved" : "rejected");
        return BookingView.from(booking);
    }

    private Employee loadEmployee(BookingRequest request, String username, boolean isAdmin) {
        if (request.pickup().trim().equalsIgnoreCase(request.drop().trim())) {
            throw new BadRequestException("VALIDATION_FAILED", "pickup and drop must be different places");
        }
        Employee employee = employees.findById(request.employeeId())
                .orElseThrow(() -> new NotFoundException("Employee", request.employeeId()));
        checkOwner(employee.getId(), username, isAdmin);
        return employee;
    }

    private void checkOwner(Long employeeId, String username, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        Employee caller = employees.findByUsername(username)
                .orElseThrow(() -> new ForbiddenException("User " + username + " has no employee profile"));
        if (!caller.getId().equals(employeeId)) {
            throw new ForbiddenException("You can only act on your own bookings");
        }
    }

    private Booking findBooking(Long bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking", bookingId));
    }

    private EvaluationContext contextFor(Employee employee, BookingRequest request) {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        return new EvaluationContext(employee, request, now);
    }

    private EvaluationOutcome runRules(EvaluationContext context, RuleSet ruleSet) {
        Timer.Sample sample = Timer.start(meters);
        EvaluationOutcome outcome = engine.evaluate(context, ruleSet.rules());
        sample.stop(meters.timer("rule.engine.evaluation"));
        return outcome;
    }

    private void recordDecision(Employee employee, EvaluationOutcome outcome, long version, LocalDateTime evaluatedAt) {
        String reasons = reasonCodes(outcome);
        decisionLogs.save(new DecisionLog(MDC.get("requestId"), employee.getId(), outcome.decision(), reasons,
                toJson(outcome.trace()), version, evaluatedAt));

        meters.counter("booking.decisions", "decision", outcome.decision().name()).increment();
        for (TraceEntry failure : outcome.failures()) {
            meters.counter("rule.violations", "rule", failure.reasonCode()).increment();
        }
        log.info("decision={} reasons={} employeeId={} configVersion={}",
                outcome.decision(), reasons.isEmpty() ? "-" : reasons, employee.getId(), version);
    }

    private String reasonCodes(EvaluationOutcome outcome) {
        return outcome.failures().stream()
                .map(TraceEntry::reasonCode)
                .collect(Collectors.joining(","));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[trace could not be written]";
        }
    }
}
