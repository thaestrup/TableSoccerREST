package com.foosball.dto;

import java.util.List;

/**
 * Wire shape for a tournament round — a list of games to be played
 * in this round. The legacy JSON key is {@code tournamentGames}.
 */
public record TournamentGameRoundDto(List<GameDto> tournamentGames) {
}
