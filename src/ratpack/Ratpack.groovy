import ratpack.groovy.sql.SqlModule
import ratpack.groovy.template.MarkupTemplateModule

import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack
import static groovy.json.JsonOutput.toJson

ratpack {
    bindings {
        module MarkupTemplateModule
        add(new Players("Hello there, "))
    }

    handlers {
        get {
            render groovyMarkupTemplate("index.gtpl", title: "My Ratpack App")
        }

        prefix("players") {
            all chain(registry.get(Players))
        }

        files { dir "public" }
    }
}
