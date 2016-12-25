import Model.Game
import Model.GamesPostResponse
import groovy.sql.GroovyRowResult
import ratpack.exec.Blocking
import ratpack.groovy.handling.GroovyChainAction
import ratpack.jackson.JsonRender

import java.util.stream.Collectors

import static ratpack.jackson.Jackson.json
import static ratpack.util.Types.listOf;

import groovy.util.logging.Slf4j
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by super on 04/10/2016.
 */
 @Slf4j
class Games extends GroovyChainAction {

    @Override
    void execute() {
      	Logger logger = LoggerFactory.getLogger(Games.class);

        // HACKING - Using the old "id" for period, so therefore temporarily not using put and delete, while changing options and get
        path(":id") {
            byMethod {
                // HACKING
                options {
                    // HACK response.headers.set('Access-Control-Allow-Methods', 'GET, OPTIONS, PUT, DELETE')
                    response.headers.set('Access-Control-Allow-Methods', 'GET, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    response.headers.set('Access-Control-Allow-Headers', 'x-requested-with, origin, content-type, accept')
                    render "OK"
                }
                // HACKING
                get {
                    Blocking.get {
		    	              // HACK - Using id to test whether a known period was asked for (if not, it is a name that is asked for)
                        String period = pathTokens["id"];
                        String hoursToGoBackInTime;
                        boolean nothingFound = false;
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
                        // DAMN, this is the ugliest hack ever! (I almost get proud :-)
                        if (nothingFound) {
                          // Didn't find "alltime", "week", "day" or "hour", so we should return for a person instead
			                    String name = period
                          getGamesForName(name)

                        } else {
                          getGamesForThisManyHoursBackInTime(hoursToGoBackInTime)
                        }

                    }.onError {
                      logger.info("ERROR - in Blocking.get")
                    }.then { result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render result
                    }
                }
                /* HACKING - Using the old "id" for period, so therefore temporarily not using put and delete in version 0.9.X
                put {
                    parse(Game.class).onError {
                        e -> render e.toString()
                    }.onError {
                      logger.info("ERROR")
                    }.then { p ->
                        Blocking.get {
                            overwriteGame(p, pathTokens["id"])
                        }.onError {
                      	    logger.info("ERROR")
                        }.then { result ->
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            render result
                        }
                    }
                }

                delete {
                    Blocking.get {
                        deleteGame(pathTokens["id"])
                    }.onError {
                      logger.info("ERROR")
                    }.then { result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render result
                    }
                }
                */
            }
        }

        all {
            byMethod {
                options {
                    response.headers.set('Access-Control-Allow-Methods', 'POST, GET, OPTIONS, PUT, DELETE')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    response.headers.set('Access-Control-Allow-Headers', 'x-requested-with, origin, content-type, accept')
                    render "OK"
                }

                get {
                    Blocking.get {
                        getAllGames()
                    }.onError {
                      logger.info("ERROR")
                    }.then { result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render result
                    }
                }

                put {
                    parse(listOf(Game.class)).onError {

                        e -> render e.toString()
                    }.onError {
                      logger.info("ERROR")
                    }.then { p ->
                        Blocking.get {
                            def result = cleanGameTable()
                            p.stream().map { q ->
                                insertGame(q)
                            }.collect(Collectors.joining(System.lineSeparator(), result + System.lineSeparator(), ""))
                        }.onError {
                          logger.info("ERROR")
                        }.then { result ->
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            render result
                        }
                    }
                }

                post {
                    parse(listOf(Game.class)).onError {
                        e -> render e.toString()
                    }.onError {
                      logger.info("ERROR")
                    }.then { p ->
                        Blocking.get {
                            GamesPostResponse newGameIds = new GamesPostResponse()
                            p.stream().map { g -> insertGame(g) }.forEach{r -> newGameIds.add(r)}
                            newGameIds
                        }.onError {
                          logger.info("ERROR")
                        }.then { newGameIds ->
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            render json(newGameIds)
                        }
                    }
                }

                delete {
                    Blocking.get {
                        cleanGameTable()
                    }.onError {
                      logger.info("ERROR")
                    }.then { result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render result
                    }
                }
            }
        }
    }

    private GroovyRowResult getGame(String id) {
        DbUtil.query("SELECT * FROM tbl_fights WHERE id = '" + id + "'").first()
    }

    private String overwriteGame(Game game, String id) {
        "overwriteGame: " + id + ", result: " + DbUtil.execute("REPLACE INTO tbl_fights (id, player_red_1, player_red_2, player_blue_1, player_blue_2, timestamp, match_winner, points_at_steake, winning_table) VALUES ('" + id + "', '" + game.getPlayer_red_1() + "', '" + game.getPlayer_red_2() + "', '" + game.getPlayer_blue_1() + "', '" + game.getPlayer_blue_2() + "', '" + game.getLastUpdated() + "', '" + game.getMatch_winner() + "', '" + game.getPoints_at_stake() + "', '" + game.getWinning_table() + "')")
    }

    private String deleteGame(String gameId) {
        "deleteGame: " + gameId + ", result: " + DbUtil.execute("DELETE FROM tbl_fights where id = '" + gameId + "'")
    }

    private JsonRender getAllGames() {
        json(DbUtil.query("SELECT * FROM tbl_fights order by ID desc").collect { row -> new Game(row) })
    }

    private JsonRender getGamesForThisManyHoursBackInTime(String hoursToGoBackInTime) {
        json(DbUtil.query("SELECT * FROM tbl_fights WHERE timestamp > DATE_SUB(NOW(), INTERVAL " + hoursToGoBackInTime +  " HOUR) order by ID desc").collect { row -> new Game(row) })
    }

    private JsonRender getGamesForName(String name) {
        json(DbUtil.query("SELECT * FROM tbl_fights WHERE player_red_1 = '" + name + "' || player_red_2 = '" + name + "' || player_blue_1 = '" + name + "' || player_blue_2 = '" + name + "' order by ID desc LIMIT 10").collect { row -> new Game(row) })
    }

    private String cleanGameTable() {
        "cleanGameTable: " + DbUtil.execute("Truncate table tbl_fights")
    }

    private String insertGame(Game game) {
        DbUtil.execute("INSERT INTO tbl_fights (player_red_1, player_red_2, player_blue_1, player_blue_2, timestamp, match_winner, points_at_steake, winning_table) VALUES ('" + game.getPlayer_red_1() + "', '" + game.getPlayer_red_2() + "', '" + game.getPlayer_blue_1() + "', '" + game.getPlayer_blue_2() + "', '" + game.getLastUpdated() + "', '" + game.getMatch_winner() + "', '" + game.getPoints_at_stake() + "', '" + game.getWinning_table() + "')")
    }

}
