package com.foosball.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire shape for a game. JSON keys are snake_case.
 *
 * <ul>
 *   <li>{@code lastUpdated} is a {@link java.sql.Timestamp#toString()}
 *       string ({@code yyyy-MM-dd HH:mm:ss.S}, variable-length nanos),
 *       not ISO-8601.</li>
 *   <li>Absent player slots are emitted as the literal string
 *       {@code "null"} (not JSON null).</li>
 * </ul>
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
