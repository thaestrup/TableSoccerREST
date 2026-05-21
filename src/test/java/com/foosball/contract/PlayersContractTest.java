package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashSet;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the Players resource (legacy: {@code Players.groovy}).
 *
 * <p>Reference fixtures: {@code players.json} (collection),
 * {@code player-by-name.json} (single).
 *
 * <p>Documented quirks:
 * <ul>
 *   <li>{@code oprettet} is wire-formatted as {@code java.sql.Timestamp.toString()}
 *       ({@link ContractSuite#LEGACY_TIMESTAMP_REGEX}), NOT ISO-8601. Most
 *       likely Quarkus drift point — Jackson defaults to ISO.</li>
 *   <li>{@code POST /players} accepts a one-element JSON array, not a bare
 *       object. Response is {@code text/plain}, not JSON; we only assert
 *       status, not body shape.</li>
 *   <li>{@code POST /players} returns HTTP 200 even when the unique-name
 *       constraint rejects a duplicate (legacy swallows the SQL exception
 *       into the text body). The suite tolerates re-runs by ignoring the
 *       insert outcome and asserting via a follow-up GET.</li>
 *   <li>{@code PUT /players/{name}} body is a bare {@code Player} object
 *       (NO array wrapper) — different from POST, mirroring legacy.</li>
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlayersContractTest extends ContractSuite {

    private static final String TEST_PLAYER = "ContractTestPlayer";
    private static final String TEST_PLAYER_TIMESTAMP = "2026-05-10 21:00:00.0";

    @Test
    @Order(1)
    void getAllPlayers_returnsArrayWithExpectedShape() {
        JsonNode body = given()
                .when().get("/players/")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);

        assertTrue(body.isArray(), "/players/ must return a JSON array");
        assertTrue(body.size() > 0, "/players/ must return at least one player");

        Set<String> names = new HashSet<>();
        for (JsonNode player : body) {
            assertTrue(player.isObject(), "each entry must be an object");
            // Mandatory keys from frontend Zod schema (FoosballUnity src/features/players/usePlayers.ts).
            assertTrue(player.has("name"), "player missing 'name'");
            assertTrue(player.has("playerReady"), "player missing 'playerReady'");
            assertTrue(player.has("oprettet"), "player missing 'oprettet'");
            assertTrue(player.has("registeredRFIDTag"), "player missing 'registeredRFIDTag'");

            assertFalse(player.get("name").isNull(), "name must not be null");
            assertTrue(player.get("name").isTextual(), "name must be a string");
            // playerReady is a JSON boolean, NOT a string.
            assertTrue(player.get("playerReady").isBoolean(),
                    "playerReady must be a JSON boolean (got: " + player.get("playerReady").getNodeType() + ")");

            String name = player.get("name").asText();
            assertTrue(names.add(name), "duplicate name in /players: " + name);
        }
    }

    @Test
    @Order(2)
    void getPlayerByName_returnsSingleObjectWithLegacyTimestamp() {
        // Pick a name guaranteed by the captured fixture.
        JsonNode fixture = loadFixture("players");
        String existingName = fixture.get(0).get("name").asText();

        JsonNode body = given()
                .when().get("/players/" + existingName)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);

        assertTrue(body.isObject(), "/players/{name} must return a single object");
        assertEquals(existingName, body.get("name").asText());
        assertTrue(body.get("playerReady").isBoolean());
        assertTrue(body.has("registeredRFIDTag"));

        // The contract drift hot-spot: oprettet must match Timestamp.toString(),
        // not ISO-8601. Quarkus Jackson default would emit
        // "2016-10-09T19:46:36" — that breaks the React Zod schema.
        String oprettet = body.get("oprettet").asText();
        assertThat("oprettet must match legacy Timestamp.toString() format",
                oprettet, matchesPattern(LEGACY_TIMESTAMP_REGEX));
    }

    @Test
    @Order(3)
    void postPlayer_acceptsArrayWrappedSingleton() {
        // Body is an array around a single player (matches legacy wire and the
        // FoosballUnity AddPlayerForm payload). Re-runs are tolerated because
        // POST returns 200 even on a duplicate — we check the GET, not the
        // insert response body.
        String body = "[{\"name\":\"" + TEST_PLAYER
                + "\",\"playerReady\":true,\"oprettet\":\"" + TEST_PLAYER_TIMESTAMP
                + "\",\"registeredRFIDTag\":\"\"}]";

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .when().post("/players/")
                .then()
                .statusCode(200);

        // Now the player must be retrievable.
        JsonNode fetched = given()
                .when().get("/players/" + TEST_PLAYER)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);

        assertEquals(TEST_PLAYER, fetched.get("name").asText());
        assertNotNull(fetched.get("playerReady"));
    }

    @Test
    @Order(4)
    void putPlayerByName_flipsPlayerReadyAndRoundTrips() {
        // Read current state (test #3 must have run, or the player exists from a prior run).
        JsonNode current = given()
                .when().get("/players/" + TEST_PLAYER)
                .then()
                .statusCode(200)
                .extract().body().as(JsonNode.class);
        boolean originalReady = current.get("playerReady").asBoolean();
        String oprettet = current.get("oprettet").asText();
        String tag = current.get("registeredRFIDTag").asText();

        // PUT body is a bare object (no array wrapper) per legacy contract.
        boolean flipped = !originalReady;
        String body = "{\"name\":\"" + TEST_PLAYER
                + "\",\"playerReady\":" + flipped
                + ",\"oprettet\":\"" + oprettet
                + "\",\"registeredRFIDTag\":\"" + tag + "\"}";

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .when().put("/players/" + TEST_PLAYER)
                .then()
                .statusCode(200);

        // Round-trip GET: the flipped value must persist.
        given()
                .when().get("/players/" + TEST_PLAYER)
                .then()
                .statusCode(200)
                .body("playerReady", equalTo(flipped));
    }

    @Test
    @Order(5)
    void getAllPlayers_collectionLargerThanOne_afterPost() {
        // Sanity: by this point the collection must contain our test player AND others.
        JsonNode body = given()
                .when().get("/players/")
                .then()
                .statusCode(200)
                .extract().body().as(JsonNode.class);
        assertThat(body.size(), greaterThan(1));
    }
}
