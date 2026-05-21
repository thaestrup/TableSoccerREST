package com.foosball.api;

import com.foosball.domain.Game;
import com.foosball.domain.Player;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistics resource — port of legacy {@code StatisticsPlayersLastPlayed.groovy}
 * (delegating to {@code MoreUtil.playersLastPlayed()}).
 *
 * <p>Wire contract preserved: returns a JSON <strong>object</strong>
 * (NOT array) keyed by player name, with epoch-millis longs as values.
 * The React frontend uses this to render an "active in last 30 days"
 * indicator, so the shape (object, not array) and the unit (epoch ms,
 * matching legacy {@link Timestamp#getTime()}) must not drift.
 *
 * <p>Performance note: the legacy implementation does N+1 queries
 * (1 to fetch all players, then 1 per player to find their newest
 * match). This port collapses it to two queries — one to load player
 * names, then one aggregate over {@code tbl_fights} that yields the
 * newest timestamp seen across any of the four player-slot columns.
 * In-memory we then resolve each player's max from the aggregate.
 */
@Path("/statisticsPlayersLastPlayed")
@Produces(MediaType.APPLICATION_JSON)
public class StatisticsResource {

    @GET
    public Map<String, Long> playersLastPlayed() {
        List<Player> players = Player.<Player>listAll();

        // One round-trip to gather (name, max(timestamp)) across the 4 slot
        // columns. JPQL doesn't support unioning across columns cleanly, so
        // we stream all games once and reduce in memory — still O(N) over
        // tbl_fights but exactly one query, vs. the legacy N+1.
        Map<String, LocalDateTime> newestByPlayer = new java.util.HashMap<>();
        List<Game> allGames = Game.<Game>listAll();
        for (Game g : allGames) {
            track(newestByPlayer, g.playerRed1, g.timestamp);
            track(newestByPlayer, g.playerRed2, g.timestamp);
            track(newestByPlayer, g.playerBlue1, g.timestamp);
            track(newestByPlayer, g.playerBlue2, g.timestamp);
        }

        // Preserve insertion order from the players table (legacy iterated
        // allPlayers and put into a HashMap, so its order was effectively
        // unspecified — we use LinkedHashMap purely for predictable test
        // output; the contract test only checks values, not order).
        Map<String, Long> result = new LinkedHashMap<>();
        for (Player p : players) {
            LocalDateTime newest = newestByPlayer.get(p.name);
            if (newest != null) {
                // Legacy used Timestamp.getTime() directly, which honors the
                // JVM's default time zone when converting from a SQL
                // TIMESTAMP literal. Mirror that here.
                result.put(p.name, Timestamp.valueOf(newest).getTime());
            }
        }
        return result;
    }

    private static void track(Map<String, LocalDateTime> acc, String name, LocalDateTime ts) {
        if (name == null || name.equals("null") || ts == null) {
            return;
        }
        LocalDateTime existing = acc.get(name);
        if (existing == null || ts.isAfter(existing)) {
            acc.put(name, ts);
        }
    }
}
