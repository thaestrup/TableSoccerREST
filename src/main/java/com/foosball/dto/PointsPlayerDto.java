package com.foosball.dto;

/** Wire shape for one row in the points-per-player ranking. */
public record PointsPlayerDto(int position, int points, int numberOfGames, String name) {
}
