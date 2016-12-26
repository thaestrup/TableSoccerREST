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
                    switch (period) {
                      case "alltime":
                        hoursToGoBackInTime = 1000000
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
                    List<Game> games = MoreUtil.getGamesForThisManyHoursBackInTime(hoursToGoBackInTime.toString());

                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render json(asdasd(new PointsPrPlayerRequest(2, 0, 1), games))

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
                    render json(asdasd(new PointsPrPlayerRequest(2, 0, 1), games))

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
                        render json(asdasd(result, response))
                    }

                }
            }
        }
    }


    private LinkedList<PointsPrPlayerPlayer> asdasd(PointsPrPlayerRequest r, response) {
      	Logger logger = LoggerFactory.getLogger(Games.class);
        Map<String, Integer> scores = new HashMap<>()
        Map<String, Integer> numberOfGames = new HashMap<>()

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
