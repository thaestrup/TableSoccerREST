import LastFirstTournament
import ratpack.groovy.template.MarkupTemplateModule
import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
    bindings {
        module MarkupTemplateModule
        add(new Players())
        add(new Games())
        add(new Configuration())
        add(new StatisticsPlayersLastPlayed())
        add(new RandomTournament())
        add(new LastFirstTournament())
        add(new AwesomeAlgorithmTournament())
        add(new PointsPrPlayer())
        add(new TimerActions())
        add(new Registrations())
    }

    handlers {
        get {
            render groovyMarkupTemplate("index.gtpl", title: "My Ratpack App")
        }

        prefix("players") {
            all chain(registry.get(Players))
        }

        prefix("games") {
            all chain(registry.get(Games))
        }

        prefix("statisticsPlayersLastPlayed") {
            all chain(registry.get(StatisticsPlayersLastPlayed))
        }

        prefix("configuration") {
            all chain(registry.get(Configuration))
        }

        prefix("timer") {
            all chain(registry.get(TimerActions))
        }

        prefix("registration") {
            all chain(registry.get(Registrations))
        }

        prefix("tournament") {
            prefix("randomTournament") {
                all chain(registry.get(RandomTournament))
            }

            prefix("lastFirstTournament") {
                all chain(registry.get(LastFirstTournament))
            }

            prefix("awesomeAlgorithmTournament") {
                all chain(registry.get(AwesomeAlgorithmTournament))
            }
        }

        prefix("pointsPrPlayer") {
            all chain(registry.get(PointsPrPlayer))
        }

        prefix("api") {
            all {
                render groovyMarkupTemplate("api.gtpl")
            }
        }

        files { dir "public" }
    }
}
