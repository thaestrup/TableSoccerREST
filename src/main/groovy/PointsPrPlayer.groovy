import Model.Game
import Model.PointsPrPlayerPlayer
import Model.PointsPrPlayerRequest
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimaps
import groovy.json.JsonSlurper
import ratpack.groovy.handling.GroovyChainAction

import java.util.stream.IntStream

import static ratpack.jackson.Jackson.json

import groovy.util.logging.Slf4j
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by super on 04/10/2016.
 */
class PointsPrPlayer extends GroovyChainAction {

    @Override
    void execute() {
    	Logger logger = LoggerFactory.getLogger(PointsPrPlayer.class);
	// HACK - "id" used for selecting a period
        path(":id") {
            byMethod {
                options {
                    response.headers.set('Access-Control-Allow-Methods', 'GET, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    response.headers.set('Access-Control-Allow-Headers', 'x-requested-with, origin, content-type, accept')
                    render "OK"

                }
                get {
                    String period = pathTokens["id"];

                    String hoursToGoBackInTime;
                    String filter = "";
                    switch (period) {
                      case "alltime":
                        hoursToGoBackInTime = 1000000
                        filter = "newElo"
                        break
                      case "alltime-onlylunch":
                        hoursToGoBackInTime = 1000000
                        filter = "onlylunch"
                        break
                      case "alltime-ratiofocus":
                        hoursToGoBackInTime = 1000000
                        filter = "ratiofocus"
                        break
                      case "alltime-elo":
                        hoursToGoBackInTime = 1000000
                        filter = "elo"
                        break
                      case "month":
                        hoursToGoBackInTime = 31 * 24
                        break
                      case "week":
                        hoursToGoBackInTime = 7 * 24
                        break
                      case "day":
                        hoursToGoBackInTime = 24
                        break
                      case "hour":
                        hoursToGoBackInTime = 1
                        break;
                      default:
                        hoursToGoBackInTime = -1
                        nothingFound = true;
                    }
                    List<Game> games = MoreUtil.getGamesForThisManyHoursBackInTime(hoursToGoBackInTime.toString(), filter);

                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render json(asdasd(new PointsPrPlayerRequest(2, 0, 1), games, filter))

                }
            }
        }


        all {
            byMethod {

                options {
                    response.headers.set('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')

                    render "OK"

                }

                get {

                    def url = 'http://localhost:5050/games' //TODO replace with configs un the future
                    List<Game> games = MoreUtil.getAllGames()
		                response.headers.set('Access-Control-Allow-Origin', '*')
                    render json(asdasd(new PointsPrPlayerRequest(2, 0, 1), games), "")

                }

                post {

                    def url = 'http://localhost:5050/games' //TODO replace with configs un the future

                    String urlContent = url.toURL().getText([connectTimeout: 30000, readTimeout: 30000])

                    def response = new JsonSlurper().parseText(urlContent)

                    parse(PointsPrPlayerRequest.class).onError { e ->
                        render e
                    }.onError {
                      	logger.info("ERROR")
                    }.then{ result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render json(asdasd(result, response, ""))
                    }

                }
            }
        }
    }


    private LinkedList<PointsPrPlayerPlayer> asdasd(PointsPrPlayerRequest r, response, filter) {
      	Logger logger = LoggerFactory.getLogger(Games.class);
        Map<String, Long> scores = new HashMap<>()
        Map<String, Integer> numberOfGames = new HashMap<>()

        if (filter == "newElo") {

          response.each { game ->
            logger.info("Game id: " + game.getAt("id"))
              def addOne = { String name, Integer score -> score + 1 }

              // Ensure all players have an initial ranking, if they haven't already played before
              if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), 1500)
              if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), 1500)
              if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), 1500)
              if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), 1500)

              // Ensure all players are registred in the numberOfGames, if they haven't already played before
              if (game.getAt("player_blue_1") != "null") numberOfGames.putIfAbsent(game.getAt("player_blue_1"), 0)
              if (game.getAt("player_blue_2") != "null") numberOfGames.putIfAbsent(game.getAt("player_blue_2"), 0)
              if (game.getAt("player_red_1") != "null") numberOfGames.putIfAbsent(game.getAt("player_red_1"), 0)
              if (game.getAt("player_red_2") != "null") numberOfGames.putIfAbsent(game.getAt("player_red_2"), 0)


              // Add to number of games
              if (game.getAt("player_blue_1") != "null") numberOfGames.computeIfPresent(game.getAt("player_blue_1"), addOne)
              if (game.getAt("player_blue_2") != "null") numberOfGames.computeIfPresent(game.getAt("player_blue_2"), addOne)
              if (game.getAt("player_red_1") != "null") numberOfGames.computeIfPresent(game.getAt("player_red_1"), addOne)
              if (game.getAt("player_red_2") != "null") numberOfGames.computeIfPresent(game.getAt("player_red_2"), addOne)


              // Then add their games
              if (game.getAt("match_winner").equals("blue")) {
                  if (game.getAt("player_blue_1") != "null") scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_2") != "null") scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_red_1") != "null") scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score - game.getAt("points_at_stake") })
                  if (game.getAt("player_red_2") != "null") scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score - game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), game.getAt("points_at_stake"))
                  if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), -1 * game.getAt("points_at_stake"))
                  if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), -1 * game.getAt("points_at_stake"))
              } else if (game.getAt("match_winner").equals("red")) {
                  if (game.getAt("player_red_1") != "null") scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_red_2") != "null") scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_1") != "null") scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score - game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_2") != "null") scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score - game.getAt("points_at_stake") })
                  if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), game.getAt("points_at_stake"))
                  if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), -1 * game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), -1 * game.getAt("points_at_stake"))
              } else if (game.getAt("match_winner").equals("draw")) {
                  if (game.getAt("player_red_1") != "null") scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_red_2") != "null") scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_1") != "null") scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_2") != "null") scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), game.getAt("points_at_stake"))
                  if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), game.getAt("points_at_stake"))
              }

          }
        } else if (filter == "theOldEloWhichWeDontUse") {

          response.each { game ->
            logger.info("Game id: " + game.getAt("id"))
              def addOne = { String name, Integer score -> score + 1 }

              // Ensure all players have an initial ranking, if they haven't already played before
              if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), 1500)
              if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), 1500)
              if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), 1500)
              if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), 1500)

              // Ensure all players are registred in the numberOfGames, if they haven't already played before
              if (game.getAt("player_blue_1") != "null") numberOfGames.putIfAbsent(game.getAt("player_blue_1"), 0)
              if (game.getAt("player_blue_2") != "null") numberOfGames.putIfAbsent(game.getAt("player_blue_2"), 0)
              if (game.getAt("player_red_1") != "null") numberOfGames.putIfAbsent(game.getAt("player_red_1"), 0)
              if (game.getAt("player_red_2") != "null") numberOfGames.putIfAbsent(game.getAt("player_red_2"), 0)


              // Add to number of games
              if (game.getAt("player_blue_1") != "null") numberOfGames.computeIfPresent(game.getAt("player_blue_1"), addOne)
              if (game.getAt("player_blue_2") != "null") numberOfGames.computeIfPresent(game.getAt("player_blue_2"), addOne)
              if (game.getAt("player_red_1") != "null") numberOfGames.computeIfPresent(game.getAt("player_red_1"), addOne)
              if (game.getAt("player_red_2") != "null") numberOfGames.computeIfPresent(game.getAt("player_red_2"), addOne)


              if (game.getAt("match_winner").equals("draw")) {
                // Do nothing
              } else {
                // Here we are working with winners and loosers

                /*
                  float blueWins;
                  float redWins;
                  if (game.getAt("match_winner").equals("blue")) {
                    blueWins = 1;
                    redWins = 0;
                  } else {
                    blueWins = 0;
                    redWins = 1;
                  }
                */

                int blueEloRanking;
                int redEloRanking;

                if (game.getAt("player_blue_2") != "null") {
                  blueEloRanking = (scores.get(game.getAt("player_blue_1")) + scores.get(game.getAt("player_blue_2"))) / 2;
                } else {
                  blueEloRanking = scores.get(game.getAt("player_blue_1"));
                }
                logger.info("blueEloRanking: " + blueEloRanking)

                if (game.getAt("player_red_2") != "null") {
                  redEloRanking = (scores.get(game.getAt("player_red_1")) + scores.get(game.getAt("player_red_2"))) / 2;
                } else {
                  redEloRanking = scores.get(game.getAt("player_red_1"));
                }
                logger.info("redEloRanking: " + redEloRanking)

                int redDiff = blueEloRanking - redEloRanking;

                float redWe = (1 / (Math.pow(10, ( redDiff / 1000)) + 1 ))
                float blueWe = 1 - redWe;

                logger.info("redWe: " + redWe)
                logger.info("blueWe: " + blueWe)


                int KFactor = 50;


                logger.info("if red wins: " + (KFactor * (1 - redWe)))
                //logger.info("if red looses: " + (KFactor * (0 - redWe)))

                logger.info("if blue wins: " + (KFactor * (1 - blueWe)))
                //logger.info("if blue looses: " + (KFactor * (0 - blueWe)))

                int blueRankingDiff;
                int redRankingDiff;

                if (game.getAt("match_winner").equals("blue")) {
                  //blueRankingDiff = Math.ceil(KFactor * (1 - blueWe))
                  blueRankingDiff = (KFactor * (1 - blueWe))
                  redRankingDiff = -1 * blueRankingDiff;
                } else {
                  //redRankingDiff = Math.ceil(KFactor * (1 - redWe));
                  redRankingDiff = (KFactor * (1 - redWe));
                  blueRankingDiff = -1 * redRankingDiff;
                }
                logger.info("Winner: " + game.getAt("match_winner"))
                //int newRedEloRanking = redEloRanking + (KFactor * (redWins - redWe));
                //int redRankingDiff = newRedEloRanking - redEloRanking;
                logger.info("redRankingDiff: " + redRankingDiff)

                //int newBlueEloRanking = blueEloRanking + (KFactor * (blueWins - blueWe));
                //int blueRankingDiff = -1 * redRankingDiff;
                logger.info("blueRankingDiff: " + blueRankingDiff)

                // If 2 vs 2
                if (game.getAt("player_blue_1") != "null" &&
                    game.getAt("player_red_1") != "null" &&
                    game.getAt("player_red_2") != "null" &&
                    game.getAt("player_blue_2") != "null" ) {
                    scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + blueRankingDiff })
                    scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + blueRankingDiff })
                    scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + redRankingDiff })
                    scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + redRankingDiff })
                }

                // Else if 1 vs 1
                else if (game.getAt("player_blue_2") == "null" &&
                    game.getAt("player_red_2") == "null") {
                    scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + blueRankingDiff })
                    scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + redRankingDiff })
                } else {
                  // Then one of the teams consist of only 1 person and we ensure that we get something that can be divided evenly between
                  // two team members
                  int blueRankingDiffInHalfAndEven = blueRankingDiff / 2
                  int redRankingDiffInHalfAndEven = -1 * blueRankingDiffInHalfAndEven
                  logger.info("blueRankingDiffInHalfAndEven: " + blueRankingDiffInHalfAndEven)
                  logger.info("redRankingDiffInHalfAndEven: " + redRankingDiffInHalfAndEven)
                  scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + blueRankingDiffInHalfAndEven })
                  scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + redRankingDiffInHalfAndEven })

                  if (game.getAt("player_blue_2") == "null") {
                    // Then blue 1 is alone
                    scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + blueRankingDiffInHalfAndEven })
                    scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + redRankingDiffInHalfAndEven })
                  } else {
                    // Then red 1 is alone
                    scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + redRankingDiffInHalfAndEven })
                    scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + blueRankingDiffInHalfAndEven })
                  }

                }

                /*
                if (game.getAt("match_winner").equals("blue")) {
                    if (game.getAt("player_blue_1") != "null") scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + 1 })
                    if (game.getAt("player_blue_2") != "null") scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + 1 })
                    if (game.getAt("player_red_1") != "null") scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score - 1 })
                    if (game.getAt("player_red_2") != "null") scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score - 1 })
                } else if (game.getAt("match_winner").equals("red")) {
                    if (game.getAt("player_red_1") != "null") scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + 1 })
                    if (game.getAt("player_red_2") != "null") scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + 1 })
                    if (game.getAt("player_blue_1") != "null") scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score - 1 })
                    if (game.getAt("player_blue_2") != "null") scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score - 1 })

                }
                */
              }

          }
        } else if (filter == "ratiofocus") {

          response.each { game ->
              def addOne = { String name, Integer score -> score + 1 }

              if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), 0)
              if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), 0)
              if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), 0)
              if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), 0)

              if (game.getAt("match_winner").equals("draw")) {
                // Do nothing
              } else {
                // Then we have winners and loosers
                if (game.getAt("player_blue_1") != "null") numberOfGames.computeIfPresent(game.getAt("player_blue_1"), addOne)
                if (game.getAt("player_blue_2") != "null") numberOfGames.computeIfPresent(game.getAt("player_blue_2"), addOne)
                if (game.getAt("player_red_1") != "null") numberOfGames.computeIfPresent(game.getAt("player_red_1"), addOne)
                if (game.getAt("player_red_2") != "null") numberOfGames.computeIfPresent(game.getAt("player_red_2"), addOne)

                if (game.getAt("player_blue_1") != "null") numberOfGames.putIfAbsent(game.getAt("player_blue_1"), 1)
                if (game.getAt("player_blue_2") != "null") numberOfGames.putIfAbsent(game.getAt("player_blue_2"), 1)
                if (game.getAt("player_red_1") != "null") numberOfGames.putIfAbsent(game.getAt("player_red_1"), 1)
                if (game.getAt("player_red_2") != "null") numberOfGames.putIfAbsent(game.getAt("player_red_2"), 1)

                if (game.getAt("match_winner").equals("blue")) {
                    if (game.getAt("player_blue_1") != "null") scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + 1 })
                    if (game.getAt("player_blue_2") != "null") scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + 1 })
                    if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), 1)
                    if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), 1)
                } else if (game.getAt("match_winner").equals("red")) {
                    if (game.getAt("player_red_1") != "null") scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + 1 })
                    if (game.getAt("player_red_2") != "null") scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + 1 })
                    if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), 1)
                    if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), 1)
                }
              }
          }
        } else {
          response.each { game ->
              def addOne = { String name, Integer score -> score + 1 }

              if (game.getAt("player_blue_1") != "null") numberOfGames.computeIfPresent(game.getAt("player_blue_1"), addOne)
              if (game.getAt("player_blue_2") != "null") numberOfGames.computeIfPresent(game.getAt("player_blue_2"), addOne)
              if (game.getAt("player_red_1") != "null") numberOfGames.computeIfPresent(game.getAt("player_red_1"), addOne)
              if (game.getAt("player_red_2") != "null") numberOfGames.computeIfPresent(game.getAt("player_red_2"), addOne)

              if (game.getAt("player_blue_1") != "null") numberOfGames.putIfAbsent(game.getAt("player_blue_1"), 1)
              if (game.getAt("player_blue_2") != "null") numberOfGames.putIfAbsent(game.getAt("player_blue_2"), 1)
              if (game.getAt("player_red_1") != "null") numberOfGames.putIfAbsent(game.getAt("player_red_1"), 1)
              if (game.getAt("player_red_2") != "null") numberOfGames.putIfAbsent(game.getAt("player_red_2"), 1)

              if (game.getAt("match_winner").equals("blue")) {
                  if (game.getAt("player_blue_1") != "null") scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_2") != "null") scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_red_1") != "null") scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score - game.getAt("points_at_stake") })
                  if (game.getAt("player_red_2") != "null") scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score - game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), game.getAt("points_at_stake"))
                  if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), -1 * game.getAt("points_at_stake"))
                  if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), -1 * game.getAt("points_at_stake"))
              } else if (game.getAt("match_winner").equals("red")) {
                  if (game.getAt("player_red_1") != "null") scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_red_2") != "null") scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_1") != "null") scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score - game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_2") != "null") scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score - game.getAt("points_at_stake") })
                  if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), game.getAt("points_at_stake"))
                  if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), -1 * game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), -1 * game.getAt("points_at_stake"))
              } else if (game.getAt("match_winner").equals("draw")) {
                  if (game.getAt("player_red_1") != "null") scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_red_2") != "null") scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_1") != "null") scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_blue_2") != "null") scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + game.getAt("points_at_stake") })
                  if (game.getAt("player_red_1") != "null") scores.putIfAbsent(game.getAt("player_red_1"), game.getAt("points_at_stake"))
                  if (game.getAt("player_red_2") != "null") scores.putIfAbsent(game.getAt("player_red_2"), game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_1") != "null") scores.putIfAbsent(game.getAt("player_blue_1"), game.getAt("points_at_stake"))
                  if (game.getAt("player_blue_2") != "null") scores.putIfAbsent(game.getAt("player_blue_2"), game.getAt("points_at_stake"))
              }
          }

        }

        if (filter == "ratiofocus") {
          Map<String, Long> scoresTemp = new HashMap<>()
          scores.keySet().each { playerName ->
              //scores.set()
              //int position = IntStream.range(0, ranking.indexOf(scores.get(playerName))).map{i -> playersPrValue.get(ranking.get(i)).size()}.sum() + 1
              BigDecimal initialResult = (scores.get(playerName) * 100) / numberOfGames.get(playerName);
              //BigDecimal roundedResult = initialResult.setScale(0, Java.Math.RoundingMode.FLOOR);
              scoresTemp.putIfAbsent(playerName, initialResult.intValue())
          }
          scores = scoresTemp;
        }

        //For calculating potitions in the ranking.
        HashMultimap<Integer, String> playersPrValue =
                Multimaps.invertFrom(Multimaps.forMap(scores),
                        HashMultimap.<Integer, String> create());
        ArrayList<Integer> ranking = new ArrayList<>(playersPrValue.keySet())
        ranking.sort { points -> -points }

        List<PointsPrPlayerPlayer> result = new LinkedList<>()
        scores.keySet().each { playerName ->
            int position = IntStream.range(0, ranking.indexOf(scores.get(playerName))).map{i -> playersPrValue.get(ranking.get(i)).size()}.sum() + 1
            result.add(new PointsPrPlayerPlayer(position, scores.get(playerName), numberOfGames.get(playerName), playerName))
        }
        //Biggest potition first
        result.sort { player -> player.getPosition() }

        result
    }
}
