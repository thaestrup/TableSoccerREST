package Model

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.sql.GroovyRowResult

/**
 * Created by super on 20/11/2016.
 */
public class PostGameRequest {

    public enum GenerationMethod {
        RANDOM, LASTFIRST, GIVEN
    }

    private final GenerationMethod generationMethod;
    private final int numberOfGames;
    private final List<Game> games;
    private final List<Player> players;

    PostGameRequest(@JsonProperty("generationMethod") GenerationMethod generationMethod,
                    @JsonProperty("numberOfGames") int numberOfGames,
                    @JsonProperty("games") List<Game> games,
                    @JsonProperty("players") List<Player> players) {
        this.generationMethod = generationMethod
        this.numberOfGames = numberOfGames
        this.games = games
        this.players = players
    }

    List<Player> getPlayers() {
        return players
    }

    GenerationMethod getGenerationMethod() {
        return generationMethod
    }

    int getNumberOfGames() {
        return numberOfGames
    }

    List<Game> getGames() {
        return games
    }
}