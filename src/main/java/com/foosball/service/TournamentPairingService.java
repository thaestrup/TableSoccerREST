package com.foosball.service;

import com.foosball.domain.Game;
import com.foosball.dto.GameDto;
import com.foosball.dto.GamesPostRequestDto;
import com.foosball.dto.PlayerDto;
import com.foosball.dto.TournamentGameMapper;
import com.foosball.dto.TournamentGameRoundDto;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tournament pairing logic for the three algorithms. Random returns a flat
 * list of games; last-first and awesome wrap games in rounds.
 */
@ApplicationScoped
public class TournamentPairingService {

    /** Awesome algorithm only weighs games from the last hour for freshness scoring. */
    private static final int AWESOME_HOURS_BACK = 1;

    /**
     * A break longer than this between consecutive games causes the awesome
     * algorithm to stop walking further back when assigning round weights.
     */
    private static final int THRESHOLD_IN_MINUTES = 30;

    /** Maximum number of past rounds the awesome algorithm considers. */
    private static final int MAX_NUMBER_OF_ROUNDS_BACK = 25;

    /** Retry count for the pair-shuffle uniqueness search. */
    private static final int CHOSEN_NUMBER_OF_TRIES = 10;

    // -------------------------------------------------------------------
    // Random
    // -------------------------------------------------------------------

    /**
     * Port of {@code RandomTournament.generateGames}.
     * Shuffles names and pairs them off four-at-a-time. The wire shape
     * for random is a flat {@code List<GameDto>} — no rounds wrapper.
     */
    public List<GameDto> generateRandom(GamesPostRequestDto request) {
        List<GameDto> result = new ArrayList<>();
        if (request.players() == null) {
            return result;
        }
        LinkedList<String> randomPlayerNames = playerNames(request.players());
        Collections.shuffle(randomPlayerNames, new Random());

        int count = 0;
        while (!randomPlayerNames.isEmpty()) {
            // Slot fill order: red_1, blue_1, red_2, blue_2.
            String playerRed1 = randomPlayerNames.poll();
            String playerBlue1 = randomPlayerNames.poll();
            String playerRed2 = randomPlayerNames.poll();
            String playerBlue2 = randomPlayerNames.poll();
            result.add(TournamentGameMapper.synthetic(
                    playerRed1, playerRed2, playerBlue1, playerBlue2));
            count++;
            if (count >= request.numberOfGames()) {
                break;
            }
        }
        return result;
    }

    // -------------------------------------------------------------------
    // Last-first
    // -------------------------------------------------------------------

    /**
     * Sorts players by their last-played timestamp (least-recently-played
     * first), trims to the count needed by the table-saturation rules,
     * shuffles, and pairs off four-at-a-time.
     */
    public List<TournamentGameRoundDto> generateLastFirst(GamesPostRequestDto request) {
        List<GameDto> games = new ArrayList<>();
        if (request.players() != null) {
            Map<String, Long> playersLastPlayed = playersLastPlayed();

            LinkedList<String> randomPlayerNames = playerNames(request.players());
            Random random = new Random(System.currentTimeMillis());
            // First a noisy shuffle (so equal-timestamp ties break
            // randomly), then a stable sort by last-played puts the
            // freshest-rested players at the front.
            randomPlayerNames.sort(Comparator.comparingLong((String p) -> random.nextLong()));
            randomPlayerNames.sort(
                    Comparator.comparingLong((String p) -> nullSafe(playersLastPlayed.get(p))));

            int maxPlayersNeeded = adjustedMaxPlayersNeeded(
                    randomPlayerNames.size(), request.numberOfGames());
            // subList returns a view; copy into a fresh LinkedList so
            // mutations (poll) don't write back to the source list.
            LinkedList<String> realList = new LinkedList<>(
                    randomPlayerNames.subList(0, maxPlayersNeeded));
            Collections.shuffle(realList);

            int count = 0;
            while (!realList.isEmpty()) {
                String playerRed1 = realList.poll();
                String playerBlue1 = realList.poll();
                String playerRed2 = realList.poll();
                String playerBlue2 = realList.poll();
                // Skip games where blue has no first player — means we ran
                // out before completing a 2-on-2.
                if (playerBlue1 != null) {
                    games.add(TournamentGameMapper.synthetic(
                            playerRed1, playerRed2, playerBlue1, playerBlue2));
                }
                count++;
                if (count >= request.numberOfGames()) {
                    break;
                }
            }
        }
        return List.of(new TournamentGameRoundDto(games));
    }

