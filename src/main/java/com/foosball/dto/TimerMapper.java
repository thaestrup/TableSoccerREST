package com.foosball.dto;

import com.foosball.domain.TimerState;
import java.sql.Timestamp;

/**
 * Converter from {@link TimerState} entity to {@link TimerActionDto} wire shape.
 * {@code lastRequestedTimerStart} bridges {@code LocalDateTime} (entity)
 * and the {@link Timestamp#toString()} string on the wire.
 */
public final class TimerMapper {

    private TimerMapper() {}

    public static TimerActionDto toDto(TimerState t) {
        return new TimerActionDto(
                t.id,
                Timestamp.valueOf(t.lastRequestedTimerStart).toString());
    }
}
