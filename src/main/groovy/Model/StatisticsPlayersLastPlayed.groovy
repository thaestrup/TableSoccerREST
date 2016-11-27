package Model

import groovy.sql.GroovyRowResult
import ratpack.exec.Blocking
import ratpack.groovy.handling.GroovyChainAction
import ratpack.jackson.JsonRender

import java.util.stream.Collectors

import static ratpack.jackson.Jackson.json
import static ratpack.util.Types.listOf

/**
 * Created by super on 04/10/2016.
 */
class StatisticsPlayersLastPlayed extends GroovyChainAction {

    @Override
    void execute() {
        all {
            byMethod {

                options {
                    response.headers.set('Access-Control-Allow-Methods:', 'GET')
                    response.headers.set('Access-Control-Allow-Origin', '*')
                    render "OK"
                }

                get {
                    Blocking.get {


                    }.then { result ->
                        response.headers.set('Access-Control-Allow-Origin', '*')
                        render result
                    }

                }
            }
        }
    }


}
