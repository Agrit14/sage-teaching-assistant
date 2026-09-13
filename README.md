# Sage — Backend

A Spring Boot backend with PostgreSQL, exposing a single endpoint that takes a
request and returns a response.

No Telegram, no bot, no external services. Something else calls this API.

## Stack

| Piece | Version |
|---|---|
| Java | 17 (LTS) |
| Spring Boot | 3.5.16 |
| PostgreSQL | 17 (via Docker) |
| Flyway | schema migrations |
| Maven | build |

---

## Running it

### 1. Start PostgreSQL

```bash
docker compose up -d
```

Postgres 17 on **host port 5433** (not 5432, to avoid clashing with a local
install). Database `sage`, user `sage`, password `sage`. Data persists in the
`sage-pgdata` volume.

### 2. Start the app

```bash
mvn spring-boot:run
```

Listens on **port 8081**. Confirm it is up:

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP"}
```

---

## The endpoint

### `POST /api/v1/request`

Request:

```json
{ "message": "hello from the calling app" }
```

Response:

```json
{ "reply": "Received: hello from the calling app" }
```

```bash
curl -X POST http://localhost:8081/api/v1/request \
  -H 'Content-Type: application/json' \
  -d '{"message": "hello from the calling app"}'
```

A blank or missing `message` returns **400**.

### Where the logic goes

`src/main/java/com/sage/teachingassistant/service/ResponseService.java`

```java
public String respond(String message) {
    // TODO: replace with the real response logic.
    return "Received: " + message;
}
```

That method is the only place response behaviour lives right now. It is a
placeholder that echoes the input, so the wiring can be exercised end to end.

---

## Configuration

All environment-overridable:

| Variable | Default | Purpose |
|---|---|---|
| `SAGE_PORT` | `8081` | HTTP port |
| `SAGE_DB_URL` | `jdbc:postgresql://localhost:5433/sage` | JDBC URL |
| `SAGE_DB_USER` | `sage` | Database user |
| `SAGE_DB_PASSWORD` | `sage` | Database password |

Port 8081 rather than the usual 8080 because something else on this machine
already occupies 8080.

---

## Database

PostgreSQL is connected and verified, but **there is no schema yet** — no
entities, no tables. The database is intentionally empty until the data model is
decided.

Migrations go in `src/main/resources/db/migration/` and are applied
automatically on startup. Name them `V1__something.sql`, `V2__...` and so on.

Hibernate runs with `ddl-auto=validate`, so once entities exist, the entity
mapping and the migration must agree.

---

## Project layout

```
src/main/java/com/sage/teachingassistant/
├── SageTeachingAssistantApplication.java
├── api/
│   ├── RequestController.java        the endpoint
│   └── dto/
│       ├── ApiRequest.java           { "message": "..." }
│       └── ApiResponse.java          { "reply": "..." }
└── service/
    └── ResponseService.java          the response logic
```

---

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home mvn clean verify
```

The `JAVA_HOME` override matters: `java` on PATH is 17, but Maven picks up JDK 25
by default. Spring Boot 3.5 targets 17.

Run the built jar:

```bash
java -jar target/sage-teaching-assistant-0.0.1-SNAPSHOT.jar
```
