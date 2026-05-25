package com.foosball.api;

import com.foosball.domain.Game;
import com.foosball.domain.Period;
import com.foosball.domain.Player;
import com.foosball.dto.GameDto;
import com.foosball.dto.GameMapper;
import com.foosball.dto.GamesPostResponseDto;
import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Games endpoints.
 *
 * <ul>
 *   <li>{@code GET /games} — all live games, newest first.</li>
 *   <li>{@code GET /games/{token}} — token in {@code hour|day|week|month|alltime}
 *       filters by time window; otherwise treated as a player name and
 *       returns up to 10 matching games.</li>
 *   <li>{@code POST /games} — body is a JSON array of {@link GameDto}.</li>
 *   <li>{@code PUT /games/{id}} — full replace of an existing game except
 *       its {@code id} and {@code timestamp}. 404 if the game is missing
 *       or already soft-deleted.</li>
 *   <li>{@code DELETE /games/{id}} — soft-delete a single game.</li>
 *   <li>{@code DELETE /games} — soft-delete every live game.</li>
 * </ul>
 *
 * <p>Soft-deleted rows are hidden from every read in this resource and
 * from leaderboard / stats / tournament-pairing computations elsewhere.
 */
@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GamesResource {

    @GET
    public List<GameDto> listAll() {
        Set<String> active = activeNames();
        return Game.<Game>find("deletedAt IS NULL", Sort.by("id").descending())
                .list()
                .stream()
                .map(g -> GameMapper.toDto(g, active))
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

    @PUT
    @Path("/{id}")
    @Transactional
    public GameDto update(@PathParam("id") Long id, GameDto dto) {
        Game existing = Game.<Game>find("id = ?1 AND deletedAt IS NULL", id)
                .firstResult();
        if (existing == null) {
            throw new NotFoundException();
        }
        GameMapper.applyEditable(dto, existing);
        return GameMapper.toDto(existing);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteOne(@PathParam("id") Long id) {
        long updated = Game.update(
                "deletedAt = ?1 WHERE id = ?2 AND deletedAt IS NULL",
                LocalDateTime.now(), id);
        if (updated == 0) {
            throw new NotFoundException();
        }
        return Response.ok("deleteGame: " + id).type(MediaType.TEXT_PLAIN).build();
    }

    @DELETE
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteAll() {
        long count = Game.update(
                "deletedAt = ?1 WHERE deletedAt IS NULL",
                LocalDateTime.now());
        return Response.ok("cleanGameTable: " + count).type(MediaType.TEXT_PLAIN).build();
    }

    private List<GameDto> findByPeriod(Period p) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(p.hoursBack);
        Set<String> active = activeNames();
        return Game.<Game>find(
                        "timestamp > ?1 AND deletedAt IS NULL ORDER BY id DESC",
                        cutoff)
                .list()
                .stream()
                .map(g -> GameMapper.toDto(g, active))
                .toList();
    }

    private List<GameDto> findByPlayerName(String name) {
        Set<String> active = activeNames();
        return Game.<Game>find(
                        "(playerRed1 = ?1 OR playerRed2 = ?1 "
                                + "OR playerBlue1 = ?1 OR playerBlue2 = ?1) "
                                + "AND deletedAt IS NULL "
                                + "ORDER BY id DESC",
                        name)
                .page(0, 10)
                .list()
                .stream()
                .map(g -> GameMapper.toDto(g, active))
                .toList();
    }

    private static Set<String> activeNames() {
        return Player.<Player>listAll().stream()
                .map(p -> p.name)
                .collect(Collectors.toSet());
    }
}
