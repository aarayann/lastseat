package com.example.bookingengine.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.example.bookingengine.TestData;
import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.Direction;
import com.example.bookingengine.domain.Gender;
import com.example.bookingengine.domain.Outcome;
import com.example.bookingengine.domain.RuleType;

import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private final EvaluationContext context = TestData.context(
            TestData.employee(Gender.MALE, "09:00", "18:00"),
            TestData.request(Direction.LOGIN, "09:00", false),
            "2026-10-12T07:00");

    private static Rule fixedRule(RuleType type, boolean passes) {
        return fakeRule(type, passes, false);
    }

    private static Rule databaseRule(RuleType type, boolean passes) {
        return fakeRule(type, passes, true);
    }

    private static Rule fakeRule(RuleType type, boolean passes, boolean needsDatabase) {
        return new Rule() {
            @Override
            public RuleType type() {
                return type;
            }

            @Override
            public RuleResult evaluate(EvaluationContext context, RuleParams params) {
                return passes ? RuleResult.pass("forced pass") : RuleResult.fail(type.name(), "forced fail");
            }

            @Override
            public boolean needsDatabase() {
                return needsDatabase;
            }
        };
    }

    private static ActiveRule active(long id, RuleType type, int priority, Decision onFail) {
        return new ActiveRule(id, type, priority, onFail, RuleParams.empty());
    }

    @Test
    void allRulesPassMeansAllow() {
        RuleEngine engine = new RuleEngine(List.of(
                fixedRule(RuleType.SHIFT_WINDOW, true),
                fixedRule(RuleType.CUTOFF, true)));

        EvaluationOutcome outcome = engine.evaluate(context, List.of(
                active(1, RuleType.SHIFT_WINDOW, 10, Decision.DENY),
                active(2, RuleType.CUTOFF, 20, Decision.DENY)));

        assertEquals(Decision.ALLOW, outcome.decision());
        assertTrue(outcome.failures().isEmpty());
    }

    @Test
    void onlyASoftFailureMeansNeedsApproval() {
        RuleEngine engine = new RuleEngine(List.of(
                fixedRule(RuleType.SHIFT_WINDOW, true),
                fixedRule(RuleType.NIGHT_SAFETY, false)));

        EvaluationOutcome outcome = engine.evaluate(context, List.of(
                active(1, RuleType.SHIFT_WINDOW, 10, Decision.DENY),
                active(3, RuleType.NIGHT_SAFETY, 30, Decision.NEEDS_APPROVAL)));

        assertEquals(Decision.NEEDS_APPROVAL, outcome.decision());
        assertEquals("NIGHT_SAFETY", outcome.failures().get(0).reasonCode());
    }

    @Test
    void hardDenyBeatsSoftFailureEvenWhenTheSoftRuleRunsFirst() {
        RuleEngine engine = new RuleEngine(List.of(
                fixedRule(RuleType.NIGHT_SAFETY, false),
                fixedRule(RuleType.CUTOFF, false)));

        EvaluationOutcome outcome = engine.evaluate(context, List.of(
                active(3, RuleType.NIGHT_SAFETY, 1, Decision.NEEDS_APPROVAL),
                active(2, RuleType.CUTOFF, 20, Decision.DENY)));

        assertEquals(Decision.DENY, outcome.decision());
    }

    @Test
    void databaseRulesAfterAHardDenyAreSkipped() {
        RuleEngine engine = new RuleEngine(List.of(
                fixedRule(RuleType.SHIFT_WINDOW, true),
                fixedRule(RuleType.CUTOFF, false),
                databaseRule(RuleType.DUPLICATE, true)));

        EvaluationOutcome outcome = engine.evaluate(context, List.of(
                active(1, RuleType.SHIFT_WINDOW, 10, Decision.DENY),
                active(2, RuleType.CUTOFF, 20, Decision.DENY),
                active(4, RuleType.DUPLICATE, 40, Decision.DENY)));

        assertEquals(Outcome.PASS, outcome.trace().get(0).outcome());
        assertEquals(Outcome.FAIL, outcome.trace().get(1).outcome());
        assertEquals(Outcome.SKIPPED, outcome.trace().get(2).outcome());
    }

    @Test
    void cheapRulesStillRunAfterAHardDenySoEveryReasonIsReported() {
        RuleEngine engine = new RuleEngine(List.of(
                fixedRule(RuleType.SHIFT_WINDOW, false),
                fixedRule(RuleType.NIGHT_SAFETY, false),
                databaseRule(RuleType.DUPLICATE, true)));

        EvaluationOutcome outcome = engine.evaluate(context, List.of(
                active(1, RuleType.SHIFT_WINDOW, 10, Decision.DENY),
                active(3, RuleType.NIGHT_SAFETY, 30, Decision.DENY),
                active(4, RuleType.DUPLICATE, 40, Decision.DENY)));

        assertEquals(Decision.DENY, outcome.decision());
        assertEquals(2, outcome.failures().size());
        assertEquals(Outcome.SKIPPED, outcome.trace().get(2).outcome());
    }

    @Test
    void verdictNeverDependsOnRuleOrder() {
        RuleEngine engine = new RuleEngine(List.of(
                fixedRule(RuleType.SHIFT_WINDOW, true),
                fixedRule(RuleType.CUTOFF, false),
                fixedRule(RuleType.NIGHT_SAFETY, false),
                databaseRule(RuleType.DUPLICATE, true),
                databaseRule(RuleType.COST_CENTRE_QUOTA, true)));

        List<ActiveRule> rules = new ArrayList<>(List.of(
                active(1, RuleType.SHIFT_WINDOW, 10, Decision.DENY),
                active(2, RuleType.CUTOFF, 20, Decision.DENY),
                active(3, RuleType.NIGHT_SAFETY, 30, Decision.NEEDS_APPROVAL),
                active(4, RuleType.DUPLICATE, 40, Decision.DENY),
                active(5, RuleType.COST_CENTRE_QUOTA, 50, Decision.DENY)));

        Random random = new Random(42);
        for (int i = 0; i < 100; i++) {
            Collections.shuffle(rules, random);
            assertEquals(Decision.DENY, engine.evaluate(context, rules).decision());
        }
    }

    @Test
    void aCrashingRuleFailsClosed() {
        Rule crashing = new AbstractRule() {
            @Override
            public RuleType type() {
                return RuleType.DUPLICATE;
            }

            @Override
            protected RuleResult check(EvaluationContext context, RuleParams params) {
                throw new IllegalStateException("database is down");
            }
        };
        RuleEngine engine = new RuleEngine(List.of(crashing));

        EvaluationOutcome outcome = engine.evaluate(context, List.of(active(4, RuleType.DUPLICATE, 40, Decision.DENY)));

        assertEquals(Decision.DENY, outcome.decision());
        assertEquals("RULE_ERROR", outcome.failures().get(0).reasonCode());
    }

    @Test
    void sortPutsHardRulesFirstThenPriorityThenId() {
        List<ActiveRule> sorted = RuleEngine.sortForEvaluation(List.of(
                active(5, RuleType.NIGHT_SAFETY, 1, Decision.NEEDS_APPROVAL),
                active(3, RuleType.CUTOFF, 20, Decision.DENY),
                active(2, RuleType.SHIFT_WINDOW, 20, Decision.DENY),
                active(1, RuleType.DUPLICATE, 10, Decision.DENY)));

        assertEquals(1L, sorted.get(0).id());
        assertEquals(2L, sorted.get(1).id());
        assertEquals(3L, sorted.get(2).id());
        assertEquals(5L, sorted.get(3).id());
    }
}
