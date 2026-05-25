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
 * Timer endpoints.
 *
 * <ul>
 *   <li>{@code GET /timer} returns a single-element JSON array
 *       {@code [{id, lastRequestedTimerStart}]}.</li>
 *   <li>{@code POST /timer} resets {@code lastRequestedTimerStart} to now,
 *       returns plain text {@code "result: <id>"}, and broadcasts the new
 *       {@link TimerActionDto} to every subscriber on {@link TimerSocket}.</li>
 * </ul>
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

        // Broadcast best-effort and non-blocking; the HTTP response shouldn't
        // wait on the slowest WS subscriber.
        TimerActionDto dto = TimerMapper.toDto(row);
        connections.findByEndpointId(TimerSocket.class.getName())
                .forEach(c -> c.sendText(dto).subscribe().with(
                        ignored -> {},
                        ignored -> {}));

        return Response.ok("result: " + row.id).type(MediaType.TEXT_PLAIN).build();
    }
}
