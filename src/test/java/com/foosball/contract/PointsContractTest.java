package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the PointsPrPlayer resource. Returns a JSON array of
 * {@code {position, points, numberOfGames, name}} sorted by points
 * descending; tied scores produce repeated {@code position} values
 * (e.g. {@code 1, 1, 3, 3}). Period tokens: {@code hour|day|week|month|alltime}.
 */
class PointsContractTest extends ContractSuite {

    private static final List<String> PERIODS = List.of("alltime", "month", "week", "day", "hour");

    @Test
    void getPointsPrPlayer_eachPeriodReturns200WithExpectedShape() {
        for (String period : PERIODS) {
            JsonNode body = given()
                    .when().get("/pointsPrPlayer/" + period)
                    .then()
                    .statusCode(200)
                    .contentType("application/json")
                    .extract().body().as(JsonNode.class);

            assertTrue(body.isArray(), "/pointsPrPlayer/" + period + " must be a JSON array");

            int previousPosition = 0;
            for (int i = 0; i < body.size(); i++) {
                JsonNode entry = body.get(i);
                assertTrue(entry.isObject(),
                        period + "[" + i + "] must be an object");

                for (String k : List.of("position", "points", "numberOfGames", "name")) {
                    assertTrue(entry.has(k),
                            period + "[" + i + "] missing key '" + k + "'");
                    assertFalse(entry.get(k).isNull(),
                            period + "[" + i + "]." + k + " must not be null");
                }

                assertTrue(entry.get("position").isInt(),
                        period + "[" + i + "].position must be an int");
                assertTrue(entry.get("points").isInt(),
                        period + "[" + i + "].points must be an int");
                assertTrue(entry.get("numberOfGames").isInt(),
                        period + "[" + i + "].numberOfGames must be an int");
                assertTrue(entry.get("name").isTextual(),
                        period + "[" + i + "].name must be a string");

                int position = entry.get("position").asInt();
                if (i == 0) {
                    assertEquals(1, position,
                            period + ": first entry's position must be 1");
                }
                // Ties produce repeated positions (e.g. "1, 1, 3, 3"); we only
                // require the sequence to be non-decreasing.
                assertTrue(position >= previousPosition,
                        period + "[" + i + "]: position must be non-decreasing"
                                + " (got " + position + " after " + previousPosition + ")");
                previousPosition = position;
            }
        }
    }

    @Test
    void deletedPlayer_droppedFromRankings_existingScoresUnchanged() {
        // Create a temp player + partner, play a game, then delete the temp.
        // The temp player's entry must be REMOVED from /pointsPrPlayer (not
        // renamed to "Deleted player"); their game still contributes to the
        // partner's score, so the partner's points must be unchanged.
        String ghost = "GhostTestP";
        String partner = "ContractTestPlayer";

        given().when().delete("/players/" + ghost);
        ensurePlayer(ghost);
        ensurePlayer(partner);

        String gameBody = "[{\"player_red_1\":\"" + ghost + "\","
                + "\"player_red_2\":\"" + partner + "\","
                + "\"player_blue_1\":\"" + partner + "\","
                + "\"player_blue_2\":\"" + ghost + "\","
                + "\"lastUpdated\":\"2026-05-25 12:00:00.0\","
                + "\"match_winner\":\"red\",\"points_at_stake\":7,\"winning_table\":1}]";
        given().header("Content-Type", "application/json").body(gameBody)
                .when().post("/games/").then().statusCode(200);

        JsonNode before = given().when().get("/pointsPrPlayer/alltime")
                .then().statusCode(200)
                .extract().body().as(JsonNode.class);
        assertTrue(streamNames(before).anyMatch(n -> n.equals(ghost)),
                "ghost player must appear by name before deletion");

        given().when().delete("/players/" + ghost).then().statusCode(200);

        JsonNode after = given().when().get("/pointsPrPlayer/alltime")
                .then().statusCode(200)
                .extract().body().as(JsonNode.class);
        assertFalse(streamNames(after).anyMatch(n -> n.equals(ghost)),
                "deleted player must no longer appear by their real name");
        assertFalse(streamNames(after).anyMatch(n -> n.equals("Deleted player")),
                "deleted player must not appear in rankings under any name");

        JsonNode partnerBefore = findByName(before, partner);
        JsonNode partnerAfter = findByName(after, partner);
        assertNotEquals(null, partnerBefore, "partner must exist before");
        assertNotEquals(null, partnerAfter, "partner must exist after");
        assertEquals(partnerBefore.get("points").asInt(),
                partnerAfter.get("points").asInt(),
                "partner's points must not change when their opponent is deleted");
    }

    private static java.util.stream.Stream<String> streamNames(JsonNode arr) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (JsonNode r : arr) out.add(r.get("name").asText());
        return out.stream();
    }

    private static JsonNode findByName(JsonNode arr, String name) {
        for (JsonNode r : arr) if (r.get("name").asText().equals(name)) return r;
        return null;
    }

    private static void ensurePlayer(String name) {
        String body = "[{\"name\":\"" + name
                + "\",\"playerReady\":true,\"oprettet\":\"2026-05-25 21:00:00.0\","
                + "\"registeredRFIDTag\":\"\"}]";
        given().header("Content-Type", "application/json").body(body)
                .when().post("/players/").then().statusCode(200);
    }

    @AfterAll
    static void cleanup() {
        for (String n : new String[] {"GhostTestP", "ContractTestPlayer"}) {
            given().when().delete("/players/" + n);
        }
    }
}
