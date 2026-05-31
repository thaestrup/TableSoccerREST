package com.foosball.api;

import com.foosball.domain.TimerState;
import com.foosball.dto.TimerActionDto;
import com.foosball.dto.TimerMapper;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import jakarta.enterprise.context.control.ActivateRequestContext;

/**
 * WebSocket push endpoint at {@code /ws/timer}. Subscribers receive the
 * current {@link TimerActionDto} on connect, then a fresh broadcast every
 * time {@link TimerResource#reset()} fires. Wire shape matches
 * {@code GET /timer}: {@code {id, lastRequestedTimerStart}}.
 */
@WebSocket(path = "/ws/timer")
public class TimerSocket {

    @OnOpen
    @ActivateRequestContext
    public TimerActionDto onOpen() {
        // @ActivateRequestContext supplies the CDI request scope Panache
        // needs for its Hibernate session (WS connections don't carry one).
        TimerState row = TimerState.findById(TimerResource.TIMER_ROW_ID);
        return row == null ? null : TimerMapper.toDto(row);
    }
}
