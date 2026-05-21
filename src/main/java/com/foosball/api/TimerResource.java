package com.foosball.api;

import com.foosball.domain.TimerState;
import com.foosball.dto.TimerActionDto;
import com.foosball.dto.TimerMapper;
import io.quarkus.websockets.next.OpenConnections;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Timer resource — port of legacy {@code TimerActions.groovy}, extended with
 * a WebSocket push side-channel.
 *
 * <p>Wire contract preserved:
 * <ul>
 *   <li>{@code GET /timer} returns a JSON <em>array</em> with a single
 *       {@code {id, lastRequestedTimerStart}} element, even though the
 *       underlying table is a singleton mailbox row. Frontend reads
 *       {@code response[0].lastRequestedTimerStart}.</li>
 *   <li>{@code POST /timer} resets {@code lastRequestedTimerStart} to now,
 *       returning a plain-text result string (legacy emitted
 *       {@code "result: <last-insert-id>"} — we emit a similarly-shaped
 *       string for parity), AND broadcasts the new {@link TimerActionDto}
 *       to every subscriber on {@link TimerSocket} (path {@code /ws/timer}).</li>
 * </ul>
 *
 * <p>The push channel removes the legacy 1 Hz polling on {@code GET /timer}
 * once the frontend swaps to a WS subscription. Polling stays available for
 * any consumer that can't / doesn't want to use WS.
 *
 * <p>The per-request {@code MoreUtil.ensureTimerTableExist()} DDL call is
 * dropped — Flyway V1 owns schema creation now.
 */
@Path("/timer")
@Produces(MediaType.APPLICATION_JSON)
public class TimerResource {

    /** Package-private so {@link TimerSocket} can read the singleton row on connect. */
    static final int TIMER_ROW_ID = 1;

    @Inject
    OpenConnections connections;

    @GET
    public List<TimerActionDto> listAll() {
        return TimerState.<TimerState>listAll()
                .stream()
                .map(TimerMapper::toDto)
                .toList();
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response reset() {
        TimerState row = TimerState.findById(TIMER_ROW_ID);
        if (row == null) {
            row = new TimerState();
            row.id = TIMER_ROW_ID;
        }
        row.lastRequestedTimerStart = LocalDateTime.now();
        row.persist();

        // Broadcast to every TimerSocket subscriber. The send is best-effort
        // and non-blocking (sendTextAndAwait would tie the HTTP response to
        // the slowest WS client). If the broadcast fails (no subscribers,
        // bad client), the GET fallback path still picks up the new state.
        TimerActionDto dto = TimerMapper.toDto(row);
        connections.findByEndpointId(TimerSocket.class.getName())
                .forEach(c -> c.sendText(dto).subscribe().with(
                        ignored -> {},
                        ignored -> {}));

        return Response.ok("result: " + row.id).type(MediaType.TEXT_PLAIN).build();
    }
}
