import groovy.sql.Sql;

import java.sql.SQLException;
import java.util.List;

import groovy.sql.GroovyRowResult;
import groovy.util.logging.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by super on 30/10/2016.
 */
public class DbUtil {
    public static final String url = "jdbc:mysql://localhost:3306/NykreditFoosballUnity?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=CET&verifyServerCertificate=false&useSSL=false";
    public static final String user = "root";
    public static final String password = "";
    public static final String driver = "com.mysql.cj.jdbc.Driver";
    //static final Logger logger = LoggerFactory.getLogger(DbUtil.class);

    public static String execute(String query) {
        Sql sql = null;
        String resultID = "ERROR";
        try {
            //logger.info("In try method");
            sql = Sql.newInstance(url, user, password, driver);
            sql.execute(query);
            resultID = sql.rows("SELECT LAST_INSERT_ID();").get(0).get("LAST_INSERT_ID()").toString();
            sql.close();
        } catch (ClassNotFoundException e) {
            //logger.info("In catch for ClassNotFoundException");
            return e.toString();
        } catch (SQLException e) {
            //logger.info("In catch for SQLException");
            return e.toString();
        } finally {
            //logger.info("In finally");
            if (sql != null) {
                sql.close();
            }
        }
        return resultID;
    }

//    public static String preparedStatement(String query, String[] keys) {
//        Sql sql = null;
//        try {
//            sql = Sql.newInstance(url, user, password, driver);
//            sql.execute(query);
//            sql.close();
//        } catch (ClassNotFoundException e) {
//            return e.toString();
//        } catch (SQLException e) {
//            return e.toString();
//        } finally {
//            if (sql != null) {
//                sql.close();
//            }
//        }
//
//        return "OK";
//    }

    public static List<GroovyRowResult> query(String query) throws SQLException, ClassNotFoundException {
        //Logger logger = LoggerFactory.getLogger(DbUtil.class);
        //logger.info("In query user:{} pass:{} url:{}", user, password, url);
        Sql sql = Sql.newInstance(url, user, password, driver);
        try {
            //logger.info("In try in query");
            return sql.rows(query);
        } finally {
            //logger.info("In finally in  query");
            sql.close();
        }
    }
}
