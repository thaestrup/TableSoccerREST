package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
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
 * Contract tests for the Players resource.
 *
 * <ul>
 *   <li>{@code oprettet} is a {@code java.sql.Timestamp.toString()} string
 *       ({@link ContractSuite#WIRE_TIMESTAMP_REGEX}), NOT ISO-8601.</li>
 *   <li>{@code POST /players} accepts a one-element JSON array, not a bare
 *       object. Response is {@code text/plain}; tests only assert status.</li>
 *   <li>{@code POST /players} returns 200 even on a duplicate name (the
 *       row is silently upserted). The suite tolerates re-runs by checking
 *       a follow-up GET rather than the POST response body.</li>
 *   <li>{@code PUT /players/{name}} body is a bare object (no array wrapper).</li>
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
    void getPlayerByName_returnsSingleObjectWithTimestamp() {
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

        String oprettet = body.get("oprettet").asText();
        assertThat("oprettet must match Timestamp.toString() wire format",
                oprettet, matchesPattern(WIRE_TIMESTAMP_REGEX));
    }

    @Test
    @Order(3)
    void postPlayer_acceptsArrayWrappedSingleton() {
        // Body is an array around a single player. Re-runs are tolerated
        // because POST is an upsert — we check via a follow-up GET.
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

        // PUT body is a bare object (no array wrapper).
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

    // ----- Rename ----------------------------------------------------------

    private static final String RENAME_FROM = "RnAlpha";
    private static final String RENAME_TO = "RnBeta";

    @Test
    @Order(10)
    void renamePlayer_cascadesNameInGames() {
        // Best-effort cleanup from any prior run.
        deletePlayerIfPresent(RENAME_FROM);
        deletePlayerIfPresent(RENAME_TO);
        ensurePlayer(RENAME_FROM);
        ensurePlayer(TEST_PLAYER);

        String gameBody = "[{\"player_red_1\":\"" + RENAME_FROM
                + "\",\"player_red_2\":\"" + TEST_PLAYER + "\","
                + "\"player_blue_1\":\"" + TEST_PLAYER + "\","
                + "\"player_blue_2\":\"" + RENAME_FROM + "\","
                + "\"lastUpdated\":\"2026-05-25 12:00:00.0\","
                + "\"match_winner\":\"red\",\"points_at_stake\":1,\"winning_table\":1}]";
        given().header("Content-Type", "application/json").body(gameBody)
                .when().post("/games/")
                .then().statusCode(200);

        // Confirm RENAME_FROM has at least 1 game before rename.
        JsonNode beforeGames = given().when().get("/games/" + RENAME_FROM)
                .then().statusCode(200)
                .extract().body().as(JsonNode.class);
        assertTrue(beforeGames.size() > 0, "RENAME_FROM should have games before rename");

        // Rename.
        given().header("Content-Type", "application/json")
                .body("{\"newName\":\"" + RENAME_TO + "\"}")
                .when().put("/players/" + RENAME_FROM + "/rename")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(matchesPattern(
                        "^rename: " + RENAME_FROM + " -> " + RENAME_TO + ", games updated: \\d+\\s*$"));

        // RENAME_FROM is gone, RENAME_TO exists.
        given().when().get("/players/" + RENAME_FROM).then().statusCode(404);
        given().when().get("/players/" + RENAME_TO).then().statusCode(200);

        // Games cascaded: RENAME_FROM has zero games, RENAME_TO inherits them.
        JsonNode afterFrom = given().when().get("/games/" + RENAME_FROM)
                .then().statusCode(200).extract().body().as(JsonNode.class);
        JsonNode afterTo = given().when().get("/games/" + RENAME_TO)
                .then().statusCode(200).extract().body().as(JsonNode.class);
        assertEquals(0, afterFrom.size(), "no games should remain under the old name");
        assertTrue(afterTo.size() > 0, "games must follow the player to the new name");
    }

    @Test
    @Order(11)
    void renamePlayer_conflictReturns409() {
        ensurePlayer(TEST_PLAYER);
        String src = "RnConflict";
        deletePlayerIfPresent(src);
        ensurePlayer(src);

        given().header("Content-Type", "application/json")
                .body("{\"newName\":\"" + TEST_PLAYER + "\"}")
                .when().put("/players/" + src + "/rename")
                .then().statusCode(409);

        deletePlayerIfPresent(src);
    }

    @Test
    @Order(12)
    void renamePlayer_unknownReturns404() {
        given().header("Content-Type", "application/json")
                .body("{\"newName\":\"Whatever\"}")
                .when().put("/players/DefinitelyNotAPlayer_xyz/rename")
                .then().statusCode(404);
    }

    // ----- Photo -----------------------------------------------------------

    private static final String PHOTO_PLAYER = "PhotoTestP";

    @Test
    @Order(20)
    void photo_putGetDeleteRoundTrip() {
        ensurePlayer(PHOTO_PLAYER);
        // Start clean.
        given().when().delete("/players/" + PHOTO_PLAYER + "/photo");

        byte[] tinyPng = tinyPngBytes();

        // PUT
        given().header("Content-Type", "image/png").body(tinyPng)
                .when().put("/players/" + PHOTO_PLAYER + "/photo")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(matchesPattern("^uploadPhoto: " + PHOTO_PLAYER + ", bytes: \\d+\\s*$"));

        // GET returns identical bytes + correct content-type.
        byte[] got = given().when().get("/players/" + PHOTO_PLAYER + "/photo")
                .then()
                .statusCode(200)
                .contentType("image/png")
                .extract().body().asByteArray();
        assertEquals(tinyPng.length, got.length, "GET should return same byte length");
        for (int i = 0; i < tinyPng.length; i++) {
            assertEquals(tinyPng[i], got[i], "byte " + i + " differs");
        }

        // DELETE
        given().when().delete("/players/" + PHOTO_PLAYER + "/photo")
                .then().statusCode(200).contentType("text/plain");

        // Subsequent GET is 404.
        given().when().get("/players/" + PHOTO_PLAYER + "/photo")
                .then().statusCode(404);
    }

    @Test
    @Order(21)
    void photo_getMissingReturns404() {
        ensurePlayer(PHOTO_PLAYER);
        given().when().delete("/players/" + PHOTO_PLAYER + "/photo");
        given().when().get("/players/" + PHOTO_PLAYER + "/photo").then().statusCode(404);
    }

    @Test
    @Order(22)
    void photo_unsupportedContentTypeReturns415() {
        ensurePlayer(PHOTO_PLAYER);
        given().header("Content-Type", "text/plain").body("not an image")
                .when().put("/players/" + PHOTO_PLAYER + "/photo")
                .then().statusCode(415);
    }

    @Test
    @Order(23)
    void photo_overSizeReturns413() {
        ensurePlayer(PHOTO_PLAYER);
        byte[] big = new byte[2 * 1024 * 1024 + 100];
        given().header("Content-Type", "image/png").body(big)
                .when().put("/players/" + PHOTO_PLAYER + "/photo")
                .then().statusCode(413);
    }

    @Test
    @Order(24)
    void photo_deleteMissingReturns404() {
        ensurePlayer(PHOTO_PLAYER);
        given().when().delete("/players/" + PHOTO_PLAYER + "/photo");
        given().when().delete("/players/" + PHOTO_PLAYER + "/photo").then().statusCode(404);
    }

    // ----- Helpers ---------------------------------------------------------

    private static void ensurePlayer(String name) {
        String body = "[{\"name\":\"" + name
                + "\",\"playerReady\":true,\"oprettet\":\"2026-05-25 21:00:00.0\","
                + "\"registeredRFIDTag\":\"\"}]";
        given().header("Content-Type", "application/json").body(body)
                .when().post("/players/").then().statusCode(200);
    }

    private static void deletePlayerIfPresent(String name) {
        // Tolerant cleanup helper — silently ignores 404.
        given().when().delete("/players/" + name);
    }

    @AfterAll
    static void cleanupTestPlayers() {
        for (String n : new String[] {
                TEST_PLAYER, RENAME_FROM, RENAME_TO, "RnConflict", PHOTO_PLAYER}) {
            deletePlayerIfPresent(n);
        }
    }

    // ----- Delete ----------------------------------------------------------

    @Test
    @Order(30)
    void deletePlayer_returns200ThenGetIs404() {
        String victim = "DelTestPlayer";
        ensurePlayer(victim);
        given().when().delete("/players/" + victim)
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(matchesPattern("^deletePlayer: " + victim + "\\s*$"));
        given().when().get("/players/" + victim).then().statusCode(404);
    }

    @Test
    @Order(31)
    void deletePlayer_unknownReturns404() {
        given().when().delete("/players/DefinitelyNotAPlayer_xyz").then().statusCode(404);
    }

    @Test
    @Order(32)
    void deletePlayer_cascadesPhotoButLeavesGames() {
        String victim = "DelTestWithBoth";
        ensurePlayer(victim);

        // Attach a photo. Cascade FK should drop it on player delete.
        given().header("Content-Type", "image/png").body(tinyPngBytes())
                .when().put("/players/" + victim + "/photo")
                .then().statusCode(200);

        // Insert a game referencing the player by name.
        String gameBody = "[{\"player_red_1\":\"" + victim
                + "\",\"player_red_2\":\"" + TEST_PLAYER + "\","
                + "\"player_blue_1\":\"" + TEST_PLAYER + "\","
                + "\"player_blue_2\":\"" + victim + "\","
                + "\"lastUpdated\":\"2026-05-25 12:00:00.0\","
                + "\"match_winner\":\"red\",\"points_at_stake\":1,\"winning_table\":1}]";
        ensurePlayer(TEST_PLAYER);
        given().header("Content-Type", "application/json").body(gameBody)
                .when().post("/games/").then().statusCode(200);

        // Delete the player.
        given().when().delete("/players/" + victim).then().statusCode(200);

        // Player row gone, photo gone, but the game still lists `victim` by name.
        given().when().get("/players/" + victim).then().statusCode(404);
        given().when().get("/players/" + victim + "/photo").then().statusCode(404);
        // GET /games/{name} still returns historical games (name lookup, not FK).
        JsonNode hist = given().when().get("/games/" + victim)
                .then().statusCode(200)
                .extract().body().as(JsonNode.class);
        assertTrue(hist.size() > 0, "historical games must survive a player delete");
    }

    /**
     * Minimal valid PNG (1×1 white pixel, ~67 bytes). Constructed inline so
     * the test doesn't depend on a binary fixture file.
     */
    private static byte[] tinyPngBytes() {
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            // Signature
            out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
            // IHDR
            byte[] ihdrData = new byte[] {
                    0, 0, 0, 1, // width = 1
                    0, 0, 0, 1, // height = 1
                    8, 2, 0, 0, 0, // bit depth, color type RGB, compression, filter, interlace
            };
            writeChunk(out, "IHDR", ihdrData);
            // IDAT: zlib-compressed scanline of one white pixel + filter byte
            java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
            raw.write(0); // filter
            raw.write(new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff}); // RGB
            java.io.ByteArrayOutputStream compressed = new java.io.ByteArrayOutputStream();
            java.util.zip.Deflater def = new java.util.zip.Deflater();
            def.setInput(raw.toByteArray());
            def.finish();
            byte[] buf = new byte[64];
            while (!def.finished()) {
                int n = def.deflate(buf);
                compressed.write(buf, 0, n);
            }
            writeChunk(out, "IDAT", compressed.toByteArray());
            // IEND
            writeChunk(out, "IEND", new byte[0]);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeChunk(java.io.ByteArrayOutputStream out, String type, byte[] data)
            throws java.io.IOException {
        out.write((data.length >>> 24) & 0xff);
        out.write((data.length >>> 16) & 0xff);
        out.write((data.length >>> 8) & 0xff);
        out.write(data.length & 0xff);
        byte[] typeBytes = type.getBytes();
        out.write(typeBytes);
        out.write(data);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(typeBytes);
        crc.update(data);
        long v = crc.getValue();
        out.write((int) ((v >>> 24) & 0xff));
        out.write((int) ((v >>> 16) & 0xff));
        out.write((int) ((v >>> 8) & 0xff));
        out.write((int) (v & 0xff));
    }
}
