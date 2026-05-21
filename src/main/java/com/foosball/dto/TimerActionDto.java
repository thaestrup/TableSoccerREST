package com.foosball.dto;

/**
 * Wire shape for the timer state. Returned wrapped in a single-element
 * array (legacy contract) — see TimerResource.
 *
 * <p>{@code lastRequestedTimerStart} is a legacy
 * {@link java.sql.Timestamp#toString()} formatted string
 * ({@code yyyy-MM-dd HH:mm:ss.S}, variable-length nanos), not ISO-8601.
 * Kept as {@code String} for exact wire parity with the legacy backend.
 */
public record TimerActionDto(int id, String lastRequestedTimerStart) {
}
