package com.foosball.dto;

import java.util.List;

/**
 * Wire shape returned from POST /games — a wrapper around the
 * list of newly-created game IDs.
 */
public record GamesPostResponseDto(List<String> newGameIDs) {
}
