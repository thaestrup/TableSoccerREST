package com.foosball.dto;

import com.foosball.domain.Game;
import java.sql.Timestamp;

/**
 * Converters between {@link Game} entity and {@link GameDto} wire shape.
 * Kept static and stateless — no CDI involved.
 *
 * <p>{@code lastUpdated} bridges {@code LocalDateTime} (entity) and the legacy
 * {@link Timestamp#toString()} string format on the wire (variable-precision
 * fractional seconds — never ISO-8601). Same approach as
 * {@link PlayerMapper#toDto} uses for {@code oprettet}.
 *
 * <p>Player slots that already contain the literal string {@code "null"}
 * (legacy quirk: legacy Groovy concatenated {@code Object.toString()} into
 * INSERTs) are passed through verbatim — they are real {@code String}
 * values in the DB column, not SQL NULLs.
 */
public final class GameMapper {

    private GameMapper() {}

    public static GameDto toDto(Game g) {
        return new GameDto(
                g.id,
                g.playerRed1,
                g.playerRed2,
                g.playerBlue1,
                g.playerBlue2,
                Timestamp.valueOf(g.timestamp).toString(),
                g.matchWinner,
                g.pointsAtStake,
                g.winningTable);
    }

    public static Game toEntity(GameDto dto) {
        Game g = new Game();
        g.playerRed1 = orNullString(dto.playerRed1());
        g.playerRed2 = orNullString(dto.playerRed2());
        g.playerBlue1 = orNullString(dto.playerBlue1());
        g.playerBlue2 = orNullString(dto.playerBlue2());
        g.timestamp = Timestamp.valueOf(dto.lastUpdated()).toLocalDateTime();
        g.matchWinner = dto.matchWinner();
        g.pointsAtStake = dto.pointsAtStake();
        g.winningTable = dto.winningTable();
        return g;
    }

    /**
     * Legacy quirk preservation: when the React frontend reports a 1v1
     * or 2v1 game, the empty back-slot fields arrive as JSON {@code null}.
     * The legacy Groovy backend concatenated {@code String.valueOf(null)}
     * into INSERTs, which produced the 4-character literal {@code "null"}
     * stored in a {@code NOT NULL} column. The Quarkus port keeps the
     * column {@code NOT NULL} (and the entity {@code @NotNull}) to match
     * the schema, so we translate inbound JSON null → {@code "null"} here.
     * Subsequent reads emit the same {@code "null"} string on the wire,
     * which the frontend's {@code GameSchema} transforms back to JS null.
     * See FINDINGS-backend.md "Legacy parity quirks" section.
     */
    private static String orNullString(String s) {
        return s == null ? "null" : s;
    }
}
