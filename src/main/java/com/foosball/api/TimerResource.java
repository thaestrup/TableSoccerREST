package com.foosball.api;

import com.foosball.domain.TimerState;
import com.foosball.dto.TimerActionDto;
import com.foosball.dto.TimerMapper;
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
 * Timer resource — port of legacy {@code TimerActions.groovy}.
 *
 * <p>Wire contract preserved:
 * <ul>
 *   <li>{@code GET /timer} returns a JSON <em>array</em> with a single
 *       {@code {id, lastRequestedTimerStart}} element, even though the
 *       underlying table is a singleton mailbox row. Frontend reads
 *       {@code response[0].lastRequestedTimerStart}.</li>
 *   <li>{@code POST /timer} resets {@code lastRequestedTimerStart} to
 *       now, returning a plain-text result string (legacy emitted
 *       {@code "result: <last-insert-id>"} — we emit a similarly-shaped
 *       string for parity).</li>
 * </ul>
 *
 * <p>The per-request {@code MoreUtil.ensureTimerTableExist()} DDL call
 * is dropped — Flyway V1 owns schema creation now.
 */
@Path("/timer")
@Produces(MediaType.APPLICATION_JSON)
public class TimerResource {

    private static final int TIMER_ROW_ID = 1;

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
        return Response.ok("result: " + row.id).type(MediaType.TEXT_PLAIN).build();
    }
}
