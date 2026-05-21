package com.foosball.dto;

/**
 * Wire shape for the points-per-player request (legacy
 * {@code PointsPrPlayerRequest}): the point values to award for
 * a win, loss, and draw.
 */
public record PointsRequestDto(int winnerPoints, int loserPoints, int drawPoints) {
}
