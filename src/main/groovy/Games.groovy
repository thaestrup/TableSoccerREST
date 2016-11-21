import Model.Game
import groovy.sql.GroovyRowResult
import ratpack.exec.Blocking
import ratpack.groovy.handling.GroovyChainAction
import ratpack.jackson.JsonRender

import static ratpack.jackson.Jackson.json

/**
 * Created by super on 04/10/2016.
 */
class Games extends GroovyChainAction {

    @Override
    void execute() {
        path(":id") {
            byMethod {
                get {
                    Blocking.get { ->
                        getGame(pathTokens["id"])
                    }.then { row -> render json(new Game(row)) }
                }

                put {
                    parse(Game.class).onError{
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.get {
                            overwriteGame(p, pathTokens["id"])
                        }.then{result -> render result}
                    }
                }
//
//                delete {
//                    Blocking.get {
//                        deletePlayer(pathTokens["id"])
//                    }.then{result -> render result}
//                }
            }
        }

        all {
            byMethod {
                get {
                    Blocking.get {
                        getAllGames()
                    }.then{result -> render result}
                }

//                put {
//                    parse(listOf(Player.class)).onError{
//                        e -> render e.toString()
//                    }.then { p ->
//                        Blocking.get {
//                            def result = cleanPlayerTable()
//                            p.stream().map { q ->
//                                insertPlayer(q)
//                            }.collect(Collectors.joining(System.lineSeparator(), result + System.lineSeparator(), ""))
//                        }.then{result -> render result}
//                    }
//                }
//
//                post {
//                    parse(listOf(Player.class)).onError{
//                        e -> render e.toString()
//                    }.then { p ->
//                        Blocking.get {
//                            p.stream().map { q ->
//                                insertPlayer(q)
//                            }.collect(Collectors.joining(System.lineSeparator()))
//                        }.then{result -> render result}
//                    }
//                }
//
//                delete {
//                    Blocking.get {
//                        cleanPlayerTable()
//                    }.then{result -> render result}
//                }
            }
        }
    }

    private GroovyRowResult getGame(String id) {
        DbUtil.query("SELECT * FROM tbl_fights WHERE id = '" + id + "'")
                .first()
    }

    private String overwriteGame(Game game, String id) {
        "overwriteGame: " + id + ", result: " + DbUtil.execute("REPLACE INTO tbl_fights (id, player_red_1, player_red_2, player_blue_1, player_blue_2, timestamp, match_winner, points_at_steake, winning_table) VALUES ('" + id + "', '" + game.getPlayer_red_1() + "', '" + game.getPlayer_red_2() + "', '" + game.getPlayer_blue_1() + "', '" + game.getPlayer_blue_2() + "', '" + game.getLastUpdated() + "', '" + game.getMatch_winner() + "', '" + game.getPoints_at_stake() + "', '" + game.getWinning_table() + "')")
    }

//    private String deletePlayer(String player) {
//        "deletePlayer: " + player + ", result: " + DbUtil.execute("DELETE FROM tbl_players where name = '" + player + "'")
//    }

    private JsonRender getAllGames() {
        json(DbUtil.query("SELECT * FROM tbl_fights").collect { row -> new Game(row) })
    }

//    private String cleanPlayerTable() {
//        "cleanPlayerTable: " + DbUtil.execute("Truncate table tbl_players")
//    }
//
//    private String insertPlayer(Player q) {
//        "insertPlayer: " + q.getName() + ", result: " + DbUtil.execute("INSERT INTO tbl_players (name, playerReady, oprettet) VALUES ('" + q.getName() + "', " + (q.getPlayerReady() ? 1 : 0) + ", '" + q.getOprettet() + "')")
//    }
}
