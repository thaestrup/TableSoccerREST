import Model.PointsPrPlayerPlayer
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimaps
import groovy.json.JsonSlurper
import ratpack.groovy.handling.GroovyChainAction

import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat

import static ratpack.jackson.Jackson.json

/**
 * Created by super on 04/10/2016.
 */
class PointsPrPlayer extends GroovyChainAction {

    @Override
    void execute() {
        all {
            byMethod {

                options {
                    response.headers.set('Access-Control-Allow-Methods:', 'GET')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render "OK"
                }

                get {
                    def url = 'http://localhost:5050/games' //TODO replace with configs un the future
                    def response = new JsonSlurper().parseText(url.toURL().text)

                    Map<String, Integer> scores = new HashMap<>()
                    Map<String, Integer> numberOfGames = new HashMap<>()
                    response.each { game ->
                        def addOne = { String name, Integer score -> score + 1 }
                        numberOfGames.computeIfPresent(game.getAt("player_blue_1"), addOne)
                        numberOfGames.computeIfPresent(game.getAt("player_blue_2"), addOne)
                        numberOfGames.computeIfPresent(game.getAt("player_red_1"), addOne)
                        numberOfGames.computeIfPresent(game.getAt("player_red_2"), addOne)

                        numberOfGames.putIfAbsent(game.getAt("player_blue_1"), 1)
                        numberOfGames.putIfAbsent(game.getAt("player_blue_2"), 1)
                        numberOfGames.putIfAbsent(game.getAt("player_red_1"), 1)
                        numberOfGames.putIfAbsent(game.getAt("player_red_2"), 1)

                        if (game.getAt("match_winner").equals("blue")) {
                            scores.computeIfPresent(game.getAt("player_blue_1"), addOne)
                            scores.computeIfPresent(game.getAt("player_blue_2"), addOne)
                            scores.putIfAbsent(game.getAt("player_blue_1"), 1)
                            scores.putIfAbsent(game.getAt("player_blue_2"), 1)
                        } else if (game.getAt("match_winner").equals("red")) {
                            scores.computeIfPresent(game.getAt("player_red_1"), addOne)
                            scores.computeIfPresent(game.getAt("player_red_2"), addOne)
                            scores.putIfAbsent(game.getAt("player_red_1"), 1)
                            scores.putIfAbsent(game.getAt("player_red_2"), 1)
                        }
                    }

                    //For calculating potitions in the ranking.
                    HashMultimap<Integer, String> playersPrValue =
                            Multimaps.invertFrom(Multimaps.forMap(scores),
                                    HashMultimap.<Integer, String> create());
                    ArrayList<Integer> ranking = new ArrayList<>(playersPrValue.keySet())
                    ranking.sort{points -> -points}

                    List<PointsPrPlayerPlayer> result = new LinkedList<>()
                    scores.keySet().each { playerName ->
                        result.add(new PointsPrPlayerPlayer(ranking.indexOf(scores.get(playerName)), scores.get(playerName), numberOfGames.get(playerName), playerName))
                    }

                    //Biggest potition first
                    result.sort{player -> player.getPosition()}

                    render json(result)
                }
            }
        }
    }


}
