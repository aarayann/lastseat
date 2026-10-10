package com.example.bookingengine.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRule implements Rule {

    private static final Logger log = LoggerFactory.getLogger(AbstractRule.class);

    @Override
    public final RuleResult evaluate(EvaluationContext context, RuleParams params) {
        try {
            return check(context, params);
        } catch (RuntimeException e) {
            log.error("Rule {} crashed, treating it as failed", type(), e);
            return RuleResult.fail("RULE_ERROR", type() + " could not be evaluated: " + e.getClass().getSimpleName());
        }
    }

    protected abstract RuleResult check(EvaluationContext context, RuleParams params);
}
