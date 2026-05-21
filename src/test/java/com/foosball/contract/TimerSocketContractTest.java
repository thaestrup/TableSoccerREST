package com.foosball.contract;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Contract test for the {@code /ws/timer} WebSocket push channel.
 *
 * <p><strong>Not a parity test.</strong> The legacy Ratpack backend never had a
 * WebSocket endpoint — the React frontend used to poll {@code GET /timer}
 * every second. The Quarkus port adds {@link com.foosball.api.TimerSocket}
 * as a push side-channel; this file pins the wire contract so the frontend's
 * subscription path keeps working.
 *
 * <p>If the backend under test lacks the endpoint (the handshake fails with
 * 404 or the connection refuses), the test self-skips via
 * {@link org.junit.jupiter.api.Assumptions#assumeTrue(boolean)} — that lets
 * the suite stay hostable against the legacy backend without false failures.
 *
 * <p>Wire contract:
 * <ul>
 *   <li>On connect, the server sends one {@code TimerActionDto} JSON frame
 *       reflecting the current singleton row.</li>
 *   <li>Every {@code POST /timer} triggers a broadcast of the new
 *       {@code TimerActionDto} to every open subscription. The frame is
 *       always a single text message of complete JSON (no chunking
 *       within an individual reset event).</li>
 * </ul>
 */
class TimerSocketContractTest extends ContractSuite {

    @Test
    void wsTimer_broadcastsCurrentStateOnConnect_andOnReset() throws Exception {
        URI wsUri = wsTimerUri();
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        WebSocket ws;
        try {
            ws = openSocket(wsUri, inbox);
        } catch (Throwable handshakeFail) {
            // Legacy backend doesn't have /ws/timer — skip cleanly so the
            // suite still runs against http://localhost:5050.
            assumeTrue(false, "Backend at " + wsUri + " has no /ws/timer: " + handshakeFail.getMessage());
            return; // unreachable, but keeps the compiler happy
        }
        try {
            // 1. The on-open payload should arrive within a generous window.
            String first = inbox.poll(5, TimeUnit.SECONDS);
            assertNotNull(first, "no on-open frame received within 5s on " + wsUri);
            JsonNode firstDto = MAPPER.readTree(first);
            assertTimerActionShape(firstDto);
            String before = firstDto.get("lastRequestedTimerStart").asText();

            // 2. MariaDB TIMESTAMP precision is per-second by default. Sleep
            // just past one second so NOW() ticks before we POST and the
            // broadcast frame carries a strictly-newer timestamp.
            Thread.sleep(1100);

            // 3. Reset triggers the broadcast. We ignore the HTTP response
            // body here; the wire shape is asserted in TimerContractTest.
            given().when().post("/timer").then().statusCode(200);

            // 4. The same open WS should receive a fresh frame.
            String pushed = inbox.poll(5, TimeUnit.SECONDS);
            assertNotNull(pushed, "no broadcast frame received after POST /timer");
            JsonNode pushedDto = MAPPER.readTree(pushed);
            assertTimerActionShape(pushedDto);
            String after = pushedDto.get("lastRequestedTimerStart").asText();

            // Legacy Timestamp.toString() is lexicographically orderable.
            assertTrue(after.compareTo(before) > 0,
                    "broadcast must advance lastRequestedTimerStart: before=" + before
                            + " after=" + after);
        } finally {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "test done")
                        .get(2, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }

    private static URI wsTimerUri() {
        String baseUrl = System.getProperty("contract.baseUrl", DEFAULT_BASE_URL);
        // http://host:port → ws://host:port/ws/timer (https → wss analogously).
        String wsBase = baseUrl.replaceFirst("^http", "ws");
        return URI.create(wsBase + "/ws/timer");
    }

    /**
     * Opens a WebSocket and routes complete text frames into {@code inbox}.
     * Throws if the handshake fails or times out — the caller skips the test
     * in that case.
     */
    private static WebSocket openSocket(URI uri, BlockingQueue<String> inbox) throws Exception {
        StringBuilder buffer = new StringBuilder();
        CompletableFuture<WebSocket> futureWs = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .buildAsync(uri, new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
                        buffer.append(data);
                        if (last) {
                            inbox.offer(buffer.toString());
                            buffer.setLength(0);
                        }
                        socket.request(1);
                        return null;
                    }
                });
        return futureWs.get(5, TimeUnit.SECONDS);
    }

    /**
     * Asserts the wire shape of a {@code TimerActionDto} as documented in
     * {@link TimerContractTest} — kept inline so this test file is
     * self-contained against the legacy timestamp regex on the base class.
     */
    private static void assertTimerActionShape(JsonNode dto) {
        assertNotNull(dto, "DTO must not be null");
        assertTrue(dto.isObject(), "DTO must be a JSON object");
        assertTrue(dto.has("id"), "missing 'id'");
        assertTrue(dto.get("id").isInt(), "'id' must be an int");
        assertTrue(dto.has("lastRequestedTimerStart"), "missing 'lastRequestedTimerStart'");
        String ts = dto.get("lastRequestedTimerStart").asText();
        assertThat("lastRequestedTimerStart must be in legacy Timestamp format",
                ts, matchesPattern(LEGACY_TIMESTAMP_REGEX));
    }
}
