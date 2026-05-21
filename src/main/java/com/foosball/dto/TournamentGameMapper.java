package com.foosball.dto;

import java.sql.Timestamp;

/**
 * Builds prefilled {@link GameDto} instances for tournament responses.
 *
 * <p>Tournament games are <em>proposed</em>, not persisted: legacy uses
 * sentinel values to signal "not yet a real game" — {@code id=-1},
 * {@code points_at_stake=-1}, {@code winning_table=-1},
 * {@code match_winner=""}. The {@code lastUpdated} string is the legacy
 * {@link Timestamp#toString()} format ({@code yyyy-MM-dd HH:mm:ss.S},
 * variable-precision fractional seconds), generated at the moment of
 * tournament generation.
 *
 * <p>Player slots that come back from the algorithm as Java {@code null}
 * (e.g. odd player counts that leave a partner empty) are emitted as the
 * literal string {@code "null"} to preserve the legacy quirk the React
 * frontend depends on.
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

    /** Legacy quirk: unfilled slots are the literal string {@code "null"}, not JSON null. */
    public static String nullToString(String s) {
        return s == null ? "null" : s;
    }
}
