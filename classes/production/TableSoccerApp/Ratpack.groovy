import ratpack.groovy.sql.SqlModule
import ratpack.groovy.template.MarkupTemplateModule

import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack
import static groovy.json.JsonOutput.toJson

class DatabaseConfig {
    String host = "localhost"
    String user = "root"
    String password
    String db = "myDB"
}

ratpack {
    bindings {
        module MarkupTemplateModule
        add(new Players("Hello there, "))
    }

    handlers {
        get {
            render groovyMarkupTemplate("index.gtpl", title: "My Ratpack App")
        }

        get("config") { DatabaseConfig config ->
            render toJson(config)
        }

        prefix("players") {
            all chain(registry.get(Players))
        }

        files { dir "public" }
    }
}
