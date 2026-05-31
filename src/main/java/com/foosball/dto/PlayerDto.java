package com.foosball.dto;

/**
 * Wire shape for a player. {@code oprettet} is a
 * {@link java.sql.Timestamp#toString()}-formatted string
 * ({@code yyyy-MM-dd HH:mm:ss.S}, variable-length nanos), not ISO-8601.
 * {@code playerReady} is a strict JSON boolean.
 */
public record PlayerDto(
        String name,
        boolean playerReady,
        String oprettet,
        String registeredRFIDTag) {
}
