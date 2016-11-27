import groovy.json.JsonSlurper
import ratpack.groovy.handling.GroovyChainAction

import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat

import static ratpack.jackson.Jackson.json

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
//                    def url = 'http://api.icndb.com/jokes/random'
//                    def json = new JsonSlurper().parseText(url.toURL().text)
//                    def joke = json?.value?.joke
//                    println joke

                    def url = 'http://localhost:5050/games'
                    def response = new JsonSlurper().parseText(url.toURL().text)
                    Map<String, Timestamp> players = new HashMap<>()
                    response.each {game ->
                        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                        Date d = formatter.parse(game.getAt("lastUpdated"));
                        Timestamp ts = new Timestamp(d.getTime());

                        def replaceIfBigger = { String name, Timestamp timestamp -> timestamp.after(ts) ? timestamp : ts }
                        players.computeIfPresent(game.getAt("player_blue_1"), replaceIfBigger)
                        players.computeIfPresent(game.getAt("player_blue_2"), replaceIfBigger)
                        players.computeIfPresent(game.getAt("player_red_1"), replaceIfBigger)
                        players.computeIfPresent(game.getAt("player_red_2"), replaceIfBigger)

                        players.putIfAbsent(game.getAt("player_blue_1"), ts)
                        players.putIfAbsent(game.getAt("player_blue_2"), ts)
                        players.putIfAbsent(game.getAt("player_red_1"), ts)
                        players.putIfAbsent(game.getAt("player_red_2"), ts)
                    }
                    render json(players)
                }
            }
        }
    }


}