    // -------------------------------------------------------------------
    // Awesome
    // -------------------------------------------------------------------

    /**
     * Pairing algorithm with a freshness-weighting heuristic: players who
     * have just played together rank lower as partners. Runs
     * {@value #CHOSEN_NUMBER_OF_TRIES} shuffle-retries and keeps the
     * uniqueness-best result.
     */
    public List<TournamentGameRoundDto> generateAwesome(GamesPostRequestDto request) {
        List<GameDto> result = new ArrayList<>();
        if (request.players() == null) {
            return List.of(new TournamentGameRoundDto(result));
        }

        LinkedList<String> namesOfAvailablePlayers = playerNames(request.players());

        int maxPlayersNeeded = adjustedMaxPlayersNeeded(
                namesOfAvailablePlayers.size(), request.numberOfGames());

        LinkedList<String> newRealList =
                generateAwesomeList(maxPlayersNeeded, namesOfAvailablePlayers);

        // Slot fill order: red_1, red_2, blue_1, blue_2 (differs from the
        // random / last-first algorithms which fill red_1, blue_1, red_2,
        // blue_2). Frontend tolerates either.
        LinkedList<String> realList = newRealList;
        int count = 0;
        while (!realList.isEmpty()) {
            String playerRed1 = realList.poll();
            String playerRed2 = realList.poll();
            String playerBlue1 = realList.poll();
            String playerBlue2 = realList.poll();
            if (playerBlue1 != null) {
                result.add(TournamentGameMapper.synthetic(
                        playerRed1, playerRed2, playerBlue1, playerBlue2));
            }
            count++;
            if (count >= request.numberOfGames()) {
                break;
            }
        }
        return List.of(new TournamentGameRoundDto(result));
    }

    // -------------------------------------------------------------------
    // generateAwesomeList internals
    // -------------------------------------------------------------------

    /** Mutable pair of player names used by the freshness-scoring search. */
    private static final class Pair {
        String player1;
        String player2;

        Pair() {
            this.player1 = null;
            this.player2 = null;
        }

        @Override
        public String toString() {
            return "P1: " + player1 + " - P2:" + player2;
        }
    }

