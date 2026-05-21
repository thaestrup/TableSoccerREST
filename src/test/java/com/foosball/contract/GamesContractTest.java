package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the Games resource (legacy: {@code Games.groovy}).
 *
 * <p>Reference fixtures: {@code games-alltime.json}, {@code games-week.json},
 * {@code games-day.json}, {@code games-month.json}, {@code games-hour.json},
 * {@code games-by-player.json}.
 *
 * <p>Documented quirks:
 * <ul>
 *   <li>The wire JSON key is {@code points_at_stake}. The DB column carries
 *       the typo ({@code points_at_steake}) but the legacy serializer never
 *       leaked it. Quarkus port keeps the wire correct.</li>
 *   <li>Absent player slots emit the literal STRING {@code "null"} (not JSON
 *       null), originating from {@code String.valueOf(null)} on the legacy
 *       side. Frontend tolerates this, so we preserve it.</li>
 *   <li>{@code lastUpdated} is a {@code Timestamp.toString()}-formatted
 *       string ({@link ContractSuite#LEGACY_TIMESTAMP_REGEX}).</li>
 *   <li>{@code GET /games/{token}} is overloaded: token matches one of
 *       {@code hour|day|week|month|alltime}, otherwise falls through to a
 *       per-player lookup. Frontend exercises BOTH paths.</li>
 *   <li>{@code POST /games/} body is a one-element array, response shape is
 *       {@code {"newGameIDs":["<id>"]}} where the id is a numeric STRING.</li>
 *   <li>{@code DELETE /games/} is destructive (truncate-all). Disabled by
 *       default; opt in with {@code -Dcontract.destructive=true}.</li>
 * </ul>
 */
class GamesContractTest extends ContractSuite {

    private static final List<String> PERIODS = List.of("alltime", "month", "week", "day", "hour");
    private static final Set<String> ALLOWED_WINNERS = Set.of("red", "blue", "draw", "");

    @Test
    void getGamesByPeriod_eachPeriodReturns200WithExpectedShape() {
        for (String period : PERIODS) {
            JsonNode body = given()
                    .when().get("/games/" + period)
                    .then()
                    .statusCode(200)
                    .contentType("application/json")
                    .extract().body().as(JsonNode.class);

            assertTrue(body.isArray(), "/games/" + period + " must be a JSON array");
            for (JsonNode game : body) {
                assertGameShape(game, period);
            }
        }
    }

    @Test
    void getGamesByPlayerName_returnsArrayEvenWhenEmpty() {
        // Pick a player that is known to have games (Thomas appears in
        // games-alltime.json). Even if the player overload returns [] for
        // some names — we observed empty for "Carsten" — the response
        // shape and status must hold.
        for (String name : List.of("Thomas", "Joan", "Jens", "Carsten")) {
            JsonNode body = given()
                    .when().get("/games/" + name)
                    .then()
                    .statusCode(200)
                    .contentType("application/json")
                    .extract().body().as(JsonNode.class);

            assertTrue(body.isArray(), "/games/" + name + " must be a JSON array");
            for (JsonNode game : body) {
                assertGameShape(game, "by-player:" + name);
            }
        }
    }

    @Test
    void postGame_acceptsArrayWrappedSingletonAndReturnsNewGameIds() {
        // Body wraps a single game in an array, mirroring the FoosballUnity
        // ReportGameForm payload. Players exist in the captured fixture.
        String body = "[{\"player_red_1\":\"Thomas\",\"player_red_2\":\"Joan\","
                + "\"player_blue_1\":\"Jens\",\"player_blue_2\":\"Allan\","
                + "\"lastUpdated\":\"2026-05-10 21:00:00.0\","
                + "\"match_winner\":\"red\",\"points_at_stake\":25,\"winning_table\":1}]";

        JsonNode response = given()
                .header("Content-Type", "application/json")
                .body(body)
                .when().post("/games/")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);

        assertTrue(response.has("newGameIDs"), "response must contain newGameIDs key");
        JsonNode ids = response.get("newGameIDs");
        assertTrue(ids.isArray(), "newGameIDs must be an array");
        assertTrue(ids.size() >= 1, "newGameIDs must have at least one entry");
        // The id is serialized as a string (legacy behavior); the frontend
        // reads newGameIDs[0] as a string for the toast.
        JsonNode firstId = ids.get(0);
        assertTrue(firstId.isTextual(), "newGameIDs[0] must be a JSON string");
        assertThat("newGameIDs[0] must be numeric-string",
                firstId.asText(), matchesPattern("^\\d+$"));
    }

    @Test
    void postGame_oneVone_acceptsJsonNullBackSlotsAndStoresNullStringSentinels() {
        // The React ReportGameForm sends JSON null in the back-row slots when
        // reporting a 1v1 result (the `randomTournament` algorithm produces
        // 1v1 boards when the player pool isn't a multiple of 4). The legacy
        // Groovy backend accidentally stringified null → "null" through
        // GString concatenation; the Quarkus port preserves the quirk via
        // GameMapper.toEntity. Without this handling, the entity's @NotNull
        // validators bounce with a 500. Regression test for that crash.
        String body = "[{\"player_red_1\":\"Lars\",\"player_red_2\":null,"
                + "\"player_blue_1\":\"Joan\",\"player_blue_2\":null,"
                + "\"lastUpdated\":\"2026-05-14 17:00:00.0\","
                + "\"match_winner\":\"red\",\"points_at_stake\":25,\"winning_table\":2}]";

        JsonNode response = given()
                .header("Content-Type", "application/json")
                .body(body)
                .when().post("/games/")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);

        String newId = response.get("newGameIDs").get(0).asText();

        // Read it back and confirm the back slots carry the literal "null" string,
        // not JSON null. The frontend's GameSchema transforms "null" → JS null on
        // the way back into the UI.
        JsonNode roundTrip = given()
                .when().get("/games/alltime")
                .then().statusCode(200)
                .extract().body().as(JsonNode.class);

        JsonNode created = null;
        for (JsonNode g : roundTrip) {
            if (g.get("id").asText().equals(newId)) {
                created = g;
                break;
            }
        }
        assertNotNull(created, "newly-inserted 1v1 game (id=" + newId + ") not visible in /games/alltime");
        assertGameShape(created, "1v1 post round-trip");
        assertTrue(created.get("player_red_2").asText().equals("null"),
                "player_red_2 must be the literal string \"null\" for a 1v1 (got "
                        + created.get("player_red_2").asText() + ")");
        assertTrue(created.get("player_blue_2").asText().equals("null"),
                "player_blue_2 must be the literal string \"null\" for a 1v1 (got "
                        + created.get("player_blue_2").asText() + ")");
    }

    @Test
    @Disabled("Destructive: truncates all games. Opt in with -Dcontract.destructive=true and "
            + "re-enable manually. Skipped by default to keep the contract suite re-runnable "
            + "against the live legacy backend.")
    void deleteAllGames_returns200() {
        // When manually re-enabled the assertion is just that legacy returns 200.
        if (!destructiveEnabled()) {
            return;
        }
        given()
                .when().delete("/games/")
                .then()
                .statusCode(200);
    }

    private static void assertGameShape(JsonNode game, String context) {
        assertTrue(game.isObject(), "each game must be an object (" + context + ")");

        // Required keys exercised by the frontend Zod schema.
        for (String key : List.of("id", "player_red_1", "player_red_2",
                "player_blue_1", "player_blue_2", "lastUpdated",
                "match_winner", "points_at_stake", "winning_table")) {
            assertTrue(game.has(key),
                    "game in " + context + " missing key '" + key + "'");
        }
        // Critical: the wire key is points_at_stake (correct), NOT the DB typo.
        assertFalse(game.has("points_at_steake"),
                "game in " + context + " leaks the DB typo 'points_at_steake' on the wire");

        assertTrue(game.get("id").isInt(), "id must be int (" + context + ")");
        assertTrue(game.get("points_at_stake").isInt(),
                "points_at_stake must be int (" + context + ")");
        assertTrue(game.get("winning_table").isInt(),
                "winning_table must be int (" + context + ")");

        // Player slots are STRINGS (possibly the literal "null"); never JSON null.
        for (String slot : List.of("player_red_1", "player_red_2",
                "player_blue_1", "player_blue_2")) {
            JsonNode v = game.get(slot);
            assertNotNull(v, slot + " present (" + context + ")");
            assertTrue(v.isTextual(),
                    slot + " must be a string (got " + v.getNodeType() + ", " + context + ")");
        }

        String winner = game.get("match_winner").asText();
        assertTrue(ALLOWED_WINNERS.contains(winner),
                "match_winner '" + winner + "' not in " + ALLOWED_WINNERS + " (" + context + ")");

        String lastUpdated = game.get("lastUpdated").asText();
        assertThat("lastUpdated must match legacy Timestamp.toString() format (" + context + ")",
                lastUpdated, matchesPattern(LEGACY_TIMESTAMP_REGEX));

        assertThat("id must be >= 0 (" + context + ")",
                game.get("id").asInt(), greaterThanOrEqualTo(0));
    }
}
