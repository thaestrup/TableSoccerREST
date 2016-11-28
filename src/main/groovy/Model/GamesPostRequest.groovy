package Model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by super on 20/11/2016.
 */
public class GamesPostRequest {

    private final int numberOfGames;
    private final List<Player> players;

    GamesPostRequest(@JsonProperty("numberOfGames") int numberOfGames,
                     @JsonProperty("players") List<Player> players) {
        this.numberOfGames = numberOfGames
        this.players = players
    }

    List<Player> getPlayers() {
        return players
    }

    int getNumberOfGames() {
        return numberOfGames
    }
}