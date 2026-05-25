package com.foosball.domain;

import java.util.Optional;

/**
 * Time-window filter for {@code GET /games/{token}} and
 * {@code GET /pointsPrPlayer/{token}}. Wire tokens are lowercase.
 * {@link #fromToken(String)} returns {@link Optional#empty()} for
 * unrecognized tokens; callers decide whether to 404 or fall through
 * (e.g. {@code /games/{token}} treats unknown tokens as a player name).
 */
public enum Period {
    HOUR("hour", 1),
    DAY("day", 24),
    WEEK("week", 7 * 24),
    MONTH("month", 31 * 24),
    ALLTIME("alltime", 1_000_000);

    public final String token;
    public final int hoursBack;

    Period(String token, int hoursBack) {
        this.token = token;
        this.hoursBack = hoursBack;
    }

    public static Optional<Period> fromToken(String token) {
        if (token == null) return Optional.empty();
        for (Period p : values()) {
            if (p.token.equals(token)) return Optional.of(p);
        }
        return Optional.empty();
    }
}
