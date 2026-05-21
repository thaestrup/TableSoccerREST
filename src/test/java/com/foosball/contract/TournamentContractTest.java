package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the Tournament resources (legacy:
 * {@code AwesomeAlgorithmTournament.groovy}, {@code LastFirstTournament.groovy},
 * {@code RandomTournament.groovy}).
 *
 * <p>Reference fixtures: {@code tournament-awesome.json},
 * {@code tournament-lastfirst.json}, {@code tournament-random.json}.
 *
 * <p>Documented quirks:
 * <ul>
 *   <li>Awesome and Last-first return a "rounds wrapper":
 *       {@code [{tournamentGames: [Game, ...]}, ...]}.</li>
 *   <li>Random returns a flat {@code [Game, ...]} array (NO rounds
 *       wrapper). The shape divergence is intentional and the frontend
 *       branches on it. Preserve.</li>
 *   <li>Synthesized tournament games use sentinel values:
 *       {@code id=-1}, {@code points_at_stake=-1}, {@code winning_table=-1},
 *       {@code match_winner=""} (empty string, NOT null).</li>
 *   <li>{@code lastUpdated} on tournament games carries up to nanosecond
 *       precision (e.g. {@code "...:57.894"}); the
 *       {@link ContractSuite#LEGACY_TIMESTAMP_REGEX} regex tolerates 1-9
 *       fractional digits.</li>
 * </ul>
 */
class TournamentContractTest extends ContractSuite {

    /**
     * Eight players, all confirmed present in {@code players.json}. The legacy
     * algorithm validates names against the DB before generating pairings.
     */
    private static final String REQUEST_BODY = "{"
            + "\"numberOfGames\":2,"
            + "\"players\":["
            + player("Joan", true, "2016-10-09 17:46:36.0", "")
            + "," + player("Jens", true, "2016-10-09 07:46:36.0", "")
            + "," + player("John", true, "2016-10-09 03:46:36.0", "")
            + "," + player("Michael", true, "2016-10-09 17:46:36.0", "")
            + "," + player("Daniel", true, "2016-10-09 03:46:36.0", "")
            + "," + player("Nikolaj", true, "2016-10-09 17:46:36.0", "")
            + "," + player("Lars", true, "2016-10-10 15:46:36.0", "")
            + "," + player("Carsten", true, "2016-10-09 19:46:36.0", "")
            + "]}";

    private static String player(String name, boolean ready, String oprettet, String tag) {
        return "{\"name\":\"" + name
                + "\",\"playerReady\":" + ready
                + ",\"oprettet\":\"" + oprettet
                + "\",\"registeredRFIDTag\":\"" + tag + "\"}";
    }

    @Test
    void awesomeAlgorithmTournament_returnsRoundsWrapper() {
        JsonNode body = postTournament("awesomeAlgorithmTournament");
        assertRoundsWrapperShape(body, "awesomeAlgorithmTournament");
    }

    @Test
    void lastFirstTournament_returnsRoundsWrapper() {
        JsonNode body = postTournament("lastFirstTournament");
        assertRoundsWrapperShape(body, "lastFirstTournament");
    }

    @Test
    void randomTournament_returnsFlatGameArray() {
        // Different shape — flat Game[]. NO tournamentGames wrapper.
        // Document this divergence: every other tournament endpoint nests
        // games inside per-round objects; randomTournament does not.
        JsonNode body = postTournament("randomTournament");
        assertTrue(body.isArray(), "randomTournament must return a flat array");
        assertTrue(body.size() > 0, "randomTournament must produce at least one game");
        for (JsonNode game : body) {
            assertSyntheticTournamentGame(game, "randomTournament");
        }
        // Confirm the rounds wrapper is NOT present.
        assertTrue(body.get(0).has("player_red_1"),
                "randomTournament's first element must be a Game (flat shape), not a rounds wrapper");
        assertTrue(!body.get(0).has("tournamentGames"),
                "randomTournament must NOT carry a tournamentGames key");
    }

    private static JsonNode postTournament(String algorithm) {
        return given()
                .header("Content-Type", "application/json")
                .body(REQUEST_BODY)
                .when().post("/tournament/" + algorithm)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);
    }

    private static void assertRoundsWrapperShape(JsonNode body, String algo) {
        assertTrue(body.isArray(), algo + ": top-level must be a JSON array");
        assertTrue(body.size() > 0, algo + ": must produce at least one round");
        for (JsonNode round : body) {
            assertTrue(round.isObject(), algo + ": each round must be an object");
            assertTrue(round.has("tournamentGames"),
                    algo + ": each round must carry 'tournamentGames'");
            JsonNode games = round.get("tournamentGames");
            assertTrue(games.isArray(), algo + ": tournamentGames must be a JSON array");
            assertTrue(games.size() > 0,
                    algo + ": tournamentGames must contain at least one synthesized game");
            for (JsonNode game : games) {
                assertSyntheticTournamentGame(game, algo);
            }
        }
    }

    private static final Set<String> EXPECTED_KEYS = Set.of(
            "id", "player_red_1", "player_red_2", "player_blue_1", "player_blue_2",
            "lastUpdated", "match_winner", "points_at_stake", "winning_table");

    private static void assertSyntheticTournamentGame(JsonNode game, String algo) {
        assertTrue(game.isObject(), algo + ": each game must be an object");
        for (String k : EXPECTED_KEYS) {
            assertTrue(game.has(k), algo + ": tournament game missing key '" + k + "'");
        }
        // Sentinel values per legacy AdditionalUtil.
        assertEquals(-1, game.get("id").asInt(), algo + ": tournament games use id=-1");
        assertEquals(-1, game.get("points_at_stake").asInt(),
                algo + ": tournament games use points_at_stake=-1");
        assertEquals(-1, game.get("winning_table").asInt(),
                algo + ": tournament games use winning_table=-1");
        assertEquals("", game.get("match_winner").asText(),
                algo + ": tournament games use match_winner=\"\" (empty string, not null)");

        // Player slots are strings.
        for (String slot : List.of("player_red_1", "player_red_2",
                "player_blue_1", "player_blue_2")) {
            JsonNode v = game.get(slot);
            assertNotNull(v, algo + ": " + slot + " present");
            assertTrue(v.isTextual(),
                    algo + ": " + slot + " must be a string (got " + v.getNodeType() + ")");
        }

        String ts = game.get("lastUpdated").asText();
        assertThat(algo + ": lastUpdated must be in legacy Timestamp format",
                ts, matchesPattern(LEGACY_TIMESTAMP_REGEX));
    }
}
