package com.foosball.api;

import com.foosball.dto.GameDto;
import com.foosball.dto.GamesPostRequestDto;
import com.foosball.dto.TournamentGameRoundDto;
import com.foosball.service.TournamentPairingService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Tournament pairing endpoints.
 *
 * <ul>
 *   <li>{@code POST /tournament/randomTournament} → flat {@code List<GameDto>}.</li>
 *   <li>{@code POST /tournament/lastFirstTournament} →
 *       {@code List<TournamentGameRoundDto>} (one round per element).</li>
 *   <li>{@code POST /tournament/awesomeAlgorithmTournament} →
 *       {@code List<TournamentGameRoundDto>}.</li>
 * </ul>
 *
 * <p>{@link TxType#NEVER}: tournament generation is read-only. The awesome
 * algorithm mutates loaded {@code Game.pointsAtStake} fields in memory for
 * scoring; running outside a transaction guarantees those mutations cannot
 * flush back to the database.
 */
@Path("/tournament")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Transactional(TxType.NEVER)
public class TournamentResource {

    @Inject
    TournamentPairingService pairingService;

    @POST
    @Path("/randomTournament")
    public List<GameDto> randomTournament(GamesPostRequestDto request) {
        return pairingService.generateRandom(request);
    }

    @POST
    @Path("/lastFirstTournament")
    public List<TournamentGameRoundDto> lastFirstTournament(GamesPostRequestDto request) {
        return pairingService.generateLastFirst(request);
    }

    @POST
    @Path("/awesomeAlgorithmTournament")
    public List<TournamentGameRoundDto> awesomeAlgorithmTournament(GamesPostRequestDto request) {
        return pairingService.generateAwesome(request);
    }
}
