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
 * Tournament pairing logic — port of legacy
 * {@code RandomTournament.groovy}, {@code LastFirstTournament.groovy},
 * and {@code AwesomeAlgorithmTournament.groovy} (whose pairing brain is
 * {@code AdditionalUtil.generateAwesomeList}).
 *
 * <p>The three algorithms are kept distinct on purpose; they have
 * subtly different shapes (random returns flat, the others wrap in
 * rounds) and different selection logic. The "awesome" port is a
 * faithful, mechanical translation of the legacy Java in
 * {@code AdditionalUtil.java} — no behavior changes, no refactor.
 * Phase 6 of the migration plan is the place to clean that up.
 */
@ApplicationScoped
public class TournamentPairingService {

    /**
     * Legacy {@code MoreUtil.getGamesForThisManyHoursBackInTime("1", "")}
     * window: the awesome algorithm only weighs games from the last hour
     * when scoring pairing freshness.
     */
    private static final int AWESOME_HOURS_BACK = 1;

    /**
     * Legacy threshold for "a meaningful break": if two consecutive games
     * are more than 30 minutes apart, the older one (and everything before
     * it) is excluded from the freshness-weighting window.
     */
    private static final int THRESHOLD_IN_MINUTES = 30;

    /** Legacy cap on how many rounds back the awesome algorithm walks. */
    private static final int MAX_NUMBER_OF_ROUNDS_BACK = 25;

    /** Legacy retry count for the pair-shuffle uniqueness search. */
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
            // Legacy ordering: red_1, blue_1, red_2, blue_2 — a quirk worth
            // preserving so output diffs against the legacy backend stay clean.
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
     * Port of {@code LastFirstTournament.generateGames}. Sorts players
     * by their last-played timestamp (so least-recently-played go first),
     * trims to the count we need based on the table-saturation rules,
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
                // Legacy quirk: skip games where blue has no first player —
                // means we ran out before completing a 2-on-2.
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
     * Port of {@code AwesomeAlgorithmTournament.generateGames} +
     * {@code AdditionalUtil.generateAwesomeList}. Faithful mechanical
     * translation — do not refactor, intentional behaviors are preserved
     * including: the doubling points-at-stake hack, the shuffle/retry
     * uniqueness search ({@value #CHOSEN_NUMBER_OF_TRIES} attempts), and
     * the "matches alone count double" buddy-score rule.
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

        // NOTE the slot order vs. random/last-first: the awesome
        // algorithm fills red_1, red_2, blue_1, blue_2 — different from
        // random's red_1, blue_1, red_2, blue_2. Preserve.
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
    // generateAwesomeList — faithful port of legacy AdditionalUtil
    // -------------------------------------------------------------------

    /**
     * Inner-class mirror of legacy {@code AdditionalUtil.Pair}.
     * Mutable, kept package-private to mirror legacy semantics.
     */
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
     * Faithful port of {@code AdditionalUtil.generateAwesomeList}.
     * Don't refactor — Phase 6 of the migration plan tackles this.
     */
    private LinkedList<String> generateAwesomeList(
            int maxPlayersNeeded, LinkedList<String> namesOfAvailablePlayers) {
        // Get all matches 1 hour back. Legacy used MoreUtil.getGamesForThisManyHoursBackInTime("1", "").
        List<Game> games = Game.<Game>find(
                "timestamp > ?1 ORDER BY id DESC",
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

            // Compute milliseconds between the previous game and this one,
            // mirroring legacy's Date.getTime() arithmetic.
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
        // index advances. The legacy comment calls this "a hack" — it
        // weights freshness exponentially so we punish recent-pair repeats
        // hardest. We mutate the entity in-memory only; nothing is persisted.
        Collections.reverse(games);

        int currentValueForGame = 1;
        String currentLastUpdated = "";

        for (Game game : games) {
            String gameLastUpdated = legacyTimestampString(game.timestamp);

            if (!currentLastUpdated.equals(gameLastUpdated)) {
                if (!currentLastUpdated.equals("")) {
                    currentValueForGame = currentValueForGame * 2;
                }
                currentLastUpdated = gameLastUpdated;
            }
            // Mutates the loaded entity in memory only — we are not in a
            // transaction and Panache won't flush this back. Mirrors legacy
            // setPoints_at_stake(...) which was likewise transient.
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
            // (Legacy unused: boolean unevenNumberOfPlayers — left out
            // since it has no observable effect.)

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
     * Port of legacy {@code MoreUtil.playersLastPlayed} but used here
     * <em>only</em> by the last-first algorithm (the
     * {@code /statisticsPlayersLastPlayed} endpoint has its own
     * resource that keeps the same shape on the wire).
     */
    private Map<String, Long> playersLastPlayed() {
        Map<String, Long> result = new HashMap<>();
        // Single-pass aggregation — collapses the legacy N+1.
        List<Game> all = Game.<Game>listAll();
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
     * Legacy table-saturation rules: cap players at the number of full
     * tables that fit (one table = 4 players), but always include
     * everyone when the count is small enough to make one extra table
     * with three or two. Copied verbatim from
     * {@code AwesomeAlgorithmTournament.generateGames}.
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

    /** Mirrors legacy {@code Timestamp.getTime()} conversion via the JVM default zone. */
    private static long toEpochMillis(LocalDateTime ts) {
        return java.sql.Timestamp.valueOf(ts).getTime();
    }

    /**
     * Legacy {@code Timestamp.toString()} formatting — used by the awesome
     * algorithm purely as a string-equality bucket key when grouping games
     * into "rounds" (same-string == same round).
     */
    private static String legacyTimestampString(LocalDateTime ts) {
        return java.sql.Timestamp.valueOf(ts).toString();
    }

    /** Treat a missing last-played as 0L (effectively "never played, sort first"). */
    private static long nullSafe(Long v) {
        return v == null ? 0L : v;
    }

    /** Standard "sort a Map by value ascending into a LinkedHashMap" helper from legacy. */
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
