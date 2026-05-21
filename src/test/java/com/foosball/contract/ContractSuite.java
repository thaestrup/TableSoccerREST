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
 * Base class for the Phase 0 parity contract test suite.
 *
 * <p>The suite is hostable: every concrete test runs against an arbitrary
 * backend determined by the {@code contract.baseUrl} system property. The
 * default points at the legacy Ratpack backend on {@code localhost:5050};
 * re-running with {@code -Dcontract.baseUrl=http://localhost:5051} exercises
 * the new Quarkus port. Both backends MUST produce identical pass/fail
 * outcomes — that is the whole point of this suite.
 *
 * <p>Why plain JUnit and not {@code @QuarkusTest}: the Quarkus annotation
 * spins up the application under test inside the same JVM. We instead want
 * the assertions to talk to a freely chosen, externally hosted backend.
 *
 * <p>Captured fixture JSON lives under {@code src/test/resources/fixtures/}
 * and represents the wire shape we are pinning. {@link #loadFixture(String)}
 * resolves them by short name.
 */
public abstract class ContractSuite {

    protected static final String DEFAULT_BASE_URL = "http://localhost:5050";

    /**
     * Legacy {@code java.sql.Timestamp.toString()} format: e.g.
     * {@code "2016-10-09 19:46:36.0"}. Space separator (not {@code T}),
     * fractional second present. Tournament responses can carry up to
     * nanosecond precision (e.g. {@code "...:57.894"}). Stored rows in
     * {@code tbl_fights} only ever carry a single trailing {@code .0}.
     * Frontend depends on this exact shape via Zod schemas.
     */
    protected static final String LEGACY_TIMESTAMP_REGEX =
            "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{1,9}$";

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void wireRestAssured() {
        String baseUrl = System.getProperty("contract.baseUrl", DEFAULT_BASE_URL);
        RestAssured.baseURI = baseUrl;
        // Legacy backend stack-traces with a 200 status and text/html bodies on
        // some error paths; we never want RestAssured silently parsing those as
        // JSON. Each test asserts the content type explicitly when it cares.
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
     *         (DELETE-all-games, etc.) opt in via this flag instead of
     *         running by default — we don't want to wipe live data while
     *         the legacy backend is the contract source.
     */
    protected static boolean destructiveEnabled() {
        return Boolean.parseBoolean(System.getProperty("contract.destructive", "false"));
    }
}
