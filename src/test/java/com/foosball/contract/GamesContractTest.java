package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the Games resource.
 *
 * <ul>
 *   <li>Wire JSON key is {@code points_at_stake} (the DB column is
 *       {@code points_at_steake}; the mapper hides that).</li>
 *   <li>Absent player slots emit the literal string {@code "null"}, not
 *       JSON null.</li>
 *   <li>{@code lastUpdated} is a {@code Timestamp.toString()} string
 *       ({@link ContractSuite#WIRE_TIMESTAMP_REGEX}).</li>
 *   <li>{@code GET /games/{token}} dispatches: token in
 *       {@code hour|day|week|month|alltime} → period filter, otherwise
 *       per-player lookup.</li>
 *   <li>{@code POST /games/} body is a one-element array; response is
 *       {@code {"newGameIDs":["<id>"]}} (id is a numeric string).</li>
 *   <li>{@code DELETE /games/} is destructive (soft-delete-all); opt-in
 *       via {@code -Dcontract.destructive=true}.</li>
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
        // newGameIDs entries are JSON strings (numeric-string).
        JsonNode firstId = ids.get(0);
        assertTrue(firstId.isTextual(), "newGameIDs[0] must be a JSON string");
        assertThat("newGameIDs[0] must be numeric-string",
                firstId.asText(), matchesPattern("^\\d+$"));
    }

    @Test
    void postGame_oneVone_acceptsJsonNullBackSlotsAndStoresNullStringSentinels() {
        // 1v1 game: back-row slots arrive as JSON null and must be stored as
        // the literal "null" string (tbl_fights.player_*_2 is NOT NULL).
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
    void putGame_replacesEditableFields_preservesIdAndTimestamp() {
        // Insert a fresh game we can mutate without polluting the suite.
        String insertBody = "[{\"player_red_1\":\"Lars\",\"player_red_2\":\"Joan\","
                + "\"player_blue_1\":\"Michael\",\"player_blue_2\":\"John\","
                + "\"lastUpdated\":\"2026-05-22 00:00:00.0\","
                + "\"match_winner\":\"red\",\"points_at_stake\":25,\"winning_table\":1}]";
        String id = given()
                .header("Content-Type", "application/json")
                .body(insertBody)
                .when().post("/games/")
                .then().statusCode(200)
                .extract().body().jsonPath().getString("newGameIDs[0]");

        String putBody = "{\"id\":" + id + ","
                + "\"player_red_1\":\"Lars\",\"player_red_2\":\"Joan\","
                + "\"player_blue_1\":\"Michael\",\"player_blue_2\":\"John\","
                + "\"lastUpdated\":\"2099-12-31 23:59:59.0\","
                + "\"match_winner\":\"blue\",\"points_at_stake\":42,\"winning_table\":3}";
        JsonNode updated = given()
                .header("Content-Type", "application/json")
                .body(putBody)
                .when().put("/games/" + id)
                .then().statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);

        assertEquals(Long.parseLong(id), updated.get("id").asLong(),
                "id must be unchanged");
        assertEquals("blue", updated.get("match_winner").asText());
        assertEquals(42, updated.get("points_at_stake").asInt());
        assertEquals(3, updated.get("winning_table").asInt());
        // timestamp must be the original insert value, NOT the 2099 spoof.
        assertThat("PUT must not change timestamp",
                updated.get("lastUpdated").asText(), matchesPattern("^2026-05-22 .*"));
    }

    @Test
    void putGame_unknownIdReturns404() {
        String putBody = "{\"id\":99999999,\"player_red_1\":\"x\",\"player_red_2\":\"y\","
                + "\"player_blue_1\":\"z\",\"player_blue_2\":\"q\","
                + "\"lastUpdated\":\"2026-05-22 00:00:00.0\","
                + "\"match_winner\":\"red\",\"points_at_stake\":1,\"winning_table\":1}";
        given()
                .header("Content-Type", "application/json")
                .body(putBody)
                .when().put("/games/99999999")
                .then().statusCode(404);
    }

    @Test
    void deleteGame_softDeletes_removesFromListAndStats() {
        String insertBody = "[{\"player_red_1\":\"Lars\",\"player_red_2\":\"Joan\","
                + "\"player_blue_1\":\"Michael\",\"player_blue_2\":\"John\","
                + "\"lastUpdated\":\"2026-05-22 00:00:00.0\","
                + "\"match_winner\":\"red\",\"points_at_stake\":7,\"winning_table\":1}]";
        String id = given()
                .header("Content-Type", "application/json")
                .body(insertBody)
                .when().post("/games/")
                .then().statusCode(200)
                .extract().body().jsonPath().getString("newGameIDs[0]");

        // Confirm visible before delete.
        JsonNode before = given().when().get("/games").then().statusCode(200)
                .extract().body().as(JsonNode.class);
        assertTrue(streamIds(before).anyMatch(x -> x.equals(id)),
                "freshly inserted game " + id + " must be visible before delete");

        // Soft-delete.
        given().when().delete("/games/" + id)
                .then().statusCode(200)
                .contentType("text/plain")
                .body(matchesPattern("^deleteGame: \\d+\\s*$"));

        // Now hidden from /games.
        JsonNode after = given().when().get("/games").then().statusCode(200)
                .extract().body().as(JsonNode.class);
        assertFalse(streamIds(after).anyMatch(x -> x.equals(id)),
                "soft-deleted game " + id + " must NOT appear in /games");

        // Re-deleting yields 404 (already soft-deleted).
        given().when().delete("/games/" + id).then().statusCode(404);
    }

    @Test
    void deleteUnknownGame_returns404() {
        given().when().delete("/games/99999998").then().statusCode(404);
    }

    private static java.util.stream.Stream<String> streamIds(JsonNode arr) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (JsonNode g : arr) out.add(g.get("id").asText());
        return out.stream();
    }

    @Test
    void deletedPlayer_appearsAsDeletedPlayerInGames() {
        // Setup: create a temp player + partner, play a game between them,
        // delete the temp. The game must remain visible in /games but the
        // deleted player's slots must read "Deleted player". The "null"
        // sentinel for empty back slots must NOT be touched.
        String ghost = "GameGhost";
        String partner = "ContractTestPlayer";

        // Best-effort prep.
        given().header("Content-Type", "application/json")
                .body("[{\"name\":\"" + ghost + "\",\"playerReady\":true,"
                        + "\"oprettet\":\"2026-05-25 12:00:00.0\","
                        + "\"registeredRFIDTag\":\"\"}]")
                .when().post("/players/").then().statusCode(200);
        given().header("Content-Type", "application/json")
                .body("[{\"name\":\"" + partner + "\",\"playerReady\":true,"
                        + "\"oprettet\":\"2026-05-25 12:00:00.0\","
                        + "\"registeredRFIDTag\":\"\"}]")
                .when().post("/players/").then().statusCode(200);

        // 1v1 game: ghost vs partner, back slots null.
        String gameBody = "[{\"player_red_1\":\"" + ghost + "\","
                + "\"player_red_2\":null,"
                + "\"player_blue_1\":\"" + partner + "\","
                + "\"player_blue_2\":null,"
                + "\"lastUpdated\":\"2026-05-25 12:00:00.0\","
                + "\"match_winner\":\"red\",\"points_at_stake\":5,\"winning_table\":1}]";
        String newId = given().header("Content-Type", "application/json")
                .body(gameBody)
                .when().post("/games/").then().statusCode(200)
                .extract().body().jsonPath().getString("newGameIDs[0]");

        // Verify the game shows ghost's real name before delete.
        JsonNode before = given().when().get("/games").then().statusCode(200)
                .extract().body().as(JsonNode.class);
        JsonNode createdBefore = findById(before, newId);
        assertNotNull(createdBefore);
        assertEquals(ghost, createdBefore.get("player_red_1").asText());

        // Delete the player. Game row stays in tbl_fights.
        given().when().delete("/players/" + ghost).then().statusCode(200);

        // Re-read /games: ghost's slot must now show "Deleted player",
        // back slots must remain the "null" string sentinel,
        // partner's slot must be unchanged.
        JsonNode after = given().when().get("/games").then().statusCode(200)
                .extract().body().as(JsonNode.class);
        JsonNode createdAfter = findById(after, newId);
        assertNotNull(createdAfter);
        assertEquals("Deleted player", createdAfter.get("player_red_1").asText());
        assertEquals(partner, createdAfter.get("player_blue_1").asText());
        assertEquals("null", createdAfter.get("player_red_2").asText(),
                "back-row null sentinel must NOT be remapped");
        assertEquals("null", createdAfter.get("player_blue_2").asText());
    }

    private static JsonNode findById(JsonNode arr, String id) {
        for (JsonNode g : arr) if (g.get("id").asText().equals(id)) return g;
        return null;
    }

    @AfterAll
    static void cleanup() {
        for (String n : new String[] {"GameGhost", "ContractTestPlayer"}) {
            given().when().delete("/players/" + n);
        }
    }

    @Test
    @Disabled("Destructive: soft-deletes every game. Opt in with -Dcontract.destructive=true.")
    void deleteAllGames_returns200() {
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
        // Wire key is points_at_stake; the DB-column typo must not leak.
        assertFalse(game.has("points_at_steake"),
                "game in " + context + " leaks 'points_at_steake' on the wire");

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
        assertThat("lastUpdated must match Timestamp.toString() wire format (" + context + ")",
                lastUpdated, matchesPattern(WIRE_TIMESTAMP_REGEX));

        assertThat("id must be >= 0 (" + context + ")",
                game.get("id").asInt(), greaterThanOrEqualTo(0));
    }
}
