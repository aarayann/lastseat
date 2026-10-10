package com.example.bookingengine.domain;

/**
 * The three possible verdicts, declared from least to most severe.
 * The order matters: the engine keeps the most severe one it sees.
 */
public enum Decision {
    ALLOW,
    NEEDS_APPROVAL,
    DENY;

    public boolean isMoreSevereThan(Decision other) {
        return this.ordinal() > other.ordinal();
    }
}
