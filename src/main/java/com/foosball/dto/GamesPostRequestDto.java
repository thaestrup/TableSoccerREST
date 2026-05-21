package com.foosball.dto;

import java.util.List;

/**
 * Wire shape for a POST /games request:
 * how many games to generate, and the player pool to draw from.
 */
public record GamesPostRequestDto(int numberOfGames, List<PlayerDto> players) {
}
