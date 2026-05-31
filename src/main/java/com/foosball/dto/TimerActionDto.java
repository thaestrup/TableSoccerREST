package com.foosball.dto;

/**
 * Wire shape for the timer state. {@code lastRequestedTimerStart} is a
 * {@link java.sql.Timestamp#toString()}-formatted string
 * ({@code yyyy-MM-dd HH:mm:ss.S}, variable-length nanos), not ISO-8601.
 */
public record TimerActionDto(int id, String lastRequestedTimerStart) {
}
