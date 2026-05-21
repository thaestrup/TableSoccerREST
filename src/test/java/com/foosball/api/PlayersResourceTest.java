package com.foosball.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Smoke test for {@link PlayersResource}. Spins up Quarkus + a Dev Services
 * MariaDB and verifies the migration + seed data + endpoints round-trip.
 *
 * <p>Not a parity test — that lives in {@code com.foosball.contract}.
 */
@QuarkusTest
class PlayersResourceTest {

    @Test
    void listsSeededPlayers() {
        given()
                .when().get("/players")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(40)))
                .body("[0].name", notNullValue())
                .body("[0].playerReady", instanceOf(Boolean.class))
                .body("[0].oprettet", matchesPattern("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,9}$"));
    }

    @Test
    void getsByName() {
        given()
                .when().get("/players/Carsten")
                .then()
                .statusCode(200)
                .body("name", equalTo("Carsten"))
                .body("registeredRFIDTag", equalTo(""));
    }

    @Test
    void unknownPlayerReturns404() {
        given()
                .when().get("/players/NotARealPlayerName_xxx")
                .then()
                .statusCode(404);
    }

    @Test
    void postAcceptsArrayWrapper() {
        // Mirrors what the React frontend actually sends:
        // JSON.stringify([player]) -- one player wrapped in a single-element array.
        given()
                .contentType("application/json")
                .body("[{\"name\":\"ContractSmokePlayer\",\"playerReady\":true,"
                        + "\"oprettet\":\"2026-05-10 21:00:00.0\","
                        + "\"registeredRFIDTag\":\"\"}]")
                .when().post("/players")
                .then()
                .statusCode(200)
                .body(startsWith("insertPlayer: ContractSmokePlayer, result: "));

        given()
                .when().get("/players/ContractSmokePlayer")
                .then()
                .statusCode(200)
                .body("name", equalTo("ContractSmokePlayer"))
                .body("playerReady", equalTo(true));
    }

    @Test
    void putUpsertsAndFlipsReady() {
        // Ensure the player exists first (idempotent — insertOne handles re-runs).
        given()
                .contentType("application/json")
                .body("[{\"name\":\"PutTestPlayer\",\"playerReady\":false,"
                        + "\"oprettet\":\"2026-05-10 21:00:00.0\","
                        + "\"registeredRFIDTag\":\"\"}]")
                .when().post("/players")
                .then().statusCode(200);

        given()
                .contentType("application/json")
                .body("{\"name\":\"PutTestPlayer\",\"playerReady\":true,"
                        + "\"oprettet\":\"2026-05-10 21:00:00.0\","
                        + "\"registeredRFIDTag\":\"\"}")
                .when().put("/players/PutTestPlayer")
                .then()
                .statusCode(200)
                .body(startsWith("overwritePlayer: PutTestPlayer, result: "));

        given()
                .when().get("/players/PutTestPlayer")
                .then()
                .statusCode(200)
                .body("playerReady", equalTo(true));
    }
}
