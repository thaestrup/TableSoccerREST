import groovy.json.JsonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.groovy.handling.GroovyChainAction
import groovy.sql.Sql

import java.util.stream.Collectors

/**
 * Created by super on 04/10/2016.
 */
class Players extends GroovyChainAction {
    private final static Logger LOGGER = LoggerFactory.getLogger(Players.class);
    private String prefix;

    Players(String prefix) {
        this.prefix = prefix
    }

    @Override
    void execute() throws Exception {

        path(":name") {
            def sql = Sql.newInstance(DbUtil.url, DbUtil.user, DbUtil.password, DbUtil.driver)
            render sql.rows("SELECT * FROM tbl_players WHERE name = '" + pathTokens["name"] + "'")
                    .collect{row -> new JsonBuilder(row).toPrettyString() }.toString()
            sql.close()
        }

        all {
            def sql = Sql.newInstance(DbUtil.url, DbUtil.user, DbUtil.password, DbUtil.driver)
            render sql.rows("SELECT * FROM tbl_players")
                    .collect{row -> new JsonBuilder(row).toPrettyString() }.toString()
            sql.close()
        }

    }
}
