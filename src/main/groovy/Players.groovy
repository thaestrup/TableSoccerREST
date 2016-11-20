import com.fasterxml.jackson.annotation.JsonProperty
import groovy.sql.GroovyRowResult
import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultProductionErrorHandler
import ratpack.exec.Blocking
import ratpack.groovy.handling.GroovyChainAction
import static ratpack.util.Types.listOf;
import static ratpack.jackson.Jackson.json;

/**
 * Created by super on 04/10/2016.
 */
class Players extends GroovyChainAction {

    public static class Player {
        private final String name;
        private final boolean playerReady;
        private final String oprettet;

        public Player(@JsonProperty("name") String name,
                      @JsonProperty("playerReady") String playerReady,
                      @JsonProperty("oprettet") String oprettet) {
            this.name = name;
            this.playerReady = Boolean.valueOf(playerReady);
            this.oprettet = oprettet;
        }

        public Player(GroovyRowResult row) {
            if (row.containsKey("name")) {
                this.name = row.getProperty("name");
            }
            this.playerReady = row.getProperty("playerReady");
            this.oprettet = row.getProperty("oprettet");
        }

        String getName() {
            return name
        }

        boolean getPlayerReady() {
            return playerReady
        }

        String getOprettet() {
            return oprettet
        }
    }

    @Override
    void execute() {
        path(":name") {
            byMethod {
                get {
                    Blocking.get { ->
                        DbUtil.query("SELECT * FROM tbl_players WHERE name = '" + pathTokens["name"] + "'")
                                .first()
                    }.then { row -> render json(new Player(row)) }
                }

                put {
                    parse(Player.class).onError{
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.exec {
                            render DbUtil.execute("REPLACE INTO tbl_players (name, playerReady, oprettet) VALUES ('" + pathTokens["name"] + "', " + (p.getPlayerReady() ? 1 : 0) + ", '" + p.getOprettet() + "')")
                        }
                    }
                }

                delete {
                    render DbUtil.execute("DELETE FROM tbl_players where name = '" + pathTokens["name"] + "'")
                }
            }
        }

        all {
            byMethod {
                get {
                    Blocking.exec { ->
                        render json(DbUtil.query("SELECT * FROM tbl_players").collect { row -> new Player(row) })
                    }
                }

                put {
                    parse(listOf(Player.class)).onError{
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.exec {
                            DbUtil.execute("Truncate table tbl_players")
                            p.stream().forEach { q ->
                                render DbUtil.execute("INSERT INTO tbl_players (name, playerReady, oprettet) VALUES ('" + q.getName() + "', " + (q.getPlayerReady() ? 1 : 0) + ", '" + q.getOprettet() + "')")
                            }
                        }
                    }
                }

                post {
                    parse(listOf(Player.class)).onError{
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.exec {
                            p.stream().forEach { q ->
                                render DbUtil.execute("INSERT INTO tbl_players (name, playerReady, oprettet) VALUES ('" + q.getName() + "', " + (q.getPlayerReady() ? 1 : 0) + ", '" + q.getOprettet() + "')")
                            }
                        }
                    }
                }

                delete {
                    render DbUtil.execute("Truncate table tbl_players")
                }
            }
        }
    }
}
