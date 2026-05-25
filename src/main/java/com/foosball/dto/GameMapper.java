package com.foosball.dto;

import com.foosball.domain.Game;
import java.sql.Timestamp;
import java.util.Set;

public final class GameMapper {

    public static final String DELETED_PLAYER = "Deleted player";

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

    /**
     * Same as {@link #toDto(Game)} but replaces any player-slot name not in
     * {@code activeNames} with {@code "Deleted player"}. The
     * {@code "null"}-string sentinel for empty back-row slots is preserved.
     */
    public static GameDto toDto(Game g, Set<String> activeNames) {
        return new GameDto(
                g.id,
                mapName(g.playerRed1, activeNames),
                mapName(g.playerRed2, activeNames),
                mapName(g.playerBlue1, activeNames),
                mapName(g.playerBlue2, activeNames),
                Timestamp.valueOf(g.timestamp).toString(),
                g.matchWinner,
                g.pointsAtStake,
                g.winningTable);
    }

    private static String mapName(String name, Set<String> activeNames) {
        if (name == null || "null".equals(name) || activeNames.contains(name)) {
            return name;
        }
        return DELETED_PLAYER;
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
     * Applies the editable subset of {@code dto} onto {@code target}.
     * {@code id} and {@code timestamp} are preserved; all other fields
     * are replaced. Used by {@code PUT /games/{id}}.
     */
    public static void applyEditable(GameDto dto, Game target) {
        target.playerRed1 = orNullString(dto.playerRed1());
        target.playerRed2 = orNullString(dto.playerRed2());
        target.playerBlue1 = orNullString(dto.playerBlue1());
        target.playerBlue2 = orNullString(dto.playerBlue2());
        target.matchWinner = dto.matchWinner();
        target.pointsAtStake = dto.pointsAtStake();
        target.winningTable = dto.winningTable();
    }

    // JSON null on a back-row slot → the literal "null" string sentinel.
    // tbl_fights.player_*_2 is NOT NULL; the React GameSchema transforms
    // "null" back into JS null on read.
    private static String orNullString(String s) {
        return s == null ? "null" : s;
    }
}
