import ratpack.groovy.template.MarkupTemplateModule
import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
    bindings {
        module MarkupTemplateModule
        add(new Players())
    }

    handlers {
        get {
            render groovyMarkupTemplate("index.gtpl", title: "My Ratpack App")
        }

        prefix("players") {
            all chain(registry.get(Players))
        }

        prefix("api") {
            all {
                render groovyMarkupTemplate("api.gtpl")
            }
        }

        files { dir "public" }
    }
}