    /**
     * Walks recent games to score how recently each pair has played
     * together, then runs a shuffle-retry uniqueness search to pick a
     * fresh-feeling ordering of {@code maxPlayersNeeded} players.
     */
    private LinkedList<String> generateAwesomeList(
            int maxPlayersNeeded, LinkedList<String> namesOfAvailablePlayers) {
        List<Game> games = Game.<Game>find(
                "timestamp > ?1 AND deletedAt IS NULL ORDER BY id DESC",
                LocalDateTime.now().minusHours(AWESOME_HOURS_BACK)).list();

        int maxNumberOfRoundsBack = MAX_NUMBER_OF_ROUNDS_BACK;
        int foundNumberOfRoundsBack = 0;

        List<Game> gamesToLookAt = new LinkedList<>();
        LocalDateTime currentLastUpdatedDate = null;

        for (Game game : games) {
            LocalDateTime gameLastUpdatedDate = game.timestamp;
            if (currentLastUpdatedDate == null) {
                currentLastUpdatedDate = gameLastUpdatedDate;
            }

            long fromCurrentLastToThis = toEpochMillis(currentLastUpdatedDate)
                    - toEpochMillis(gameLastUpdatedDate);

            // Don't go further if there has been a long break.
            if (fromCurrentLastToThis < (long) THRESHOLD_IN_MINUTES * 60 * 1000) {
                if (!currentLastUpdatedDate.equals(gameLastUpdatedDate)) {
                    foundNumberOfRoundsBack++;
                    currentLastUpdatedDate = gameLastUpdatedDate;
                } else {
                    currentLastUpdatedDate = gameLastUpdatedDate;
                }
                if (foundNumberOfRoundsBack < maxNumberOfRoundsBack) {
                    gamesToLookAt.add(game);
                }
            } else {
                break;
            }
        }

        games = gamesToLookAt;

        // Walk oldest-to-newest, doubling points_at_stake as the round
        // index advances. Exponentially weights freshness so recent-pair
        // repeats are punished hardest. In-memory mutation only — never
        // persisted (the resource is @Transactional(NEVER)).
        Collections.reverse(games);

        int currentValueForGame = 1;
        String currentLastUpdated = "";

        for (Game game : games) {
            String gameLastUpdated = wireTimestampString(game.timestamp);

            if (!currentLastUpdated.equals(gameLastUpdated)) {
                if (!currentLastUpdated.equals("")) {
                    currentValueForGame = currentValueForGame * 2;
                }
                currentLastUpdated = gameLastUpdated;
            }
            // In-memory mutation only — the resource is @Transactional(NEVER)
            // so Hibernate cannot flush these back.
            game.pointsAtStake = currentValueForGame;
        }

        // Then create array for all possible players
        Map<String, Integer> currentPlayerScore = new HashMap<>();
        for (String playerName : namesOfAvailablePlayers) {
            currentPlayerScore.put(playerName, 0);
        }

        // And create array for all pairs
        Map<String, Integer> currentBuddyScore = new HashMap<>();
        for (String playerName1 : namesOfAvailablePlayers) {
            for (String playerName2 : namesOfAvailablePlayers) {
                currentBuddyScore.put(playerName1 + "<->" + playerName2, 0);
            }
        }

        for (Game game : games) {
            String pair1Player1 = game.playerRed1;
            String pair1Player2 = game.playerRed2;
            String pair2Player1 = game.playerBlue1;
            String pair2Player2 = game.playerBlue2;

            int currentPointsScore = game.pointsAtStake;

            if (namesOfAvailablePlayers.contains(pair1Player1) && !"null".equals(pair1Player1)) {
                Integer currentNumber = currentPlayerScore.get(pair1Player1);
                if (currentNumber == null) currentNumber = 0;
                int nextNumber = currentNumber + currentPointsScore;
                currentPlayerScore.put(pair1Player1, nextNumber);
            }

            if (namesOfAvailablePlayers.contains(pair1Player2) && !"null".equals(pair1Player2)) {
                Integer currentNumber = currentPlayerScore.get(pair1Player2);
                if (currentNumber == null) currentNumber = 0;
                int nextNumber = currentNumber + currentPointsScore;
                currentPlayerScore.put(pair1Player2, nextNumber);
            }

            if (namesOfAvailablePlayers.contains(pair2Player1) && !"null".equals(pair2Player1)) {
                Integer currentNumber = currentPlayerScore.get(pair2Player1);
                if (currentNumber == null) currentNumber = 0;
                int nextNumber = currentNumber + currentPointsScore;
                currentPlayerScore.put(pair2Player1, nextNumber);
            }

            if (namesOfAvailablePlayers.contains(pair2Player2) && !"null".equals(pair2Player2)) {
                Integer currentNumber = currentPlayerScore.get(pair2Player2);
                if (currentNumber == null) currentNumber = 0;
                int nextNumber = currentNumber + currentPointsScore;
                currentPlayerScore.put(pair2Player2, nextNumber);
            }

            // Pair 1
            boolean pair1ShallBeSaved = true;
            if (namesOfAvailablePlayers.contains(pair1Player1)) {
                String pair1String = "";
                if (!"null".equals(pair1Player2)) {
                    if (!namesOfAvailablePlayers.contains(pair1Player2)) {
                        pair1ShallBeSaved = false;
                    } else {
                        pair1String = pair1Player1 + "<->" + pair1Player2;
                    }
                } else {
                    pair1String = pair1Player1 + "<->" + pair1Player1;
                }
                if (pair1ShallBeSaved) {
                    Integer currentNumberPair1 = currentBuddyScore.get(pair1String);
                    if (currentNumberPair1 == null) currentNumberPair1 = 0;
                    int nextNumberPair1 = currentNumberPair1 + currentPointsScore;
                    currentBuddyScore.put(pair1String, nextNumberPair1);
                }
            }

            // Pair 2
            boolean pair2ShallBeSaved = true;
            if (namesOfAvailablePlayers.contains(pair2Player1)) {
                String pair2String = "";
                if (!"null".equals(pair2Player2)) {
                    if (!namesOfAvailablePlayers.contains(pair2Player2)) {
                        pair2ShallBeSaved = false;
                    } else {
                        pair2String = pair2Player1 + "<->" + pair2Player2;
                    }
                } else {
                    pair2String = pair2Player1 + "<->" + pair2Player1;
                }
                if (pair2ShallBeSaved) {
                    Integer currentNumberPair2 = currentBuddyScore.get(pair2String);
                    if (currentNumberPair2 == null) currentNumberPair2 = 0;
                    int nextNumberPair2 = currentNumberPair2 + currentPointsScore;
                    currentBuddyScore.put(pair2String, nextNumberPair2);
                }
            }
        }

        currentPlayerScore = sortByValue(currentPlayerScore);

        // Take players directly into the pool while they all fit
        LinkedList<String> certainPlayers = new LinkedList<>();
        LinkedList<String> possiblePlayers = new LinkedList<>();
        Integer previousNumber = -1;
        int currentCertainPlayers = 0;

        for (Map.Entry<String, Integer> entry : currentPlayerScore.entrySet()) {
            Integer val = entry.getValue();
            String key = entry.getKey();
            if (currentCertainPlayers < maxPlayersNeeded) {
                // If this value differs from the previous, we know that
                // some players in possiblePlayers can be promoted directly
                // (we still need players).
                if (val > previousNumber) {
                    if (possiblePlayers.size() + certainPlayers.size() <= maxPlayersNeeded) {
                        // OK to promote them all directly
                        certainPlayers.addAll(possiblePlayers);
                        currentCertainPlayers = certainPlayers.size();

                        possiblePlayers = new LinkedList<>();
                        previousNumber = val;
                        possiblePlayers.add(key);
                    } else {
                        // We have more in possiblePlayers than fit; pick
                        // a random subset to promote.
                        Collections.shuffle(possiblePlayers);
                        LinkedList<String> randomPlayers = new LinkedList<>(
                                possiblePlayers.subList(0, maxPlayersNeeded - currentCertainPlayers));
                        certainPlayers.addAll(randomPlayers);
                        currentCertainPlayers = certainPlayers.size();
                    }
                } else {
                    // Same number as before
                    possiblePlayers.add(key);
                }
            }
        }

        // Final sweep: drain anything still in possiblePlayers
        if (possiblePlayers.size() + certainPlayers.size() <= maxPlayersNeeded) {
            certainPlayers.addAll(possiblePlayers);
            currentCertainPlayers = certainPlayers.size();
        } else {
            Collections.shuffle(possiblePlayers);
            LinkedList<String> randomPlayers = new LinkedList<>(
                    possiblePlayers.subList(0, maxPlayersNeeded - currentCertainPlayers));
            certainPlayers.addAll(randomPlayers);
            currentCertainPlayers = certainPlayers.size();
        }

        // Now make some great pairs — try a handful of shuffles, pick the
        // one with the lowest summed pair-history weight.
        int triesSoFar;
        int bestPairsFoundInRound = -1;
        int previousUniquenessScore = Integer.MAX_VALUE;
        LinkedList<Pair> bestFoundPairs = null;

        for (triesSoFar = 0; triesSoFar < CHOSEN_NUMBER_OF_TRIES; triesSoFar++) {
            if (previousUniquenessScore == 0) break;
            Collections.shuffle(certainPlayers);

            int numberOfPlayers = certainPlayers.size();
            int playersForLastTable = numberOfPlayers % 4;
            int index = 0;
            Pair currentPair = null;
            LinkedList<Pair> allPairs = new LinkedList<>();
            for (String player : certainPlayers) {
                if (index % 2 == 0) {
                    // Then we have first player in Pair
                    currentPair = new Pair();
                    currentPair.player1 = player;

                    // If the last table has 3 players, just add to allPairs
                    // now (we will not get to the else below ever).
                    if ((index + 1) == numberOfPlayers) {
                        currentPair.player2 = null;
                        allPairs.add(currentPair);
                    }
                } else {
                    if (index + 1 == numberOfPlayers && playersForLastTable == 2) {
                        // Then we are at the last person at the last game
                        // with two players.
                        currentPair.player2 = null;
                        allPairs.add(currentPair);
                        currentPair = new Pair();
                        currentPair.player1 = player;
                        currentPair.player2 = null;
                        allPairs.add(currentPair);
                    } else {
                        currentPair.player2 = player;
                        allPairs.add(currentPair);
                    }
                }
                index++;
            }

            // Compute uniqueness score
            int uniquenessScore = 0;
            for (Pair par : allPairs) {
                String comb1;
                String comb2;
                if (par.player2 != null && !"null".equals(par.player2)) {
                    comb1 = par.player1 + "<->" + par.player2;
                    comb2 = par.player2 + "<->" + par.player1;
                } else {
                    comb1 = par.player1 + "<->" + par.player1;
                    // On purpose — solo matches count double, to discourage
                    // benching the same player.
                    comb2 = par.player1 + "<->" + par.player1;
                }

                Integer currentScoreComb1 = currentBuddyScore.get(comb1);
                if (currentScoreComb1 == null) currentScoreComb1 = 0;
                uniquenessScore = uniquenessScore + currentScoreComb1;

                Integer currentScoreComb2 = currentBuddyScore.get(comb2);
                if (currentScoreComb2 == null) currentScoreComb2 = 0;
                uniquenessScore = uniquenessScore + currentScoreComb2;
            }

            if (uniquenessScore < previousUniquenessScore) {
                previousUniquenessScore = uniquenessScore;
                bestFoundPairs = allPairs;
                bestPairsFoundInRound = triesSoFar;
            }
        }

        LinkedList<String> awesomePlayerList = new LinkedList<>();
        if (bestFoundPairs != null) {
            for (Pair par : bestFoundPairs) {
                awesomePlayerList.add(par.player1);
                awesomePlayerList.add(par.player2);
            }
        }
        return awesomePlayerList;
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    /**
     * Map of player name → most-recent-game epoch-millis. Used by the
     * last-first algorithm to bias selection toward least-recently-played.
     */
    private Map<String, Long> playersLastPlayed() {
        Map<String, Long> result = new HashMap<>();
        List<Game> all = Game.<Game>find("deletedAt IS NULL").list();
        for (Game g : all) {
            consider(result, g.playerRed1, g.timestamp);
            consider(result, g.playerRed2, g.timestamp);
            consider(result, g.playerBlue1, g.timestamp);
            consider(result, g.playerBlue2, g.timestamp);
        }
        return result;
    }

    private static void consider(Map<String, Long> acc, String name, LocalDateTime ts) {
        if (name == null || "null".equals(name) || ts == null) return;
        long ms = toEpochMillis(ts);
        Long existing = acc.get(name);
        if (existing == null || ms > existing) {
            acc.put(name, ms);
        }
    }

    /**
     * Table-saturation rule. Cap players at the number of full tables that
     * fit (4 per table) but include everyone when the available count makes
     * one extra table of 3 or 2 viable.
     */
    static int adjustedMaxPlayersNeeded(int availablePlayersCount, int numberOfGames) {
        int maxPlayersNeeded = 4 * numberOfGames;
        if (availablePlayersCount >= 0 && availablePlayersCount <= 3) {
            maxPlayersNeeded = availablePlayersCount;
        } else if (availablePlayersCount <= 5) {
            // If only players for 1 table
            if (maxPlayersNeeded > 4) maxPlayersNeeded = 4;
        } else if (availablePlayersCount > 5 && availablePlayersCount <= 7) {
            maxPlayersNeeded = availablePlayersCount;
        } else if (availablePlayersCount <= 9) {
            // If only players for 2 tables
            if (maxPlayersNeeded > 8) maxPlayersNeeded = 8;
        } else if (availablePlayersCount > 9 && availablePlayersCount <= 11) {
            maxPlayersNeeded = availablePlayersCount;
        } else if (availablePlayersCount <= 13) {
            // If only players for 3 tables
            if (maxPlayersNeeded > 12) maxPlayersNeeded = 12;
        }
        return maxPlayersNeeded;
    }

    private static LinkedList<String> playerNames(List<PlayerDto> players) {
        LinkedList<String> names = new LinkedList<>();
        for (PlayerDto p : players) {
            names.add(p.name());
        }
        return names;
    }

    /** {@link LocalDateTime} → epoch-millis via the JVM default zone. */
    private static long toEpochMillis(LocalDateTime ts) {
        return java.sql.Timestamp.valueOf(ts).getTime();
    }

    /**
     * {@link LocalDateTime} → wire-format string. Used as a string-equality
     * bucket key when grouping games into "rounds" (same-string == same round).
     */
    private static String wireTimestampString(LocalDateTime ts) {
        return java.sql.Timestamp.valueOf(ts).toString();
    }

    /** Treat a missing last-played as 0L ("never played, sort first"). */
    private static long nullSafe(Long v) {
        return v == null ? 0L : v;
    }

    /** Sort a {@code Map} by value ascending, returning a {@link LinkedHashMap}. */
    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Comparator.comparing(Map.Entry::getValue));
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
