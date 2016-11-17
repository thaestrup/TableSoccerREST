import com.fasterxml.jackson.annotation.JsonProperty
import groovy.sql.GroovyRowResult
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
    void execute() throws Exception {

        path(":name") {
            byMethod {
                get {
                    Blocking.get { ->
                        DbUtil.query("SELECT * FROM tbl_players WHERE name = '" + pathTokens["name"] + "'")
                                .first()
                    }.then { row -> render json(new Player(row)) }
                }

                put {
                    //TODO Check for multiple instances and only use the first instance: Just need error handling now
                    parse(Player.class).then { p -> Blocking.exec { overwritePlayer(p, pathTokens["name"]) } }
                    render "OK"
                }

                delete {
                    delete(pathTokens["name"]);
                    render "OK"
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
                    truncateTable();
                    parse(listOf(Player.class)).then { p ->
                        Blocking.exec {
                            p.stream().forEach { q -> insertPlayer(q) }
                        }
                    }
                    render "OK"
                }

                post {
                    //TODO Error handling
                    parse(listOf(Player.class)).then { p ->
                        Blocking.exec {
                            p.stream().forEach { q -> insertPlayer(q) }
                        }
                    }
                    render "OK"
                }

                delete {
                    truncateTable()
                    render "OK"
                }
            }
        }
    }

    private String overwritePlayer(Player p, String name) {
        DbUtil.execute("REPLACE INTO tbl_players (name, playerReady, oprettet) VALUES ('" + name + "', " + (p.getPlayerReady() ? 1 : 0) + ", '" + p.getOprettet() + "')")
        return "OK"
    }

    private String insertPlayer(Player p) {
        DbUtil.execute("INSERT INTO tbl_players (name, playerReady, oprettet) VALUES ('" + p.getName() + "', " + (p.getPlayerReady() ? 1 : 0) + ", '" + p.getOprettet() + "')")
        return "OK"
    }

    private String truncateTable() {
        DbUtil.execute("Truncate table tbl_players")
        return "OK"
    }

    private String delete(String name) {
        DbUtil.execute("DELETE FROM tbl_players where name = '" + name + "'")
        return "OK"
    }
}
