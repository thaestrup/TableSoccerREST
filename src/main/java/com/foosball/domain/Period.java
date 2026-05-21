package com.foosball.domain;

import java.util.Optional;

/**
 * Time-window filter shared by {@code GET /games/{token}} and
 * {@code GET /pointsPrPlayer/{token}}.
 *
 * <p>The wire token is lowercase (matches what the React frontend sends).
 * Conversion is via {@link #fromToken(String)} which returns
 * {@link Optional#empty()} for unknown tokens — callers decide whether
 * to fall through to a player-name lookup (legacy behavior on
 * {@code /games/{token}}) or 404 (cleaner behavior on
 * {@code /pointsPrPlayer/{token}}).
 *
 * <p>The legacy backend supported additional filter tokens on
 * {@code /pointsPrPlayer/} ({@code alltime-onlylunch},
 * {@code alltime-ratiofocus}, {@code alltime-elo}). These are dropped
 * per FRONTEND-USAGE.md — the React UI never requests them.
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
