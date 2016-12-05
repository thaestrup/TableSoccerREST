import Model.Game
import Model.GamesPostRequest
import groovy.json.JsonSlurper
import ratpack.exec.Blocking
import ratpack.groovy.handling.GroovyChainAction

import java.sql.Timestamp

import static ratpack.jackson.Jackson.json

/**
 * Created by super on 04/10/2016.
 */
class LastFirstTournament extends GroovyChainAction {

    @Override
    void execute() {
        all {
            byMethod {

                options {
                    response.headers.set('Access-Control-Allow-Methods:', 'POST, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render "OK"
                }

                post {
                    parse(GamesPostRequest.class).onError {
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.get {
                            generateGames(p)
                        }.then { result ->
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            render json(result)
                        }
                    }
                }
            }
        }
    }

    private List<Game> generateGames(GamesPostRequest game) {
        List<Game> result = new LinkedList<>();

        if (game.getPlayers() != null) {
            def url = 'http://localhost:5050/statisticsPlayersLastPlayed' //TODO replace with configs in the future
            Map<String, Long> playersLastPlayed = new HashMap<>(new JsonSlurper().parseText(url.toURL().text))
            LinkedList<String> randomPlayerNames = new LinkedList<String>(game.getPlayers().stream().map { player -> player.getName() }.collect())

            randomPlayerNames.sort{element -> playersLastPlayed.get(element)}

            int count = 0;
            while (randomPlayerNames.size() > 0) {
                String player_red_1 = randomPlayerNames.poll();
                String player_blue_1 = randomPlayerNames.poll();
                String player_red_2 = randomPlayerNames.poll();
                String player_blue_2 = randomPlayerNames.poll();
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
                result.add(newGame)
                count++;
                if (count >= game.getNumberOfGames()) {
                    break;
                }
            }
        }

        return result;
    }
}
