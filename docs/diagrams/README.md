Diagrams
========

PlantUML sources for the C4 model and the domain class diagram.
Render via the PlantUML CLI, the IntelliJ / VS Code PlantUML plugins, or
any of the online PlantUML renderers.

| File | Level | Audience |
|---|---|---|
| `c4-container.puml` | C4 L2 — Container | Architects / ops; "what runs where" |
| `c4-component.puml` | C4 L3 — Component | Backend devs; resources, services, mappers |
| `class-model.puml`  | UML class diagram | Backend devs; entities + DTOs + mappers |

Render
------

Each `.puml` file is self-contained and pulls the C4-PlantUML stdlib over
HTTPS at render time. To render locally:

```
podman run --rm -v "$PWD":/work plantuml/plantuml -tpng /work/c4-container.puml
```

or use the PlantUML jar:

```
plantuml -tpng c4-container.puml c4-component.puml class-model.puml
```

PNGs land next to the `.puml` files. SVG (`-tsvg`) is also fine.

Why these three
---------------

C4 has four levels (Context, Container, Component, Code). System-Context
is omitted because the FoosballUnity stack has no meaningful third-party
dependencies — players and the organizer talk to a frontend that talks to
a backend that talks to MariaDB, all inside one compose stack. The
Container diagram captures that without redundancy.

The Code level is rendered as a UML class diagram (`class-model.puml`)
since C4 itself doesn't standardize a code-level notation.

Keeping in sync
---------------

These diagrams describe the **target Quarkus port**, not the legacy
Ratpack/Groovy implementation. Update them when:

- a new resource lands (component diagram)
- a new entity / DTO / mapper lands (class diagram)
- the deployment topology changes — e.g. WebSocket service split out,
  Redis cache added, etc. (container diagram)

The status table in `../../../MIGRATION-PLAN.md` tracks which Phase 2
slices are done; reflect each slice's components/classes here as it lands.
