import Model.Registration
import Model.Player

import groovy.sql.GroovyRowResult
import ratpack.jackson.JsonRender

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
class Registrations extends GroovyChainAction {

    @Override
    void execute() {
    Logger logger = LoggerFactory.getLogger(Registrations.class);
    logger.info("In execute in Registration")
        all {
            byMethod {

                options {
                    response.headers.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    response.headers.set('Access-Control-Allow-Headers', 'x-requested-with, origin, content-type, accept')
                    render "OK"
                }

                get {
                    Blocking.get { ->
                        getPlayer()
                    }.then { result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render json(result)
                    }
                }

                post {

                    response.headers.set('Access-Control-Allow-Origin', '*')
                    //render updateRegistration("ABC123")

                    parse(listOf(Registration.class)).onError {
                        e -> render e.toString()
                    }.then { p ->
                        Blocking.get {
                            p.stream().map { q ->
                                updateRegistration(q.RFIDTag)
                            }.collect(Collectors.joining(System.lineSeparator()))
                        }.then { result ->
                            response.headers.set('Access-Control-Allow-Origin', '*')
                            render result
                        }
                    }

                }
            }
        }
    }

    /*private String updateTimerAction(TimerAction ta) {
        "timerAction: " + ta.getId() + ", result: " + DbUtil.execute("UPDATE tbl_timer SET lastRequestedTimerStart = NOW() WHERE id = 1")
    }*/
    private String updateRegistration(String RFIDTag) {
        "result: " + MoreUtil.insertRFIDTag(RFIDTag)
    }

    private Player getPlayer() {

    Logger logger = LoggerFactory.getLogger(Registrations.class);
    logger.info("in getPlayer()")
      //return new Player("Name100", "1", "2011-11-11 11:11:11", "100100");

      // Do we have a registration row at all?
      List<GroovyRowResult> registrationRows = DbUtil.query("SELECT * FROM tbl_latest_rfid_registration")





      if (registrationRows.size() == 0) {
        // NO registrations - Return empty player objct (no name, no RFID)
        return new Player("", "0", "2011-11-11 11:11:11", "");
      } else {
        // We had a registration, let's see if we know it
        logger.info("We had an registration")
        // Ensure that there is no row, until next user registers
        DbUtil.execute("DELETE FROM tbl_latest_rfid_registration")

        String registeredRFIDTag = (registrationRows.first()).getProperty('registeredRFIDTag')
        logger.info("registeredRFIDTag" + registeredRFIDTag)
        List<GroovyRowResult> playerRow = DbUtil.query("SELECT * FROM tbl_players WHERE registeredRFIDTag = '" + registeredRFIDTag + "'" )
        if (playerRow.size > 0) {
          return new Player(playerRow.first());
        } else {
          return new Player("", "0", "2011-11-11 11:11:11", registeredRFIDTag);
        }

      }


    }
/*
    private GroovyRowResult getPlayer(String player) {
        DbUtil.query("SELECT * FROM tbl_players WHERE name = '" + player + "'")
                .first()
    }
    */
}
