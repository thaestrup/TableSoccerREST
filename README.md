TableSoccerREST
===============

Backend REST API for the foosball / table soccer tournament organizer. Built on Ratpack 1.6 (Groovy) running on the JVM. Talks to MariaDB.

The frontend lives in a sibling repo:

```
git clone https://github.com/nlkolbe/FoosballUnity.git ../FoosballUnity
```

That repo also hosts the `docker-compose.yml` that orchestrates the full stack (frontend + backend + database).

Layout
------

```
TableSoccerREST/
├── Dockerfile                  # standalone backend image (eclipse-temurin:11-jdk)
├── build.gradle                # Ratpack 1.6, mysql-connector 6.0.5
├── db-init/                    # MariaDB init scripts (mounted by compose)
│   ├── 00-create-user.sql      # creates `football` user with empty password
│   └── 01-schema.sql           # phpMyAdmin schema dump
├── src/
│   ├── main/groovy/            # handlers (Players, Games, Configuration, etc.)
│   │   └── Model/              # data classes
│   └── ratpack/
│       ├── Ratpack.groovy      # routing
│       ├── ratpack.properties
│       ├── templates/
│       └── public/             # built frontend assets (legacy — not the live frontend)
└── gradlew, gradle/            # gradle wrapper (5.4.1)
```

Run as part of the full stack
-----------------------------

This is the easy path. From the `FoosballUnity/` sibling repo:

```
cd ../FoosballUnity
podman compose up -d            # or: docker compose up -d
```

That starts MariaDB (with the `db-init/` scripts above), this backend, and the frontend. Backend is reachable at `http://localhost:5050`.

Run standalone
--------------

If you need to run just the backend (e.g. for API work without the frontend):

```
podman build -t foosball-backend .
podman run --rm --network host foosball-backend
```

Caveat: it expects a MariaDB at `localhost:3306` with database `nykreditfoosballunity` and user `football` (empty password). The `db-init/` scripts handle that for the compose setup; standalone you need to provide it yourself.

Dev loop
--------

The compose stack bind-mounts source into the backend container, so editing files in `src/` triggers Ratpack's reload. No rebuild needed for code changes; rebuild only when `Dockerfile` or dependencies change:

```
cd ../FoosballUnity
podman compose up -d --build backend
```

Endpoints
---------

Routing in `src/ratpack/Ratpack.groovy`. Top-level prefixes:

- `GET /` — index template
- `GET /api` — API listing template
- `players/`
- `games/`
- `configuration/`
- `statisticsPlayersLastPlayed/`
- `timer/`
- `registration/`
- `tournament/randomTournament/`, `tournament/lastFirstTournament/`, `tournament/awesomeAlgorithmTournament/`
- `pointsPrPlayer/`

Each prefix maps to a handler under `src/main/groovy/` of the same name.

Database
--------

Configured in `src/main/groovy/DbUtil.java`:

```
url:    jdbc:mysql://localhost:3306/nykreditfoosballunity
user:   football
password: (empty)
driver: com.mysql.cj.jdbc.Driver
```

The credentials are currently hardcoded as `public static final String`. See `../FINDINGS.md` for follow-ups.

`db-init/` is consumed by MariaDB's `/docker-entrypoint-initdb.d/` mechanism on first boot — files run in alphabetical order, so `00-create-user.sql` runs before `01-schema.sql`. Init only fires on a fresh data volume.
