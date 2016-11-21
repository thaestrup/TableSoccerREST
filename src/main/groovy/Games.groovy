import Model.Game
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
//        path(":id") {
//            byMethod {
//                get {
//                    Blocking.get { ->
//                        getPlayer(pathTokens["id"])
//                    }.then { row -> render json(new Player(row)) }
//                }

//                put {
//                    parse(Player.class).onError{
//                        e -> render e.toString()
//                    }.then { p ->
//                        Blocking.get {
//                            overwritePlayer(p, pathTokens["id"])
//                        }.then{result -> render result}
//                    }
//                }
//
//                delete {
//                    Blocking.get {
//                        deletePlayer(pathTokens["id"])
//                    }.then{result -> render result}
//                }
//            }
//        }

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

//    private GroovyRowResult getPlayer(String player) {
//        DbUtil.query("SELECT * FROM tbl_players WHERE name = '" + player + "'")
//                .first()
//    }
//
//    private String overwritePlayer(Player p, String player) {
//        "overwritePlayer: " + player + ", result: " + DbUtil.execute("REPLACE INTO tbl_players (name, playerReady, oprettet) VALUES ('" + player + "', " + (p.getPlayerReady() ? 1 : 0) + ", '" + p.getOprettet() + "')")
//    }
//
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
