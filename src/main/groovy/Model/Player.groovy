package Model

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.sql.GroovyRowResult

/**
 * Created by super on 20/11/2016.
 */
public class Player {
    private final String name;
    private final boolean playerReady;
    private final String oprettet;

    public Player(@JsonProperty("name") String name,
                  @JsonProperty("playerReady") String playerReady,
                  @JsonProperty("oprettet") String oprettet) {
        this.name = name;
        this.playerReady = Boolean.valueOf(playerReady);
        this.oprettet = oprettet;
    }

    public Player(GroovyRowResult row) {
        if (row.containsKey("name")) {
            this.name = row.getProperty("name");
        }
        this.playerReady = row.getProperty("playerReady");
        this.oprettet = row.getProperty("oprettet");
    }

    String getName() {
        return name
    }

    boolean getPlayerReady() {
        return playerReady
    }

    String getOprettet() {
        return oprettet
    }
}