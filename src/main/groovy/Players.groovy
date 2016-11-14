import com.fasterxml.jackson.annotation.JsonProperty
import groovy.json.JsonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.exec.Blocking
import ratpack.func.Function
import ratpack.groovy.handling.GroovyChainAction
import groovy.sql.Sql
import ratpack.handling.Context
import ratpack.http.Request

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import static ratpack.jackson.Jackson.jsonNode
import static ratpack.jackson.Jackson.fromJson;

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

        path(":name") {
            byMethod {
                get {
                    Blocking.exec { ->
                        def sql = Sql.newInstance(DbUtil.url, DbUtil.user, DbUtil.password, DbUtil.driver)
                        render sql.rows("SELECT * FROM tbl_players WHERE name = '" + pathTokens["name"] + "'")
                                .collect { row -> new JsonBuilder(row).toPrettyString() }.toString()
                        sql.close()
                    }
                }

                put {
                    Blocking.get { -> parse(Player.class).map { p -> testing2(p) } }
                            .then { i -> render i }
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

    //This method shoudl http://dev.mysql.com/doc/refman/5.7/en/replace.html
    private String testing2(Player p) {
        System.out.println(p.getName())

        def sql = Sql.newInstance(DbUtil.url, DbUtil.user, DbUtil.password, DbUtil.driver)
        def string = sql.rows("SELECT * FROM tbl_players WHERE name = '" + p.getName() + "'").collect { row -> new JsonBuilder(row).toPrettyString() }.toString()
        sql.close()

        return string
    }
}
