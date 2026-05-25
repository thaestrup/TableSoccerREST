package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract tests for the Configuration resource. Returned as a JSON array
 * of {@code {name, value}} pairs; {@code numberOfTables} and
 * {@code nameTable<N>} must be present.
 */
class ConfigurationContractTest extends ContractSuite {

    @Test
    void getConfiguration_returnsArrayOfNameValuePairs() {
        JsonNode body = given()
                .when().get("/configuration/")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().as(JsonNode.class);

        assertTrue(body.isArray(), "/configuration must return a JSON array");
        assertTrue(body.size() > 0, "/configuration must not be empty");

        Set<String> keys = new HashSet<>();
        for (JsonNode entry : body) {
            assertTrue(entry.isObject(), "each entry must be an object");
            assertTrue(entry.has("name"), "entry missing 'name'");
            assertTrue(entry.has("value"), "entry missing 'value'");
            assertTrue(entry.get("name").isTextual(), "'name' must be a string");
            assertTrue(entry.get("value").isTextual(), "'value' must be a string");
            keys.add(entry.get("name").asText());
        }

        assertTrue(keys.contains("numberOfTables"),
                "/configuration must include 'numberOfTables' (frontend depends on it)");
        assertTrue(keys.contains("nameTable1"),
                "/configuration must include 'nameTable1' (frontend depends on it)");
    }
}
