import Model.PointsPrPlayerPlayer
import Model.PointsPrPlayerRequest
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimaps
import groovy.json.JsonSlurper
import ratpack.groovy.handling.GroovyChainAction

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
                    response.headers.set('Access-Control-Allow-Methods:', 'POST, GET, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render "OK"
                }

                get {
                    def url = 'http://localhost:5050/games' //TODO replace with configs un the future
                    def response = new JsonSlurper().parseText(url.toURL().text)
                    render json(asdasd(new PointsPrPlayerRequest(2, 0, 1), response))
                }

                post {
                    def url = 'http://localhost:5050/games' //TODO replace with configs un the future
                    def response = new JsonSlurper().parseText(url.toURL().text)
                    PointsPrPlayerRequest r = null;

                    parse(PointsPrPlayerRequest.class).onError { e ->
                        render e
                    }.then{ result ->
                        render json(asdasd(result, response))
                    }
                }
            }
        }
    }

    private LinkedList<PointsPrPlayerPlayer> asdasd(PointsPrPlayerRequest r, response) {
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
                scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + r.getWinnerPoints() })
                scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + r.getWinnerPoints() })
                scores.putIfAbsent(game.getAt("player_blue_1"), r.getWinnerPoints())
                scores.putIfAbsent(game.getAt("player_blue_2"), r.getWinnerPoints())
                scores.putIfAbsent(game.getAt("player_red_1"), r.getLoserPoints())
                scores.putIfAbsent(game.getAt("player_red_2"), r.getLoserPoints())
            } else if (game.getAt("match_winner").equals("red")) {
                scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + r.getWinnerPoints() })
                scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + r.getWinnerPoints() })
                scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + r.getLoserPoints() })
                scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + r.getLoserPoints() })
                scores.putIfAbsent(game.getAt("player_red_1"), r.getWinnerPoints())
                scores.putIfAbsent(game.getAt("player_red_2"), r.getWinnerPoints())
                scores.putIfAbsent(game.getAt("player_blue_1"), r.getLoserPoints())
                scores.putIfAbsent(game.getAt("player_blue_2"), r.getLoserPoints())
            } else if (game.getAt("match_winner").equals("even")) {
                scores.computeIfPresent(game.getAt("player_red_1"), { String name, Integer score -> score + r.getEvenPoints() })
                scores.computeIfPresent(game.getAt("player_red_2"), { String name, Integer score -> score + r.getEvenPoints() })
                scores.computeIfPresent(game.getAt("player_blue_1"), { String name, Integer score -> score + r.getEvenPoints() })
                scores.computeIfPresent(game.getAt("player_blue_2"), { String name, Integer score -> score + r.getEvenPoints() })
                scores.putIfAbsent(game.getAt("player_red_1"), r.getEvenPoints())
                scores.putIfAbsent(game.getAt("player_red_2"), r.getEvenPoints())
                scores.putIfAbsent(game.getAt("player_blue_1"), r.getEvenPoints())
                scores.putIfAbsent(game.getAt("player_blue_2"), r.getEvenPoints())
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
            result.add(new PointsPrPlayerPlayer(ranking.indexOf(scores.get(playerName)), scores.get(playerName), numberOfGames.get(playerName), playerName))
        }

        //Biggest potition first
        result.sort { player -> player.getPosition() }
        result
    }


}
