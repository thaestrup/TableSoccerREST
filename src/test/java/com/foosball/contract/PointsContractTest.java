package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the PointsPrPlayer resource (legacy:
 * {@code PointsPrPlayer.groovy}, default "newElo" branch).
 *
 * <p>Reference fixtures: {@code points-alltime.json}, {@code points-week.json},
 * {@code points-month.json}, {@code points-day.json}.
 *
 * <p>Documented quirks:
 * <ul>
 *   <li>Returns a JSON array of {@code {position, points, numberOfGames, name}}
 *       objects. All four fields are read by the frontend; all must be
 *       present and non-null.</li>
 *   <li>{@code position} starts at 1. With ties, the same {@code position}
 *       value can repeat for tied players (e.g. {@code 1, 1, 3, 3}). The
 *       sequence is non-decreasing — assert that, not strict monotonicity.</li>
 *   <li>The five frontend-supported period tokens are
 *       {@code hour|day|week|month|alltime}. The dropped filter tokens
 *       ({@code alltime-elo}, {@code alltime-onlylunch},
 *       {@code alltime-ratiofocus}) are NOT exercised by this suite — see
 *       FRONTEND-USAGE.md drop list.</li>
 * </ul>
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
}
