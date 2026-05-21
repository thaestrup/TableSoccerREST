package com.foosball.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire shape for a game. Preserves the legacy snake_case JSON keys
 * ({@code player_red_1}, {@code match_winner}, etc.).
 *
 * <p>The legacy wire already exposed {@code points_at_stake} (correctly
 * spelled) even though the underlying DB column is {@code points_at_steake}.
 * The Quarkus port carries that same wire name forward; the DB column is
 * cleaned up to match in V1.
 *
 * <p>{@code lastUpdated} is the legacy wire name for the underlying DB
 * {@code timestamp} column.
 *
 * <p>Date strings use the legacy {@link java.sql.Timestamp#toString()}
 * format ({@code yyyy-MM-dd HH:mm:ss.S}, variable-length nanos), not
 * ISO-8601. Kept as {@code String} so we don't have to fight Jackson over
 * the variable-precision fractional seconds.
 *
 * <p>Absent player slots are emitted as the literal string {@code "null"}
 * (not JSON null) to preserve the legacy quirk relied on by the frontend.
 */
public record GameDto(
        Long id,
        @JsonProperty("player_red_1") String playerRed1,
        @JsonProperty("player_red_2") String playerRed2,
        @JsonProperty("player_blue_1") String playerBlue1,
        @JsonProperty("player_blue_2") String playerBlue2,
        String lastUpdated,
        @JsonProperty("match_winner") String matchWinner,
        @JsonProperty("points_at_stake") int pointsAtStake,
        @JsonProperty("winning_table") int winningTable) {
}
