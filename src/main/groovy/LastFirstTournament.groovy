import Model.Game
import Model.GamesPostRequest
import Model.TournamentGameRound
import groovy.json.JsonSlurper
import ratpack.exec.Blocking
import ratpack.groovy.handling.GroovyChainAction

import java.sql.Timestamp

import static ratpack.jackson.Jackson.json

import groovy.util.logging.Slf4j
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by super on 04/10/2016.
 */
class LastFirstTournament extends GroovyChainAction {

    @Override
    void execute() {
    	Logger logger = LoggerFactory.getLogger(LastFirstTournament.class);
        all {
            byMethod {

                options {
                    response.headers.set('Access-Control-Allow-Methods', 'POST, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    response.headers.set('Access-Control-Allow-Headers', 'x-requested-with, origin, content-type, accept')
                    render "OK"
                }

                post {
                    parse(GamesPostRequest.class).onError {
                        e -> render e.toString()
                    }.onError {
                      logger.info("ERROR in parse")
                    }.then { p ->
                        Blocking.get {
                            generateGames(p)
                        }.onError {
                          logger.info("ERROR in Blocking.get")
                        }.then { result ->
                            response.headers.set('Access-Control-Allow-Methods', 'POST, OPTIONS')
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            response.headers.set('Access-Control-Allow-Headers', 'x-requested-with, origin, content-type, accept')

                            render json(result)

                        }
                    }
                }
            }
        }
    }



    private List<List<Game>> generateGames(GamesPostRequest game) {
    	Logger logger = LoggerFactory.getLogger(LastFirstTournament.class);
        List<Game> result = new LinkedList<>();
        Random random;
        if (game.getPlayers() != null) {
            Map<String, Long> playersLastPlayed = MoreUtil.playersLastPlayed();

            LinkedList<String> randomPlayerNames = new LinkedList<String>(game.getPlayers().stream().map { player -> player.getName() }.collect())
            randomPlayerNames.sort{element -> new Random() }
            randomPlayerNames.sort{element -> playersLastPlayed.get(element)}
            int maxPlayersNeeded = 4 * game.getNumberOfGames()
            if (randomPlayerNames.size() >= 0 && randomPlayerNames.size() <= 3) {
              maxPlayersNeeded = randomPlayerNames.size();
            } else if (randomPlayerNames.size() <= 5) {
              // If only players for 1 table
              if (maxPlayersNeeded > 4) maxPlayersNeeded = 4;
            } else if (randomPlayerNames.size() > 5 && randomPlayerNames.size() <= 7) {
              maxPlayersNeeded = randomPlayerNames.size();
            } else if (randomPlayerNames.size() <= 9) {
              // If only players for 2 tables
              if (maxPlayersNeeded > 8) maxPlayersNeeded = 8;
            } else if (randomPlayerNames.size() > 9 && randomPlayerNames.size() <= 11) {
              maxPlayersNeeded = randomPlayerNames.size();
            } else if (randomPlayerNames.size() <= 13) {
              // If only players for 3 tables
              if (maxPlayersNeeded > 12) maxPlayersNeeded = 12;
           }

            LinkedList<String> realList = randomPlayerNames.subList(0, maxPlayersNeeded)
            Collections.shuffle(realList, new Random())
            int count = 0;

            while (realList.size() > 0) {

                String player_red_1 = realList.poll();
                String player_blue_1 = realList.poll();
                String player_red_2 = realList.poll();
                String player_blue_2 = realList.poll();

                Game newGame = new Game(
                        null,
                        player_red_1,
                        player_red_2,
                        player_blue_1,
                        player_blue_2,
                        new Timestamp(System.currentTimeMillis()).toString(),
                        "",
                        "-1",
                        "-1");
                // Don't return games, where there isn't at least 1 player on both teams
                if (player_blue_1 != null) {
                  result.add(newGame)
                }
                count++;

                if (count >= game.getNumberOfGames()) {

                    break;
                }

            }

        }
        List<List<Game>> resultAsArray = new LinkedList<LinkedList<Game>>();
        TournamentGameRound tournamentGameRound = new TournamentGameRound(result)
        resultAsArray.add(tournamentGameRound)

        return resultAsArray;
    }
}
