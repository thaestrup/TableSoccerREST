import Model.Player
import Model.Game
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

  private static List<Game> getAllGames() {
      DbUtil.query("SELECT * FROM tbl_fights order by ID desc").collect { row -> new Game(row) }
  }

  private static List<Game> getGamesForThisManyHoursBackInTime(String hoursToGoBackInTime) {
      DbUtil.query("SELECT * FROM tbl_fights WHERE timestamp > DATE_SUB(NOW(), INTERVAL " + hoursToGoBackInTime +  " HOUR) order by ID desc").collect { row -> new Game(row) }
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
