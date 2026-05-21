package com.foosball.dto;

/**
 * Wire shape for a single player in the points-per-player ranking
 * (legacy {@code PointsPrPlayerPlayer}).
 */
public record PointsPlayerDto(int position, int points, int numberOfGames, String name) {
}
