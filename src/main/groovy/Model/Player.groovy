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
    private final String registeredRFIDTag;

    public Player(@JsonProperty("name") String name,
                  @JsonProperty("playerReady") String playerReady,
                  @JsonProperty("oprettet") String oprettet,
                  @JsonProperty("registeredRFIDTag") String registeredRFIDTag) {
        this.name = name;
        this.playerReady = Boolean.valueOf(playerReady);
        this.oprettet = oprettet;
        this.registeredRFIDTag = registeredRFIDTag;
    }

/*
    public Player(String name,
                  String playerReady,
                  String oprettet,
                  String registeredRFIDTag) {
        this.name = name;
        this.playerReady = Boolean.valueOf(playerReady);
        this.oprettet = oprettet;
        this.registeredRFIDTag = registeredRFIDTag;
    }
    */

    public Player(GroovyRowResult row) {
        if (row.containsKey("name")) {
            this.name = row.getProperty("name");
        }
        this.playerReady = row.getProperty("playerReady");
        this.oprettet = row.getProperty("oprettet");
        this.registeredRFIDTag = row.getProperty("registeredRFIDTag");
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

    String getRegisteredRFIDTag() {
        return registeredRFIDTag
    }
}
