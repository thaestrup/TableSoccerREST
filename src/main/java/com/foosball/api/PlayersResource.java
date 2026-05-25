package com.foosball.api;

import com.foosball.domain.Game;
import com.foosball.domain.Player;
import com.foosball.domain.PlayerPhoto;
import com.foosball.dto.PlayerDto;
import com.foosball.dto.PlayerMapper;
import com.foosball.dto.RenameRequest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Players endpoints.
 *
 * <ul>
 *   <li>{@code GET /players}, {@code GET /players/{name}}, {@code POST /players}
 *       (array body), {@code PUT /players/{name}} (upsert).</li>
 *   <li>{@code PUT /players/{name}/rename} — rename a player. New name in the
 *       JSON body. All games in {@code tbl_fights} that reference the old
 *       name (any of the four slot columns) are updated in the same
 *       transaction.</li>
 *   <li>{@code GET /players/{name}/photo} — returns the player's photo bytes
 *       with the original {@code Content-Type}. 404 if no photo set.</li>
 *   <li>{@code PUT /players/{name}/photo} — uploads a photo. Body is the raw
 *       image bytes; {@code Content-Type} must be {@code image/jpeg},
 *       {@code image/png}, or {@code image/webp}. Max 2 MB.</li>
 *   <li>{@code DELETE /players/{name}/photo} — clears the photo.</li>
 *   <li>{@code DELETE /players/{name}} — removes the player row. Game rows in
 *       {@code tbl_fights} that reference the name are unaffected; the
 *       deleted player still appears in game-history reads under the
 *       label {@code "Deleted player"} but is dropped from
 *       {@code /pointsPrPlayer}.</li>
 * </ul>
 *
 * <p>Write responses return {@code text/plain} bodies that the React
 * frontend uses as toast messages.
 */
@Path("/players")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlayersResource {

    static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;
    static final Set<String> ALLOWED_PHOTO_MIMES =
            Set.of("image/jpeg", "image/png", "image/webp");

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

    @PUT
    @Path("/{name}/rename")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response rename(@PathParam("name") String oldName, RenameRequest req) {
        if (req == null || req.newName() == null || req.newName().isBlank()) {
            throw new BadRequestException("newName required");
        }
        String newName = req.newName().trim();
        if (newName.equals(oldName)) {
            return Response.ok("rename: " + oldName + " unchanged").type(MediaType.TEXT_PLAIN).build();
        }
        Player player = Player.find("name", oldName).firstResult();
        if (player == null) {
            throw new NotFoundException();
        }
        if (Player.find("name", newName).firstResult() != null) {
            throw new ClientErrorException("name '" + newName + "' already taken", Response.Status.CONFLICT);
        }

        long touched =
                Game.update("playerRed1 = ?1 WHERE playerRed1 = ?2", newName, oldName)
              + Game.update("playerRed2 = ?1 WHERE playerRed2 = ?2", newName, oldName)
              + Game.update("playerBlue1 = ?1 WHERE playerBlue1 = ?2", newName, oldName)
              + Game.update("playerBlue2 = ?1 WHERE playerBlue2 = ?2", newName, oldName);

        player.name = newName;

        String result = "rename: " + oldName + " -> " + newName + ", games updated: " + touched;
        return Response.ok(result).type(MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/{name}/photo")
    @Produces({"image/jpeg", "image/png", "image/webp"})
    public Response getPhoto(@PathParam("name") String name) {
        Player p = Player.find("name", name).firstResult();
        if (p == null) {
            throw new NotFoundException();
        }
        PlayerPhoto ph = PlayerPhoto.findById(p.id);
        if (ph == null) {
            throw new NotFoundException();
        }
        return Response.ok(ph.photo)
                .type(ph.photoMime)
                .header("Cache-Control", "private, max-age=60")
                .build();
    }

    @PUT
    @Path("/{name}/photo")
    @Consumes({"image/jpeg", "image/png", "image/webp"})
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response setPhoto(@PathParam("name") String name,
                             @HeaderParam("Content-Type") String contentType,
                             byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("empty body");
        }
        if (bytes.length > MAX_PHOTO_BYTES) {
            throw new ClientErrorException(
                    "photo > " + MAX_PHOTO_BYTES + " bytes",
                    Response.Status.REQUEST_ENTITY_TOO_LARGE);
        }
        String mime = stripParams(contentType);
        if (!ALLOWED_PHOTO_MIMES.contains(mime)) {
            throw new ClientErrorException(
                    "unsupported content-type: " + mime,
                    Response.Status.UNSUPPORTED_MEDIA_TYPE);
        }
        Player p = Player.find("name", name).firstResult();
        if (p == null) {
            throw new NotFoundException();
        }
        PlayerPhoto ph = PlayerPhoto.findById(p.id);
        boolean isNew = (ph == null);
        if (isNew) {
            ph = new PlayerPhoto();
            ph.playerId = p.id;
        }
        ph.photo = bytes;
        ph.photoMime = mime;
        if (isNew) {
            ph.persist();
        }
        return Response.ok("uploadPhoto: " + name + ", bytes: " + bytes.length)
                .type(MediaType.TEXT_PLAIN).build();
    }

    @DELETE
    @Path("/{name}")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response delete(@PathParam("name") String name) {
        Player p = Player.find("name", name).firstResult();
        if (p == null) {
            throw new NotFoundException();
        }
        // tbl_player_photo's FK has ON DELETE CASCADE; the photo row goes
        // with the player. Historical game rows in tbl_fights keep their
        // name strings — game history survives the player being removed.
        p.delete();
        return Response.ok("deletePlayer: " + name).type(MediaType.TEXT_PLAIN).build();
    }

    @DELETE
    @Path("/{name}/photo")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response deletePhoto(@PathParam("name") String name) {
        Player p = Player.find("name", name).firstResult();
        if (p == null) {
            throw new NotFoundException();
        }
        long deleted = PlayerPhoto.deleteById(p.id) ? 1 : 0;
        if (deleted == 0) {
            throw new NotFoundException();
        }
        return Response.ok("deletePhoto: " + name).type(MediaType.TEXT_PLAIN).build();
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

    private static String stripParams(String contentType) {
        if (contentType == null) return "";
        int semi = contentType.indexOf(';');
        return (semi >= 0 ? contentType.substring(0, semi) : contentType).trim().toLowerCase();
    }
}
