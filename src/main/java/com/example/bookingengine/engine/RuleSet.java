package com.example.bookingengine.engine;

import java.util.List;

public record RuleSet(long version, List<ActiveRule> rules) {
}
