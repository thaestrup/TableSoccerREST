package com.foosball.dto;

import java.sql.Timestamp;

/**
 * Builds prefilled {@link GameDto} instances for tournament responses.
 * Tournament games are <em>proposed</em>, not persisted: sentinels signal
 * "not yet a real game" — {@code id=-1}, {@code points_at_stake=-1},
 * {@code winning_table=-1}, {@code match_winner=""}. Empty player slots
 * are emitted as the literal string {@code "null"}, not JSON null.
 */
public final class TournamentGameMapper {

    private TournamentGameMapper() {}

    /** Sentinel id for not-yet-persisted tournament games. */
    public static final long SYNTHETIC_ID = -1L;

    /** Sentinel value for points_at_stake / winning_table on synthetic games. */
    public static final int SYNTHETIC_INT = -1;

    /** Builds a synthetic tournament {@link GameDto} for the four supplied slots. */
    public static GameDto synthetic(String red1, String red2, String blue1, String blue2) {
        return new GameDto(
                SYNTHETIC_ID,
                nullToString(red1),
                nullToString(red2),
                nullToString(blue1),
                nullToString(blue2),
                new Timestamp(System.currentTimeMillis()).toString(),
                "",
                SYNTHETIC_INT,
                SYNTHETIC_INT);
    }

    /** Unfilled slots are the literal string {@code "null"}, not JSON null. */
    public static String nullToString(String s) {
        return s == null ? "null" : s;
    }
}
