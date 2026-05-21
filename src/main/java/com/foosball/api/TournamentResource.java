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
 * Tournament resource — port of legacy
 * {@code RandomTournament.groovy}, {@code LastFirstTournament.groovy},
 * and {@code AwesomeAlgorithmTournament.groovy}.
 *
 * <p>Wire contract preserved (intentional shape divergence — see
 * {@code TournamentContractTest}):
 * <ul>
 *   <li>{@code POST /tournament/randomTournament} returns a flat
 *       {@code List<GameDto>}, NOT a rounds wrapper.</li>
 *   <li>{@code POST /tournament/lastFirstTournament} returns
 *       {@code List<TournamentGameRoundDto>} (one round, currently).</li>
 *   <li>{@code POST /tournament/awesomeAlgorithmTournament} returns
 *       {@code List<TournamentGameRoundDto>} (one round, currently).</li>
 * </ul>
 *
 * <p>OPTIONS preflight is handled by the global CORS filter — we do
 * not register OPTIONS handlers here.
 */
@Path("/tournament")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
// Defense-in-depth: tournament generation is read-only. The awesome
// algorithm mutates Game.pointsAtStake on loaded entities for in-memory
// scoring (see TournamentPairingService.generateAwesomeList line 283).
// Empirical test 2026-05-10 confirmed those mutations do NOT flush in the
// current Quarkus/Hibernate config, but @Transactional(NEVER) makes the
// invariant explicit so a future change can't silently start writing
// pointsAtStake deltas back to the DB.
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
