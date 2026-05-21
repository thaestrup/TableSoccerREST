package com.foosball.dto;

/**
 * Wire shape for a player.
 *
 * <p>{@code oprettet} is a legacy {@link java.sql.Timestamp#toString()}
 * formatted string ({@code yyyy-MM-dd HH:mm:ss.S}, variable-length nanos),
 * not ISO-8601. Kept as {@code String} for exact wire parity with the
 * legacy backend.
 *
 * <p><strong>Breaking change vs. legacy:</strong> the legacy {@code Player}
 * accepted {@code playerReady} as either a JSON boolean or a JSON string
 * (e.g. {@code "true"}/{@code "false"}) and ran {@code Boolean.valueOf}
 * on it. The Quarkus port accepts only a JSON boolean. The frontend
 * already sends booleans, so this is a no-op for the React client.
 */
public record PlayerDto(
        String name,
        boolean playerReady,
        String oprettet,
        String registeredRFIDTag) {
}
