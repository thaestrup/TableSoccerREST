import com.fasterxml.jackson.annotation.JsonProperty
import groovy.json.JsonBuilder
import groovy.sql.GroovyRowResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.exec.Blocking
import ratpack.groovy.handling.GroovyChainAction
import groovy.sql.Sql

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Created by super on 04/10/2016.
 */
class Players extends GroovyChainAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(Players.class);

    public static class Player {
        private final int id;
        private final String name;
        private final boolean playerReady;
        private final LocalDateTime oprettet;

        public Player(@JsonProperty("id") String id,
                      @JsonProperty("name") String name,
                      @JsonProperty("playerReady") String playerReady,
                      @JsonProperty("oprettet") String oprettet) {
            this.id = Integer.valueOf(id)
            this.name = name;
            this.playerReady = Boolean.valueOf(playerReady);

            //2015-08-03T11:47:47+0000
            DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
            this.oprettet = LocalDateTime.parse(oprettet, pattern);
        }

        int getId() {
            return id
        }

        String getName() {
            return name
        }

        boolean getPlayerReady() {
            return playerReady
        }

        LocalDateTime getOprettet() {
            return oprettet
        }
    }

    @Override
    void execute() throws Exception {

        path(":id") {
            byMethod {
                get {
                    Blocking.exec { ->
                        def sql = Sql.newInstance(DbUtil.url, DbUtil.user, DbUtil.password, DbUtil.driver)
                        //TODO only return one object, not an array of json objects
                        render sql.rows("SELECT * FROM tbl_players WHERE id = '" + pathTokens["id"] + "'")
                                .collect { row -> new JsonBuilder(row).toPrettyString() }.toString()
                        sql.close()
                    }
                }

                put {
                    //TODO Check for multiple instances and only use the first instance.
                    Blocking.get{parse(Player.class).map{ p -> overwritePlayer(p) }}.then{i -> render i}
                }
            }
        }

        get {
            Blocking.exec { ->
                def sql = Sql.newInstance(DbUtil.url, DbUtil.user, DbUtil.password, DbUtil.driver)
                render sql.rows("SELECT * FROM tbl_players")
                        .collect { row -> new JsonBuilder(row).toPrettyString() }.toString()
                sql.close()
            }
        }
    }

    private String overwritePlayer(Player p) {
        def sql = Sql.newInstance(DbUtil.url, DbUtil.user, DbUtil.password, DbUtil.driver)
        sql.execute("REPLACE INTO tbl_players VALUES (" + p.getId() + ", '" + p.getName() + "', " + (p.getPlayerReady() ? 1 : 0) + ", '" + p.getOprettet() + "')")
        sql.close()
        return "OK"
    }
}
