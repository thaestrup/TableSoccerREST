package com.foosball.dto;

/** Wire shape for points-per-player requests: point values awarded for win, loss, draw. */
public record PointsRequestDto(int winnerPoints, int loserPoints, int drawPoints) {
}
