import Model.Game
import Model.GamesPostRequest
import Model.GamesPostResponse
import groovy.json.JsonSlurper
import ratpack.exec.Blocking
import ratpack.groovy.handling.GroovyChainAction

import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat

import static ratpack.jackson.Jackson.json

/**
 * Created by super on 04/10/2016.
 */

/*
 *
 * PLEASE NOTICE: This class is not used in version 0.9.X and therefore not updated.
 * It probably needs to use the new TournamentGameRound object and maybe other changes
 *
*/


class RandomTournament extends GroovyChainAction {

    @Override
    void execute() {
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
            Queue<String> randomPlayerNames = new LinkedList<String>(game.getPlayers().parallelStream().map { player -> player.getName() }.collect())
            Collections.shuffle(randomPlayerNames, new Random())

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
