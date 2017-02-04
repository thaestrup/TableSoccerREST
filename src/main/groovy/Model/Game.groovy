package Model

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.sql.GroovyRowResult

/**
 * Created by super on 20/11/2016.
 */
public class Game {
    private final int id;
    private final String player_red_1;
    private final String player_red_2;
    private final String player_blue_1;
    private final String player_blue_2;
    private String lastUpdated;
    private final String match_winner;
    private final int winning_table;
    private int points_at_stake;

    Game(@JsonProperty("id") String id,
         @JsonProperty("player_red_1") String player_red_1,
         @JsonProperty("player_red_2") String player_red_2,
         @JsonProperty("player_blue_1") String player_blue_1,
         @JsonProperty("player_blue_2") String player_blue_2,
         @JsonProperty("lastUpdated") String lastUpdated,
         @JsonProperty("match_winner") String match_winner,
         @JsonProperty("points_at_stake") String points_at_stake,
         @JsonProperty("winning_table") String winning_table) {
        this.id = id != null ? Integer.valueOf(id) : -1
        this.player_red_1 = player_red_1
        this.player_red_2 = player_red_2
        this.player_blue_1 = player_blue_1
        this.player_blue_2 = player_blue_2
        this.lastUpdated = lastUpdated
        this.match_winner = match_winner
        this.winning_table = Integer.valueOf(winning_table)
        this.points_at_stake = Integer.valueOf(points_at_stake)
    }

    public Game(GroovyRowResult row) {
        this.id = Integer.valueOf(row.getProperty("id"))
        this.player_red_1 = row.getProperty("player_red_1")
        this.player_red_2 = row.getProperty("player_red_2")
        this.player_blue_1 = row.getProperty("player_blue_1")
        this.player_blue_2 = row.getProperty("player_blue_2")
        this.lastUpdated = row.getProperty("timestamp")
        this.match_winner = row.getProperty("match_winner")
        this.winning_table = Integer.valueOf(row.getProperty("winning_table"))
        this.points_at_stake = Integer.valueOf(row.getProperty("points_at_steake"))
    }

    int getPoints_at_stake() {
        return points_at_stake
    }

    void setPoints_at_stake(int tempPoints_at_stake) {
        points_at_stake = tempPoints_at_stake
    }

    int getId() {
        return id
    }

    String getPlayer_red_1() {
        return player_red_1
    }

    String getPlayer_red_2() {
        return player_red_2
    }

    String getPlayer_blue_1() {
        return player_blue_1
    }

    String getPlayer_blue_2() {
        return player_blue_2
    }

    String getLastUpdated() {
        return lastUpdated
    }

    void setLastUpdated(String tempLastUpdated) {
        lastUpdated = tempLastUpdated
    }

    String getMatch_winner() {
        return match_winner
    }

    int getWinning_table() {
        return winning_table
    }
}
