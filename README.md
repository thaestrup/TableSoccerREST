TableSoccerREST (Quarkus port)
==============================

Backend REST API for the foosball / table soccer tournament organizer.
Quarkus 3.35 / Java 21 / MariaDB / Hibernate-ORM-Panache / Flyway.

Replaces an earlier Ratpack 1.6 / Groovy 2.5 implementation. The migration
plan, scope cuts, and status live at `../MIGRATION-PLAN.md`. Frontend usage
audit at `../FRONTEND-USAGE.md`.

The frontend lives in the sibling repo:

```
git clone https://github.com/nlkolbe/FoosballUnity.git ../FoosballUnity
```

That repo also hosts `docker-compose.yml` orchestrating the full stack
(frontend + this backend + MariaDB).

Layout
------

```
TableSoccerREST/
├── pom.xml                              Quarkus 3.35.2, Java 21
├── mvnw, .mvn/                          Maven wrapper
├── src/main/
│   ├── java/com/foosball/
│   │   ├── api/                         JAX-RS resources (PlayersResource, …)
│   │   ├── domain/                      JPA entities (Panache active record)
│   │   ├── dto/                         Wire shapes (records) + mappers
│   │   ├── service/                     Business logic (tournaments, Elo)
│   │   └── config/                      Health checks, etc.
│   ├── resources/
│   │   ├── application.properties       port 5051, MariaDB, CORS, Flyway, OpenAPI
│   │   └── db/migration/                V1 schema, V2 indices, V3 dev seed
│   └── docker/
│       ├── Dockerfile.jvm               temurin:21 multi-stage (~310 MB)
│       └── Dockerfile.native            Mandrel multi-stage → micro-image (~50 MB)
└── src/test/java/com/foosball/
    ├── api/                             @QuarkusTest smoke tests
    └── contract/                        RestAssured parity suite (legacy ↔ Quarkus)
```

Run as part of the full stack
-----------------------------

```
cd ../FoosballUnity
podman compose up -d --build           # builds the JVM image; first run ~2-3 min
```

Backend reachable at `http://localhost:5051`.

To force the (smaller, slower-to-build) native image instead, point the compose
`backend` service's `dockerfile:` at `src/main/docker/Dockerfile.native`.
That build needs ~6 GB RAM for Mandrel — first build ~5 minutes.

Run from the published image
----------------------------

Tagged releases (`vX.Y.Z`) are built and published to GitHub Container
Registry by `.github/workflows/docker-publish.yml`. The JVM image is at:

```
ghcr.io/thaestrup/tablesoccerrest:latest      # most recent release
ghcr.io/thaestrup/tablesoccerrest:1.0.0       # specific version
```

Pull and run against an external MariaDB:

```
podman run --rm -p 5051:5051 \
    -e DB_URL='jdbc:mariadb://<host>:3306/nykreditfoosballunity?useSsl=false' \
    -e DB_USER=football \
    -e DB_PASSWORD=football \
    -e ALLOWED_ORIGINS='https://foosball.example.com' \
    ghcr.io/thaestrup/tablesoccerrest:latest
```

Flyway migrates the schema on startup, so the DB only needs to exist and be
reachable — empty is fine. The container exposes `5051`; health endpoints
`/q/health/ready` and `/q/health/live` are suitable for orchestrator probes.

If the package is private (GHCR's default for new packages), authenticate
first with a personal access token that has the `read:packages` scope:

```
echo $GHCR_TOKEN | podman login ghcr.io -u <github-username> --password-stdin
```

Cutting a release: tag the commit and push the tag — the workflow does the
rest.

```
git tag v1.0.0 && git push origin v1.0.0
```

Run standalone (dev mode)
-------------------------

```
./mvnw quarkus:dev
```

Quarkus Dev Services starts a local MariaDB container automatically (requires
the Podman compatibility socket: `systemctl --user enable --now podman.socket`
and `export DOCKER_HOST=unix:///run/user/1000/podman/podman.sock`). Hot reload
on every file save. Dev UI at `http://localhost:5051/q/dev/`. OpenAPI and
Swagger UI paths are documented under "API documentation" below.

To run against an external MariaDB instead of Dev Services, set:

```
export DB_URL=jdbc:mariadb://localhost:3306/nykreditfoosballunity?useSsl=false
export DB_USER=football
export DB_PASSWORD=football
./mvnw quarkus:dev -Dquarkus.datasource.devservices.enabled=false
```

Tests
-----

Two suites:

```
./mvnw test                                       # @QuarkusTest smoke (needs Dev Services)
./mvnw test -Dtest='*ContractTest' \
            -Dcontract.baseUrl=http://localhost:5050    # parity suite vs. legacy or new
```

The contract suite is hostable: change `-Dcontract.baseUrl` to point at
either backend (legacy on 5050 if running, new on 5051). Identical pass/fail
is the migration's parity gate.

Endpoints
---------

JAX-RS routing under `src/main/java/com/foosball/api/`. Surface is a subset
of the legacy backend — see `../FRONTEND-USAGE.md` for the kept/dropped
matrix.

- `GET/POST /players`, `GET/PUT /players/{name}`
- `GET /games`, `GET /games/{periodOrName}`, `POST /games`, `DELETE /games`
- `GET /configuration`
- `GET /statisticsPlayersLastPlayed`
- `GET /timer`
- `POST /tournament/{randomTournament|lastFirstTournament|awesomeAlgorithmTournament}`
- `GET /pointsPrPlayer/{period}`
- `GET /q/health`, `GET /q/health/ready`

API documentation
-----------------

OpenAPI 3 spec and Swagger UI are wired up out-of-the-box via
`quarkus-smallrye-openapi` and `quarkus-swagger-ui`. Both are auto-derived
from the JAX-RS annotations on each resource — no maintenance overhead.

- **`http://localhost:5051/openapi`** — OpenAPI 3 spec in YAML.
  Append `?format=json` for JSON.
- **`http://localhost:5051/swagger-ui`** — interactive Swagger UI.
  Send requests, inspect schemas, browse the surface.

Both paths are configured in `src/main/resources/application.properties`
(`quarkus.smallrye-openapi.path`, `quarkus.swagger-ui.path`). Swagger UI
is included in production builds (`quarkus.swagger-ui.always-include=true`)
because this service runs on a private network — no risk of leaking the
admin interface. Set this to `false` and gate by profile if that ever
changes.

To enrich the generated spec (descriptions, response examples, tag
groupings), add MicroProfile OpenAPI annotations on resource methods:

```java
@GET
@Path("/{name}")
@Operation(summary = "Look up a player by name")
@APIResponse(responseCode = "200", description = "Player found")
@APIResponse(responseCode = "404", description = "Unknown player name")
public PlayerDto getByName(@PathParam("name") String name) { ... }
```

Database
--------

Configured via env (`DB_URL`, `DB_USER`, `DB_PASSWORD`) — defaults in
`src/main/resources/application.properties`. Flyway migrates on startup;
schema lives in `src/main/resources/db/migration/`. The legacy `db-init/`
SQL scripts are gone — Flyway owns DDL.
