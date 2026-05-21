package com.foosball.api;

import com.foosball.domain.Player;
import com.foosball.dto.PlayerDto;
import com.foosball.dto.PlayerMapper;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Players resource — port of legacy {@code Players.groovy}.
 *
 * <p>Wire contract preserved: {@code POST /players} accepts a JSON
 * <em>array</em> body (single-item arrays are valid — that's what the
 * React frontend sends), and {@code PUT /players/{name}} accepts a single
 * Player object. Plain-text responses for writes match legacy strings like
 * {@code "insertPlayer: Foo, result: 88"} so frontend toasts don't change.
 *
 * <p>Dropped vs. legacy: {@code DELETE /players}, {@code DELETE /players/{name}},
 * {@code PUT /players} (bulk) — frontend never calls them
 * (see FRONTEND-USAGE.md).
 */
@Path("/players")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlayersResource {

    @GET
    public List<PlayerDto> listAll() {
        return Player.<Player>listAll()
                .stream()
                .map(PlayerMapper::toDto)
                .toList();
    }

    @GET
    @Path("/{name}")
    public PlayerDto getByName(@PathParam("name") String name) {
        Player p = Player.find("name", name).firstResult();
        if (p == null) {
            throw new NotFoundException();
        }
        return PlayerMapper.toDto(p);
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertAll(List<PlayerDto> incoming) {
        String body = incoming.stream()
                .map(this::insertOne)
                .collect(Collectors.joining(System.lineSeparator()));
        return Response.ok(body).type(MediaType.TEXT_PLAIN).build();
    }

    @PUT
    @Path("/{name}")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response upsert(@PathParam("name") String name, PlayerDto dto) {
        Player p = Player.find("name", name).firstResult();
        boolean isNew = (p == null);
        if (isNew) {
            p = new Player();
        }
        PlayerMapper.apply(dto, p, name);
        if (isNew) {
            p.persist();
        }
        String result = "overwritePlayer: " + name + ", result: " + (isNew ? p.id : 0);
        return Response.ok(result).type(MediaType.TEXT_PLAIN).build();
    }

    private String insertOne(PlayerDto dto) {
        Player existing = Player.find("name", dto.name()).firstResult();
        if (existing != null) {
            PlayerMapper.apply(dto, existing, dto.name());
            return "insertPlayer: " + dto.name() + ", result: " + existing.id;
        }
        Player p = new Player();
        PlayerMapper.apply(dto, p, dto.name());
        p.persist();
        return "insertPlayer: " + dto.name() + ", result: " + p.id;
    }
}
