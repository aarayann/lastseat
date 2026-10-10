package com.example.bookingengine.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.example.bookingengine.domain.Decision;
import com.example.bookingengine.domain.Outcome;
import com.example.bookingengine.domain.RuleType;

import org.springframework.stereotype.Component;

@Component
public class RuleEngine {

    private final Map<RuleType, Rule> rulesByType = new EnumMap<>(RuleType.class);

    public RuleEngine(List<Rule> rules) {
        for (Rule rule : rules) {
            rulesByType.put(rule.type(), rule);
        }
    }

    public Rule ruleFor(RuleType type) {
        Rule rule = rulesByType.get(type);
        if (rule == null) {
            throw new IllegalStateException("No implementation for rule type " + type);
        }
        return rule;
    }

    public EvaluationOutcome evaluate(EvaluationContext context, List<ActiveRule> rules) {
        Decision decision = Decision.ALLOW;
        List<TraceEntry> trace = new ArrayList<>();
        RuleType stoppedBy = null;
        int order = 1;

        for (ActiveRule active : rules) {
            Rule rule = ruleFor(active.type());

            if (stoppedBy != null && rule.needsDatabase()) {
                trace.add(new TraceEntry(order++, active.type(), active.onFail(), Outcome.SKIPPED, null,
                        "skipped after hard deny by " + stoppedBy));
                continue;
            }

            RuleResult result = rule.evaluate(context, active.params());
            trace.add(new TraceEntry(order++, active.type(), active.onFail(), result.outcome(),
                    result.reasonCode(), result.detail()));

            if (result.failed()) {
                if (active.onFail().isMoreSevereThan(decision)) {
                    decision = active.onFail();
                }
                if (active.onFail() == Decision.DENY && stoppedBy == null) {
                    stoppedBy = active.type();
                }
            }
        }
        return new EvaluationOutcome(decision, trace);
    }

    public static List<ActiveRule> sortForEvaluation(List<ActiveRule> rules) {
        List<ActiveRule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator
                .comparingInt((ActiveRule rule) -> rule.onFail() == Decision.DENY ? 0 : 1)
                .thenComparingInt(ActiveRule::priority)
                .thenComparing(ActiveRule::id));
        return sorted;
    }
}
