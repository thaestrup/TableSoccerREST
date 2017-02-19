import Model.ConfigurationItem
import Model.Player
import Model.Game
import Model.TimerAction
import groovy.sql.GroovyRowResult;
import groovy.sql.Sql;

import java.sql.SQLException;
import java.util.List;

import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by super on 30/10/2016.
 */
public class MoreUtil {

  public static Map<String, String> playersLastPlayed() {
    //Logger logger = LoggerFactory.getLogger(MoreUtil.class);
    //logger.info("In playersLastPlayed");
    List<GroovyRowResult> allPlayers = getAllPlayers();
    HashMap hashMap = new HashMap<>()
    allPlayers.each {
      //logger.info("name: " + it["name"])
      List<GroovyRowResult> lines = getNewestMatchForPlayer(it["name"])
      if (lines.isEmpty()) {
        //logger.info("lines was null")
      } else {
        //logger.info("timestamp: " + lines[0]["timestamp"])
        hashMap.put(it["name"], lines[0]["timestamp"])
      }
    }
    return hashMap;
  }

  private static ensureConfigurationTableExist() {
      DbUtil.execute("CREATE TABLE IF NOT EXISTS `tbl_configuration`(  `name` VARCHAR(255) NOT NULL UNIQUE,   `value` VARCHAR(255) NOT NULL  )")
  }

  private static ensureTimerTableExist() {
      DbUtil.execute("CREATE TABLE IF NOT EXISTS `tbl_timer`(  `id` int(11) UNIQUE NOT NULL, `lastRequestedTimerStart` timestamp NOT NULL )")
      DbUtil.execute("INSERT IGNORE INTO `tbl_timer` (id, lastRequestedTimerStart) values (1, NOW())");
  }

  private static List<TimerAction> getTimerActions() {
      DbUtil.query("SELECT * FROM tbl_timer").collect { row -> new TimerAction(row) }
  }

  private static List<ConfigurationItem> getAllConfigurationItems() {
      DbUtil.query("SELECT * FROM tbl_configuration").collect { row -> new ConfigurationItem(row) }
  }

  private static List<Game> getAllGames() {
      DbUtil.query("SELECT * FROM tbl_fights order by ID desc").collect { row -> new Game(row) }
  }

  public static List<Game> getGamesForThisManyHoursBackInTime(String hoursToGoBackInTime, String filter) {
    if (filter == "onlylunch") {
        DbUtil.query("SELECT * FROM tbl_fights WHERE (time(timestamp) >= '11:27:00' AND time(timestamp) <= '12:33:59') AND  timestamp > DATE_SUB(NOW(), INTERVAL " + hoursToGoBackInTime +  " HOUR) order by ID desc").collect { row -> new Game(row) }
    } else if (filter == "elo") {
      DbUtil.query("SELECT * FROM tbl_fights WHERE timestamp > DATE_SUB(NOW(), INTERVAL " + hoursToGoBackInTime +  " HOUR) order by ID asc").collect { row -> new Game(row) }

    } else {
      DbUtil.query("SELECT * FROM tbl_fights WHERE timestamp > DATE_SUB(NOW(), INTERVAL " + hoursToGoBackInTime +  " HOUR) order by ID desc").collect { row -> new Game(row) }
    }
  }

  private static List<GroovyRowResult> getAllPlayers() {
      DbUtil.query("SELECT * FROM tbl_players").collect { row -> new Player(row) }
  }
  private static List<GroovyRowResult> getNewestMatchForPlayer(String player) {
      //Logger logger = LoggerFactory.getLogger(MoreUtil.class);
      String sql = "SELECT * FROM tbl_fights WHERE player_red_1 = '" + player + "' OR  player_red_2 = '" + player + "' OR player_blue_1 = '" + player + "' OR player_blue_2 = '" + player + "' ORDER by timestamp desc LIMIT 1"
      //logger.info("sql: " + sql)
      return DbUtil.query(sql)
  }
}
