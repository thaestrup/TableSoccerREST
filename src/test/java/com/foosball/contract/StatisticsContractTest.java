package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the {@code /statisticsPlayersLastPlayed} endpoint.
 * Returned as a JSON object keyed by player name (NOT an array) with
 * epoch-millis numeric values.
 */
class StatisticsContractTest extends ContractSuite {

    @Test
    void getStatisticsPlayersLastPlayed_returnsObjectMapOfNameToEpochMs() {
        JsonNode body = given()
                .when().get("/statisticsPlayersLastPlayed")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);

        assertTrue(body.isObject(), "/statisticsPlayersLastPlayed must return a JSON object, not an array");
        assertTrue(body.size() > 0, "expected at least one player entry");

        Iterator<Map.Entry<String, JsonNode>> it = body.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode value = entry.getValue();
            assertTrue(value.isNumber(),
                    "value for '" + entry.getKey() + "' must be a number, got " + value.getNodeType());
            // Epoch millis: must be a positive integral number.
            assertTrue(value.asLong() > 0,
                    "value for '" + entry.getKey() + "' must be a positive epoch-ms");
        }
    }
}
