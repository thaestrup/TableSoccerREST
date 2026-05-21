package com.foosball.api;

import com.foosball.domain.Game;
import com.foosball.domain.Period;
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
import java.util.List;
import java.util.Optional;

/**
 * Points-per-Player resource — port of legacy {@code PointsPrPlayer.groovy}.
 *
 * <p>Wire contract preserved: {@code GET /pointsPrPlayer/{period}} returns a
 * JSON array of {@code {position, points, numberOfGames, name}} objects,
 * sorted by {@code points DESC}, with tied scores producing repeated
 * {@code position} values (e.g. {@code 1, 1, 3, 3}).
 *
 * <p>Period dispatch matches the legacy switch:
 * <ul>
 *   <li>{@code alltime} — applies the {@code newElo} branch (1500 starting
 *       score, ± {@code points_at_stake} per game).</li>
 *   <li>{@code month}/{@code week}/{@code day}/{@code hour} — fall through
 *       the legacy switch with {@code filter == ""} and reach the
 *       {@code else} branch (same delta math, 0 starting score).</li>
 * </ul>
 *
 * <p>Dropped vs. legacy: {@code POST /pointsPrPlayer/...},
 * {@code GET /pointsPrPlayer/} (no period), and the filter tokens
 * {@code alltime-onlylunch}, {@code alltime-ratiofocus},
 * {@code alltime-elo} — frontend never calls them
 * (see FRONTEND-USAGE.md).
 *
 * <p>Unlike the legacy service the games are read directly from the
 * database; the legacy {@code POST} branch fetched
 * {@code http://localhost:5050/games} over HTTP from itself, which the port
 * skips.
 */
@Path("/pointsPrPlayer")
@Produces(MediaType.APPLICATION_JSON)
public class PointsResource {

    @Inject
    EloService eloService;

    @GET
    @Path("/{period}")
    public List<PointsPlayerDto> getByPeriod(@PathParam("period") String token) {
        // Return type is List<PointsPlayerDto> rather than Response so
        // Quarkus's native-image static analysis can see the payload type
        // and auto-register PointsPlayerDto for reflection. Wrapping in
        // Response hides the generic and produces a runtime
        // "no serializer found" error on the native binary.
        Period p = Period.fromToken(token).orElseThrow(NotFoundException::new);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(p.hoursBack);
        // Order by id ASC to mirror the legacy MoreUtil "elo" branch's
        // chronological ordering. The newElo math is order-independent in
        // aggregate (each game adds/subtracts a constant), but iterating in
        // insertion order keeps log output stable and matches the legacy
        // code path most closely.
        List<Game> games = Game.<Game>find("timestamp > ?1 ORDER BY id ASC", cutoff).list();

        return (p == Period.ALLTIME)
                ? eloService.rankWithEloStart(games)
                : eloService.rankWithDefaultStart(games);
    }
}
