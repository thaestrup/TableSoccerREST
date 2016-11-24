import Model.Game
import Model.PostGameRequest
import groovy.sql.GroovyRowResult
import ratpack.exec.Blocking
import ratpack.groovy.handling.GroovyChainAction
import ratpack.jackson.JsonRender

import java.util.stream.Collectors

import static ratpack.jackson.Jackson.json
import static ratpack.util.Types.listOf;

/**
 * Created by super on 04/10/2016.
 */
class Games extends GroovyChainAction {

    @Override
    void execute() {
        path(":id") {
            byMethod {
                options {
                    response.headers.set('Access-Control-Allow-Methods:', 'GET, OPTIONS, PUT, DELETE')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render "OK"
                }

                get {
                    Blocking.get { ->
                        getGame(pathTokens["id"])
                    }.then { result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render json(new Game(result))
                    }
                }

                put {
                    parse(Game.class).onError{
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.get {
                            overwriteGame(p, pathTokens["id"])
                        }.then{result ->
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            render result
                        }
                    }
                }

                delete {
                    Blocking.get {
                        deleteGame(pathTokens["id"])
                    }.then{result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render result
                    }
                }
            }
        }

        all {
            byMethod {
                options {
                    response.headers.set('Access-Control-Allow-Methods:', 'POST, GET, OPTIONS, PUT, DELETE')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render "OK"
                }

                get {
                    Blocking.get {
                        getAllGames()
                    }.then{result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render result
                    }
                }

                put {
                    parse(listOf(Game.class)).onError{
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.get {
                            def result = cleanGameTable()
                            p.stream().map { q ->
                                insertGame(q)
                            }.collect(Collectors.joining(System.lineSeparator(), result + System.lineSeparator(), ""))
                        }.then{result ->
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            render result
                        }
                    }
                }

                post {
                    parse(PostGameRequest.class).onError{
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.get {
                            generateGames(p)
                        }.then{result ->
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            render result
                        }
                    }
                }

                delete {
                    Blocking.get {
                        cleanGameTable()
                    }.then{result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render result
                    }
                }
            }
        }
    }

    private String generateGames(PostGameRequest game) {
        StringBuffer result = new StringBuffer();

        if (game.generationMethod == PostGameRequest.GenerationMethod.GIVEN) {

        } else if (game.generationMethod == PostGameRequest.GenerationMethod.LASTFIRST) {

        } else if (game.generationMethod == PostGameRequest.GenerationMethod.RANDOM) {

        } else {
            result.append("generationMethod: ").append(game.generationMethod).append(" not known.")
        }

        return result;
    }

    private GroovyRowResult getGame(String id) {
        DbUtil.query("SELECT * FROM tbl_fights WHERE id = '" + id + "'")
                .first()
    }

    private String overwriteGame(Game game, String id) {
        "overwriteGame: " + id + ", result: " + DbUtil.execute("REPLACE INTO tbl_fights (id, player_red_1, player_red_2, player_blue_1, player_blue_2, timestamp, match_winner, points_at_steake, winning_table) VALUES ('" + id + "', '" + game.getPlayer_red_1() + "', '" + game.getPlayer_red_2() + "', '" + game.getPlayer_blue_1() + "', '" + game.getPlayer_blue_2() + "', '" + game.getLastUpdated() + "', '" + game.getMatch_winner() + "', '" + game.getPoints_at_stake() + "', '" + game.getWinning_table() + "')")
    }

    private String deleteGame(String gameId) {
        "deleteGame: " + gameId + ", result: " + DbUtil.execute("DELETE FROM tbl_fights where id = '" + gameId + "'")
    }

    private JsonRender getAllGames() {
        json(DbUtil.query("SELECT * FROM tbl_fights").collect { row -> new Game(row) })
    }

    private String cleanGameTable() {
        "cleanGameTable: " + DbUtil.execute("Truncate table tbl_fights")
    }

    private String insertGame(Game game) {
        "insertGame: " + game.getId() + ", result: " + DbUtil.execute("INSERT INTO tbl_fights (id, player_red_1, player_red_2, player_blue_1, player_blue_2, timestamp, match_winner, points_at_steake, winning_table) VALUES ('" + game.getId() + "', '" + game.getPlayer_red_1() + "', '" + game.getPlayer_red_2() + "', '" + game.getPlayer_blue_1() + "', '" + game.getPlayer_blue_2() + "', '" + game.getLastUpdated() + "', '" + game.getMatch_winner() + "', '" + game.getPoints_at_stake() + "', '" + game.getWinning_table() + "')")
    }


}
