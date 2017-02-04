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
 * Created by NIKL
 */
class AwesomeAlgorithmTournament extends GroovyChainAction {

    @Override
    void execute() {
    	Logger logger = LoggerFactory.getLogger(AwesomeAlgorithmTournament.class);
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
    	Logger logger = LoggerFactory.getLogger(AwesomeAlgorithmTournament.class);
      logger.info("før random:");

      List<Game> result = new LinkedList<>();
      //result.add(null)

      LinkedList<String> namesOfAvailablePlayers = new LinkedList<String>(game.getPlayers().stream().map { player -> player.getName() }.collect())


      if (game.getPlayers() == null) {
        return null;
      } else {
        //logger.info("namesOfAvailablePlayers");
        logger.info("names: " + namesOfAvailablePlayers);

        int maxPlayersNeeded = 4 * game.getNumberOfGames()
        logger.info("original maxPlayersNeeded: " + maxPlayersNeeded)
        if (namesOfAvailablePlayers.size() >= 0 && namesOfAvailablePlayers.size() <= 3) {
          maxPlayersNeeded = namesOfAvailablePlayers.size();
        } else if (namesOfAvailablePlayers.size() <= 5) {
          // If only players for 1 table
          if (maxPlayersNeeded > 4) maxPlayersNeeded = 4;
        } else if (namesOfAvailablePlayers.size() > 5 && namesOfAvailablePlayers.size() <= 7) {
          maxPlayersNeeded = namesOfAvailablePlayers.size();
        } else if (namesOfAvailablePlayers.size() <= 9) {
          // If only players for 2 tables
          if (maxPlayersNeeded > 8) maxPlayersNeeded = 8;
        } else if (namesOfAvailablePlayers.size() > 9 && namesOfAvailablePlayers.size() <= 11) {
          maxPlayersNeeded = namesOfAvailablePlayers.size();
        } else if (namesOfAvailablePlayers.size() <= 13) {
          // If only players for 3 tables
          if (maxPlayersNeeded > 12) maxPlayersNeeded = 12;
       }
       logger.info("numberOfTables: " + game.getNumberOfGames())
       logger.info("adjusted maxPlayersNeeded: " + maxPlayersNeeded)

      LinkedList<String> newRealList = AdditionalUtil.generateAwesomeList(maxPlayersNeeded, namesOfAvailablePlayers);

  /** --------------- OLD STUFF BELOW ---------------------------------
          Random random = new Random(System.currentTimeMillis());
          if (game.getPlayers() != null) {
              Map<String, Long> playersLastPlayed = MoreUtil.playersLastPlayed();

              LinkedList<String> randomPlayerNames = new LinkedList<String>(game.getPlayers().stream().map { player -> player.getName() }.collect())
              logger.info("før random: " + randomPlayerNames)
              randomPlayerNames.sort{element -> random.nextLong() }
              logger.info("efter random: " + randomPlayerNames)
              randomPlayerNames.sort{element -> playersLastPlayed.get(element)}
              logger.info("efter get: " + randomPlayerNames)
              logger.info("-----------------")



              LinkedList<String> realList = randomPlayerNames.subList(0, maxPlayersNeeded)
              Collections.shuffle(realList)

          }
     --------------- OLD STUFF ABOVE --------------------------------- **/

           LinkedList<String> realList = newRealList; //namesOfAvailablePlayers; //.subList(0, maxPlayersNeeded)
           int count = 0;

           while (realList.size() > 0) {
               String player_red_1 = realList.poll();
               String player_red_2 = realList.poll();
               String player_blue_1 = realList.poll();
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

          List<List<Game>> resultAsArray = new LinkedList<LinkedList<Game>>();
          TournamentGameRound tournamentGameRound = new TournamentGameRound(result)
          resultAsArray.add(tournamentGameRound)



          return resultAsArray;
        }
    }


}
