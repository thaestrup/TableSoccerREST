package com.foosball.dto;

import com.foosball.domain.TimerState;
import java.sql.Timestamp;

/**
 * Converter from {@link TimerState} entity to {@link TimerActionDto} wire shape.
 * Kept static and stateless — no CDI involved.
 *
 * <p>{@code lastRequestedTimerStart} bridges {@code LocalDateTime} (entity)
 * and the legacy {@link Timestamp#toString()} string format on the wire.
 */
public final class TimerMapper {

    private TimerMapper() {}

    public static TimerActionDto toDto(TimerState t) {
        return new TimerActionDto(
                t.id,
                Timestamp.valueOf(t.lastRequestedTimerStart).toString());
    }
}
