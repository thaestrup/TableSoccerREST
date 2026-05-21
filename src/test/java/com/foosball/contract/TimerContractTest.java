package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the Timer resource (legacy: {@code Timer.groovy}).
 *
 * <p>Reference fixture: {@code timer.json}.
 *
 * <p>Documented quirks:
 * <ul>
 *   <li>{@code GET /timer} returns a JSON ARRAY containing exactly one
 *       element, even though the timer is logically a singleton. Frontend
 *       reads {@code response[0].lastRequestedTimerStart}.</li>
 *   <li>{@code lastRequestedTimerStart} is the legacy
 *       {@code Timestamp.toString()} format, not ISO-8601.</li>
 *   <li>{@code POST /timer} resets {@code lastRequestedTimerStart} to NOW;
 *       returns plain-text {@code "result: 1"} (legacy used
 *       {@code LAST_INSERT_ID()} which is meaningless for an UPDATE — we
 *       emit the row id instead for parity-shaped output). The Quarkus
 *       port preserves this. Wired to {@code Countdown.tsx} reset control
 *       (re-confirmed in use 2026-05-14 — the original audit missed it).</li>
 * </ul>
 */
class TimerContractTest extends ContractSuite {

    @Test
    void getTimer_returnsSingleElementArrayWithLegacyTimestamp() {
        JsonNode body = given()
                .when().get("/timer")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);

        assertTrue(body.isArray(), "/timer must return an array, not an object");
        assertEquals(1, body.size(), "/timer array must contain exactly one element");

        JsonNode entry = body.get(0);
        assertTrue(entry.isObject(), "/timer[0] must be an object");
        assertTrue(entry.has("id"), "/timer[0] missing 'id'");
        assertTrue(entry.has("lastRequestedTimerStart"),
                "/timer[0] missing 'lastRequestedTimerStart'");
        assertTrue(entry.get("id").isInt(), "id must be an int");

        String ts = entry.get("lastRequestedTimerStart").asText();
        assertThat("lastRequestedTimerStart must be in legacy Timestamp format",
                ts, matchesPattern(LEGACY_TIMESTAMP_REGEX));
    }

    @Test
    void postTimer_updatesLastRequestedTimerStart() throws InterruptedException {
        // Snapshot the current timestamp.
        String before = given()
                .when().get("/timer")
                .then().statusCode(200)
                .extract().body().jsonPath().getString("[0].lastRequestedTimerStart");

        // Sleep just long enough that NOW() resolves to a strictly larger
        // timestamp than `before`. MariaDB TIMESTAMP precision is per-second
        // by default, so 1.1s is the minimum that guarantees a tick.
        Thread.sleep(1100);

        String body = given()
                .when().post("/timer")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .extract().body().asString();

        // Legacy emitted "result: <last-insert-id>"; the Quarkus port mirrors
        // the shape with the singleton row id.
        assertThat("POST /timer body must start with 'result:'",
                body, matchesPattern("^result:\\s*\\d+\\s*$"));

        String after = given()
                .when().get("/timer")
                .then().statusCode(200)
                .extract().body().jsonPath().getString("[0].lastRequestedTimerStart");

        assertThat("lastRequestedTimerStart should still match the legacy format after POST",
                after, matchesPattern(LEGACY_TIMESTAMP_REGEX));
        assertTrue(after.compareTo(before) > 0,
                "POST /timer must advance lastRequestedTimerStart — before=" + before + " after=" + after);
    }
}
