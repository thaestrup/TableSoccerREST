import groovy.json.JsonSlurper
import ratpack.groovy.handling.GroovyChainAction

import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat

import static ratpack.jackson.Jackson.json

import groovy.util.logging.Slf4j
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by super on 04/10/2016.
 */
class StatisticsPlayersLastPlayed extends GroovyChainAction {

    @Override
    void execute() {
    //Logger logger = LoggerFactory.getLogger(StatisticsPlayersLastPlayed.class);
    //logger.info("In execute in StatisticsPlayerLastPlayed")
        all {
            byMethod {

                options {
                    response.headers.set('Access-Control-Allow-Methods', 'GET, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    response.headers.set('Access-Control-Allow-Headers', 'x-requested-with, origin, content-type, accept')
                    render "OK"
                }

                get {
                    Map<String, Long> players = MoreUtil.playersLastPlayed();
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render json(players)
                }
            }
        }
    }
}
