package com.foosball.service;

import com.foosball.domain.Game;
import com.foosball.dto.PointsPlayerDto;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure leaderboard ranker — port of the {@code asdasd} method in the legacy
 * {@code PointsPrPlayer.groovy}.
 *
 * <p>The legacy switch dispatches the request to one of four strategy
 * branches via a {@code filter} string:
 * <ul>
 *   <li>{@code "newElo"} — used for the {@code alltime} period token.
 *       Initializes every encountered player at 1500 points and applies
 *       {@code ± points_at_stake} per game.</li>
 *   <li>{@code "theOldEloWhichWeDontUse"} — dead branch, never reached.</li>
 *   <li>{@code "ratiofocus"} — only reached by {@code alltime-ratiofocus}
 *       (dropped per FRONTEND-USAGE.md).</li>
 *   <li>{@code ""} (the default {@code else} branch) — used for
 *       {@code month}, {@code week}, {@code day}, {@code hour}. Same
 *       per-game {@code ± points_at_stake} math as {@code newElo} but
 *       starts each player at 0 instead of 1500.</li>
 * </ul>
 *
 * <p>The two relevant branches collapse into one algorithm parameterised on
 * the starting score: 1500 for {@code alltime}, 0 for the rolling windows.
 * The captured fixtures confirm this — {@code points-alltime.json}'s scores
 * differ from {@code points-week.json}'s by exactly 1500 for every player.
 *
 * <p>Quirks preserved:
 * <ul>
 *   <li>The legacy compares {@code game.player_blue_2 != "null"} (string
 *       comparison against the literal {@code "null"}) because the legacy
 *       DB stored absent slots as the four-character string {@code "null"}.
 *       Mirrored here so re-imported legacy rows still rank correctly.</li>
 *   <li>Players with zero games never enter the score map.</li>
 *   <li>Tied scores produce repeated {@code position} values
 *       ({@code 1, 1, 3, 3}).</li>
 *   <li>Unknown {@code match_winner} values (anything other than
 *       {@code red}/{@code blue}/{@code draw}) are skipped — the entry is
 *       still seeded at the starting score and counted toward
 *       {@code numberOfGames}, matching the legacy behavior of running the
 *       {@code putIfAbsent} initializers on every game regardless of
 *       winner.</li>
 * </ul>
 */
@ApplicationScoped
public class EloService {

    private static final int ELO_START = 1500;
    private static final int DEFAULT_START = 0;
    private static final String NULL_SLOT = "null";

    /**
     * Rank players using the {@code newElo} starting score (1500). Used for
     * the {@code alltime} period token.
     */
    public List<PointsPlayerDto> rankWithEloStart(List<Game> games) {
        return rank(games, ELO_START);
    }

    /**
     * Rank players using the default starting score (0). Used for the
     * {@code hour}/{@code day}/{@code week}/{@code month} period tokens.
     */
    public List<PointsPlayerDto> rankWithDefaultStart(List<Game> games) {
        return rank(games, DEFAULT_START);
    }

    private List<PointsPlayerDto> rank(List<Game> games, int startingScore) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        Map<String, Integer> numberOfGames = new LinkedHashMap<>();

        for (Game game : games) {
            seedPlayer(scores, numberOfGames, game.playerBlue1, startingScore);
            seedPlayer(scores, numberOfGames, game.playerBlue2, startingScore);
            seedPlayer(scores, numberOfGames, game.playerRed1, startingScore);
            seedPlayer(scores, numberOfGames, game.playerRed2, startingScore);

            incrementGames(numberOfGames, game.playerBlue1);
            incrementGames(numberOfGames, game.playerBlue2);
            incrementGames(numberOfGames, game.playerRed1);
            incrementGames(numberOfGames, game.playerRed2);

            int delta = game.pointsAtStake;
            String winner = game.matchWinner;
            if ("blue".equals(winner)) {
                applyDelta(scores, game.playerBlue1, delta);
                applyDelta(scores, game.playerBlue2, delta);
                applyDelta(scores, game.playerRed1, -delta);
                applyDelta(scores, game.playerRed2, -delta);
            } else if ("red".equals(winner)) {
                applyDelta(scores, game.playerRed1, delta);
                applyDelta(scores, game.playerRed2, delta);
                applyDelta(scores, game.playerBlue1, -delta);
                applyDelta(scores, game.playerBlue2, -delta);
            } else if ("draw".equals(winner)) {
                applyDelta(scores, game.playerBlue1, delta);
                applyDelta(scores, game.playerBlue2, delta);
                applyDelta(scores, game.playerRed1, delta);
                applyDelta(scores, game.playerRed2, delta);
            }
            // Unknown winner: skip delta application (defensive — schema
            // permits any 20-char string).
        }

        return assemble(scores, numberOfGames);
    }

    private void seedPlayer(Map<String, Integer> scores,
                            Map<String, Integer> numberOfGames,
                            String name,
                            int startingScore) {
        if (isPresent(name)) {
            scores.putIfAbsent(name, startingScore);
            numberOfGames.putIfAbsent(name, 0);
        }
    }

    private void incrementGames(Map<String, Integer> numberOfGames, String name) {
        if (isPresent(name)) {
            numberOfGames.computeIfPresent(name, (k, v) -> v + 1);
        }
    }

    private void applyDelta(Map<String, Integer> scores, String name, int delta) {
        if (isPresent(name)) {
            scores.computeIfPresent(name, (k, v) -> v + delta);
        }
    }

    private boolean isPresent(String slot) {
        return slot != null && !NULL_SLOT.equals(slot);
    }

    private List<PointsPlayerDto> assemble(Map<String, Integer> scores,
                                           Map<String, Integer> numberOfGames) {
        // Sort entries by score descending; tied scores get repeated
        // positions (e.g. [1500, 1500, 1490] -> positions [1, 1, 3]).
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed());

        List<PointsPlayerDto> result = new ArrayList<>(sorted.size());
        int previousScore = Integer.MIN_VALUE;
        int previousPosition = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            int score = entry.getValue();
            int position = (i == 0 || score != previousScore) ? (i + 1) : previousPosition;
            previousScore = score;
            previousPosition = position;
            result.add(new PointsPlayerDto(
                    position,
                    score,
                    numberOfGames.getOrDefault(entry.getKey(), 0),
                    entry.getKey()));
        }
        return result;
    }
}
