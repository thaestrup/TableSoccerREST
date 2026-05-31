package com.foosball.dto;

import java.util.List;

/** Wire shape for a tournament round — the games to be played in this round. */
public record TournamentGameRoundDto(List<GameDto> tournamentGames) {
}
