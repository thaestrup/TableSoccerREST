import Model.TimerAction

import groovy.json.JsonSlurper
import ratpack.groovy.handling.GroovyChainAction
import ratpack.exec.Blocking

import java.util.stream.Collectors

import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat

import static ratpack.jackson.Jackson.json
import static ratpack.util.Types.listOf;

import groovy.util.logging.Slf4j
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by NIKL
 */
class TimerActions extends GroovyChainAction {

    @Override
    void execute() {
    Logger logger = LoggerFactory.getLogger(Timer.class);
    logger.info("In execute in Timer")
        all {
            byMethod {

                options {
                    response.headers.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    response.headers.set('Access-Control-Allow-Headers', 'x-requested-with, origin, content-type, accept')
                    render "OK"
                }

                get {
                    // First check for table (or create if it doesn't exist)
                    MoreUtil.ensureTimerTableExist();

                    // Then get values
                    List<TimerAction> timerActions = MoreUtil.getTimerActions();
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render json(timerActions)
                }

                post {

                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render updateTimerAction()

                    /*parse(listOf(TimerAction.class)).onError {
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.get {
                            p.stream().map { q ->
                                updateTimerAction(q)
                            }.collect(Collectors.joining(System.lineSeparator()))
                        }.then { result ->
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            render result
                        }
                    }*/

                }
            }
        }
    }

    /*private String updateTimerAction(TimerAction ta) {
        "timerAction: " + ta.getId() + ", result: " + DbUtil.execute("UPDATE tbl_timer SET lastRequestedTimerStart = NOW() WHERE id = 1")
    }*/
    private String updateTimerAction() {
        "result: " + DbUtil.execute("UPDATE tbl_timer SET lastRequestedTimerStart = NOW() WHERE id = 1")
    }
}
