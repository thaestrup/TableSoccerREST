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
 * Pure leaderboard ranker. Walks a list of {@link Game} rows and produces a
 * sorted {@link PointsPlayerDto} list.
 *
 * <p>Math per game: the winning side gains {@code points_at_stake}, the
 * losing side loses the same amount; draws credit both sides. Players
 * start at 1500 for the {@code alltime} window and at 0 for the rolling
 * windows ({@code hour}/{@code day}/{@code week}/{@code month}).
 *
 * <p>Behavior:
 * <ul>
 *   <li>Player slots equal to the literal {@code "null"} string (or actual
 *       {@code null}) are ignored — that's the empty-back-row sentinel
 *       used for 1v1/2v1 games.</li>
 *   <li>Players with zero games never appear.</li>
 *   <li>Tied scores produce repeated {@code position} values
 *       (e.g. {@code 1, 1, 3, 3}).</li>
 *   <li>Unknown {@code match_winner} values (not {@code red}/{@code blue}/{@code draw})
 *       count toward {@code numberOfGames} but apply no delta.</li>
 * </ul>
 */
@ApplicationScoped
public class EloService {

    private static final int ELO_START = 1500;
    private static final int DEFAULT_START = 0;
    private static final String NULL_SLOT = "null";

    /** Rank using starting score 1500 (the {@code alltime} window). */
    public List<PointsPlayerDto> rankWithEloStart(List<Game> games) {
        return rank(games, ELO_START);
    }

    /** Rank using starting score 0 (the rolling-window periods). */
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
            // Unknown winner: skip delta application.
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
