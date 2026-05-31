package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for the contract test suite. Concrete tests run against the
 * URL given by {@code -Dcontract.baseUrl} (defaults to
 * {@code http://localhost:5050}); assertions hit that backend via
 * RestAssured.
 *
 * <p>Captured fixture JSON lives under {@code src/test/resources/fixtures/}
 * and represents the wire shape we are pinning. {@link #loadFixture(String)}
 * resolves them by short name.
 */
public abstract class ContractSuite {

    protected static final String DEFAULT_BASE_URL = "http://localhost:5050";

    /**
     * Wire-format date regex: {@code java.sql.Timestamp.toString()} —
     * {@code "yyyy-MM-dd HH:mm:ss.S"} with a space separator (not
     * {@code T}) and 1–9 trailing fractional digits.
     */
    protected static final String WIRE_TIMESTAMP_REGEX =
            "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,9}$";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void wireRestAssured() {
        RestAssured.baseURI = System.getProperty("contract.baseUrl", DEFAULT_BASE_URL);
        RestAssured.urlEncodingEnabled = true;
    }

    /**
     * Reads a captured fixture from {@code src/test/resources/fixtures/<name>.json}
     * and returns it as a Jackson {@link JsonNode}. Tests use these as
     * "expected wire shape" anchors.
     */
    protected static JsonNode loadFixture(String name) {
        Path path = Paths.get("src/test/resources/fixtures", name + ".json");
        try {
            return MAPPER.readTree(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load fixture: " + name, e);
        }
    }

    /**
     * @return {@code true} when the suite was launched with
     *         {@code -Dcontract.destructive=true}. Destructive tests
     *         (DELETE-all-games, etc.) opt in via this flag.
     */
    protected static boolean destructiveEnabled() {
        return Boolean.parseBoolean(System.getProperty("contract.destructive", "false"));
    }
}
