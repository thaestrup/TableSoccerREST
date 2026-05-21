package com.foosball.dto;

import com.foosball.domain.Player;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Converters between {@link Player} entity and {@link PlayerDto} wire shape.
 * Kept static and stateless — no CDI involved.
 *
 * <p>{@code oprettet} bridges {@code LocalDateTime} (entity) and the legacy
 * {@link Timestamp#toString()} string format on the wire.
 */
public final class PlayerMapper {

    private PlayerMapper() {}

    public static PlayerDto toDto(Player p) {
        return new PlayerDto(
                p.name,
                p.playerReady,
                Timestamp.valueOf(p.oprettet).toString(),
                p.registeredRFIDTag);
    }

    public static void apply(PlayerDto dto, Player target, String overrideName) {
        target.name = overrideName != null ? overrideName : dto.name();
        target.playerReady = dto.playerReady();
        target.oprettet = Timestamp.valueOf(dto.oprettet()).toLocalDateTime();
        target.registeredRFIDTag = dto.registeredRFIDTag() != null ? dto.registeredRFIDTag() : "";
    }
}
