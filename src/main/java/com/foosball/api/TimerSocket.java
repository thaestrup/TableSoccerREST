package com.foosball.api;

import com.foosball.domain.TimerState;
import com.foosball.dto.TimerActionDto;
import com.foosball.dto.TimerMapper;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import jakarta.enterprise.context.control.ActivateRequestContext;

/**
 * WebSocket push endpoint for the match timer.
 *
 * <p>Subscribers receive the current {@link TimerActionDto} on connect, and
 * a fresh broadcast every time {@link TimerResource#reset()} fires. This
 * replaces the legacy 1 Hz polling on {@code GET /timer} from the React
 * frontend — when the frontend swaps to subscribing here, the polling
 * traffic disappears.
 *
 * <p>{@code GET /timer} stays available unchanged for any non-WS consumer
 * (curl, scripts, the openapi/swagger-ui browser). The wire shape on both
 * paths is identical: {@code {id, lastRequestedTimerStart}}.
 *
 * <p>The endpoint id used by {@link io.quarkus.websockets.next.OpenConnections}
 * matches this class's fully-qualified name.
 */
@WebSocket(path = "/ws/timer")
public class TimerSocket {

    @OnOpen
    @ActivateRequestContext
    public TimerActionDto onOpen() {
        // WS connections don't carry an HTTP request context. Panache needs
        // an active CDI request scope (or a transaction) to use the Hibernate
        // session — @ActivateRequestContext gives us the scope for the
        // duration of this single read.
        TimerState row = TimerState.findById(TimerResource.TIMER_ROW_ID);
        return row == null ? null : TimerMapper.toDto(row);
    }
}
