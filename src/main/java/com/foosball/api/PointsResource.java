package com.foosball.api;

import com.foosball.domain.Game;
import com.foosball.domain.Period;
import com.foosball.domain.Player;
import com.foosball.dto.PointsPlayerDto;
import com.foosball.service.EloService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code GET /pointsPrPlayer/{period}} — leaderboard for {@code hour|day|
 * week|month|alltime}. Returns a JSON array of {@code {position, points,
 * numberOfGames, name}} sorted by {@code points DESC}; ties produce
 * repeated position values.
 *
 * <p>Entries whose name no longer exists in {@code tbl_players} (the
 * player has been deleted but their games remain) are dropped from the
 * response entirely. Existing players' points and positions are
 * unchanged — their wins/losses against the deleted player still count
 * in the underlying Elo math.
 */
@Path("/pointsPrPlayer")
@Produces(MediaType.APPLICATION_JSON)
public class PointsResource {

    @Inject
    EloService eloService;

    @GET
    @Path("/{period}")
    public List<PointsPlayerDto> getByPeriod(@PathParam("period") String token) {
        Period p = Period.fromToken(token).orElseThrow(NotFoundException::new);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(p.hoursBack);
        List<Game> games = Game.<Game>find(
                "timestamp > ?1 AND deletedAt IS NULL ORDER BY id ASC",
                cutoff).list();

        List<PointsPlayerDto> ranked = (p == Period.ALLTIME)
                ? eloService.rankWithEloStart(games)
                : eloService.rankWithDefaultStart(games);

        Set<String> activeNames = Player.<Player>listAll().stream()
                .map(pl -> pl.name)
                .collect(Collectors.toSet());

        List<PointsPlayerDto> filtered = ranked.stream()
                .filter(r -> activeNames.contains(r.name()))
                .toList();

        // Renumber positions starting at 1 after filtering; ties (same points
        // in adjacent rows) keep the same position.
        List<PointsPlayerDto> result = new ArrayList<>(filtered.size());
        int currentPos = 0;
        int currentPoints = Integer.MIN_VALUE;
        for (int i = 0; i < filtered.size(); i++) {
            PointsPlayerDto r = filtered.get(i);
            if (r.points() != currentPoints) {
                currentPos = i + 1;
                currentPoints = r.points();
            }
            result.add(new PointsPlayerDto(
                    currentPos, r.points(), r.numberOfGames(), r.name()));
        }
        return result;
    }
}
