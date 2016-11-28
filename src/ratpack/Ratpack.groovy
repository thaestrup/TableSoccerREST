import ratpack.groovy.template.MarkupTemplateModule
import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
    bindings {
        module MarkupTemplateModule
        add(new Players())
        add(new Games())
        add(new StatisticsPlayersLastPlayed())
        add(new RandomTournament())
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

        prefix("tournament") {
            prefix("randomTournament") {
                all chain(registry.get(RandomTournament))
            }
        }

        prefix("api") {
            all {
                render groovyMarkupTemplate("api.gtpl")
            }
        }

        files { dir "public" }
    }
}
