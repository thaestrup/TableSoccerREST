package Model

import com.fasterxml.jackson.annotation.JsonProperty
import Model.Game

/**
 * Created by NIKL.
 */
public class TournamentGameRound {
    private final List<Game> tournamentGames;

    TournamentGameRound(@JsonProperty("tournamentGames") List<Game> tournamentGames) {
      this.tournamentGames = tournamentGames
    }

    List<Game> getTournamentGames() {
        return tournamentGames
    }

}
