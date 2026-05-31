package com.foosball.api;

import io.quarkus.hibernate.orm.panache.Panache;
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
 * {@code GET /statisticsPlayersLastPlayed} — returns a JSON object keyed
 * by player name, with values in epoch-millis. Players with no games
 * are omitted; the {@code "null"}-string sentinel in empty back-row
 * slots is filtered out.
 */
@Path("/statisticsPlayersLastPlayed")
@Produces(MediaType.APPLICATION_JSON)
public class StatisticsResource {

    private static final String QUERY = """
            SELECT p.name, MAX(slots.ts) AS last_played
              FROM tbl_players p
              JOIN (
                       SELECT player_red_1  AS name, `timestamp` AS ts FROM tbl_fights WHERE deleted_at IS NULL
             UNION ALL SELECT player_red_2,  `timestamp`               FROM tbl_fights WHERE deleted_at IS NULL
             UNION ALL SELECT player_blue_1, `timestamp`               FROM tbl_fights WHERE deleted_at IS NULL
             UNION ALL SELECT player_blue_2, `timestamp`               FROM tbl_fights WHERE deleted_at IS NULL
                   ) AS slots ON slots.name = p.name
             WHERE slots.name <> 'null'
             GROUP BY p.name
            """;

    @GET
    public Map<String, Long> playersLastPlayed() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = (List<Object[]>) Panache.getEntityManager()
                .createNativeQuery(QUERY)
                .getResultList();

        Map<String, Long> result = new LinkedHashMap<>(rows.size());
        for (Object[] row : rows) {
            String name = (String) row[0];
            LocalDateTime ts = (LocalDateTime) row[1];
            result.put(name, Timestamp.valueOf(ts).getTime());
        }
        return result;
    }
}
