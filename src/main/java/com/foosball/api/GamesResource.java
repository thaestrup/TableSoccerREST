package com.foosball.api;

import com.foosball.domain.Game;
import com.foosball.domain.Period;
import com.foosball.dto.GameDto;
import com.foosball.dto.GameMapper;
import com.foosball.dto.GamesPostResponseDto;
import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Games resource — port of legacy {@code Games.groovy}.
 *
 * <p>Wire contract preserved:
 * <ul>
 *   <li>{@code GET /games} — all games, ordered {@code id DESC}.</li>
 *   <li>{@code GET /games/{token}} — overloaded: token in
 *       {@code hour|day|week|month|alltime} → time-window filter; otherwise
 *       treat as a player name → matches any of the four player slots,
 *       capped at 10 rows. Both branches are exercised by the React
 *       frontend (see FRONTEND-USAGE.md).</li>
 *   <li>{@code POST /games} — body is a JSON array of {@link GameDto};
 *       returns {@link GamesPostResponseDto} with stringified ids.</li>
 *   <li>{@code DELETE /games} — wipes the table; returns plain text.</li>
 * </ul>
 *
 * <p>Dropped vs. legacy: {@code PUT /games}, {@code PUT /games/{id}},
 * {@code DELETE /games/{id}} — frontend never calls them
 * (see FRONTEND-USAGE.md).
 */
@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GamesResource {

    @GET
    public List<GameDto> listAll() {
        return Game.<Game>listAll(Sort.by("id").descending())
                .stream()
                .map(GameMapper::toDto)
                .toList();
    }

    @GET
    @Path("/{token}")
    public List<GameDto> getByToken(@PathParam("token") String token) {
        Optional<Period> period = Period.fromToken(token);
        if (period.isPresent()) {
            return findByPeriod(period.get());
        }
        return findByPlayerName(token);
    }

    @POST
    @Transactional
    public GamesPostResponseDto insertAll(List<GameDto> incoming) {
        List<String> ids = new ArrayList<>(incoming.size());
        for (GameDto dto : incoming) {
            Game g = GameMapper.toEntity(dto);
            g.persist();
            ids.add(String.valueOf(g.id));
        }
        return new GamesPostResponseDto(ids);
    }

    @DELETE
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteAll() {
        long count = Game.deleteAll();
        return Response.ok("cleanGameTable: " + count).type(MediaType.TEXT_PLAIN).build();
    }

    private List<GameDto> findByPeriod(Period p) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(p.hoursBack);
        return Game.<Game>find("timestamp > ?1 ORDER BY id DESC", cutoff)
                .list()
                .stream()
                .map(GameMapper::toDto)
                .toList();
    }

    private List<GameDto> findByPlayerName(String name) {
        return Game.<Game>find(
                        "(playerRed1 = ?1 OR playerRed2 = ?1 "
                                + "OR playerBlue1 = ?1 OR playerBlue2 = ?1) "
                                + "ORDER BY id DESC",
                        name)
                .page(0, 10)
                .list()
                .stream()
                .map(GameMapper::toDto)
                .toList();
    }
}
