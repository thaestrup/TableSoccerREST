import Model.ConfigurationItem

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
 * Created by NIKL on 2017-01-15
 */
class Configuration extends GroovyChainAction {

    @Override
    void execute() {
    Logger logger = LoggerFactory.getLogger(Configuration.class);
    logger.info("In execute in Configuration")
        all {
            byMethod {

                options {
                    response.headers.set('Access-Control-Allow-Methods', 'GET, OPTIONS')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    response.headers.set('Access-Control-Allow-Headers', 'x-requested-with, origin, content-type, accept')
                    render "OK"
                }

                get {
                    // First check for table (or create if it doesn't exist)
                    MoreUtil.ensureConfigurationTableExist();

                    // Then get values
                    List<ConfigurationItem> configurationItems = MoreUtil.getAllConfigurationItems();
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render json(configurationItems)
                }
            }
        }
    }
}
