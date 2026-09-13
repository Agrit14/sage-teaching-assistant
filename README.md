# Sage — Teaching Assistant

A Telegram teaching assistant. Sage is meant to guide a learner to understanding
rather than hand over answers — asking the question that unblocks them, checking
their reasoning, and remembering where they struggled.

**Status: first milestone.** The bot is live on Telegram, talks to a Spring Boot
backend, and persists every exchange to PostgreSQL. The actual tutoring behaviour
is not built yet — see [What's next](#whats-next).

---

## Architecture

```
Telegram  ──long poll──▶  Spring Boot app  ──JPA──▶  PostgreSQL
                                │
                                └──REST API──▶  (anything else that wants to talk to Sage)
```

One process. The Spring Boot application *is* the bot — it long-polls Telegram for
updates and also serves a REST API. There is no separate bot service to run.

This was a deliberate choice: it is the shortest path to a working bot, and the
REST API is already the seam you would use if the teaching logic later moves out
into its own service.

## Stack

| Piece | Version |
|---|---|
| Java | 17 (LTS) |
| Spring Boot | 3.5.16 |
| PostgreSQL | 17 (via Docker) |
| Flyway | schema migrations |
| telegrambots | 10.3.0 (long polling) |

---

## Prerequisites

- JDK 17
- Maven 3.9+
- Docker (for PostgreSQL)
- A Telegram account

---

## Setup

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts Postgres 17 on **host port 5433** (not 5432, to avoid clashing with a
local install) with database `sage`, user `sage`, password `sage`. Data persists
in the `sage-pgdata` volume.

Flyway creates the schema automatically on first boot.

### 2. Create the Telegram bot

1. Open Telegram and message [@BotFather](https://t.me/BotFather)
2. Send `/newbot`
3. Give it a display name (e.g. `Sage`) and a username ending in `bot`
   (e.g. `SageTeachingBot`)
4. BotFather replies with a token that looks like
   `123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw`

Keep that token private — anyone with it controls your bot.

### 3. Run the app

```bash
export TELEGRAM_BOT_TOKEN="123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw"
export TELEGRAM_BOT_USERNAME="SageTeachingBot"

mvn spring-boot:run
```

The app listens on **port 8081**. If a token is present you'll see:

```
Sage is live and polling Telegram as @SageTeachingBot
```

Open Telegram, find your bot, and send `/start`.

> **No token? It still runs.** Without `TELEGRAM_BOT_TOKEN` the bot is skipped
> entirely and only the REST API and database start up. Useful for working on the
> backend without touching Telegram.

---

## Configuration

Everything is environment-overridable:

| Variable | Default | Purpose |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | *(empty)* | BotFather token. Blank = bot disabled |
| `TELEGRAM_BOT_USERNAME` | *(empty)* | Bot handle, used to parse `/cmd@BotName` |
| `TELEGRAM_BOT_ENABLED` | `true` | Set `false` to disable the bot even with a token |
| `SAGE_PORT` | `8081` | HTTP port |
| `SAGE_DB_URL` | `jdbc:postgresql://localhost:5433/sage` | JDBC URL |
| `SAGE_DB_USER` | `sage` | Database user |
| `SAGE_DB_PASSWORD` | `sage` | Database password |

Port 8081 rather than the usual 8080 because something else on this machine
already occupies 8080.

---

## Bot commands

| Command | What it does |
|---|---|
| `/start` | Introduces Sage and registers the learner |
| `/help` | Lists available commands |
| `/subjects` | Shows the planned subject coverage |
| `/id` | Replies with the Telegram chat id |
| `/about` | Explains what Sage is |

Anything that isn't a command is stored and acknowledged with a placeholder reply,
because teaching mode isn't wired up yet.

---

## REST API

Base path `/api/v1`.

### `GET /bot/status`

```json
{ "botActive": false, "botUsername": "", "learnerCount": 0 }
```

### `GET /learners`

All known learners.

### `GET /learners/{chatId}`

One learner, or `404`.

### `GET /learners/{chatId}/messages?page=0&size=50`

Paged message history, newest first.

### `POST /messages`

Push a message into a Telegram chat. This is the endpoint an external service
would call to drive Sage.

```bash
curl -X POST http://localhost:8081/api/v1/messages \
  -H 'Content-Type: application/json' \
  -d '{"chatId": 123456789, "text": "Time to revise."}'
```

Returns `503` when no bot token is configured, since there is no way to send.

### `GET /actuator/health`

Standard Spring Boot health check.

---

## Database

Two tables, created by `src/main/resources/db/migration/V1__init.sql`:

- **`learners`** — one row per Telegram chat. Holds the chat id, the user's
  profile fields, and when they were last seen.
- **`messages`** — every inbound and outbound message, with a `direction` of
  `INBOUND` or `OUTBOUND`, cascading on learner delete.

Hibernate runs with `ddl-auto=validate`, so **entity and migration must change
together** — add a `V2__*.sql` rather than editing `V1`.

---

## Project layout

```
src/main/java/com/sage/teachingassistant/
├── SageTeachingAssistantApplication.java
├── api/                  REST controllers and DTOs
├── config/               properties binding + conditional bot wiring
├── domain/               JPA entities
├── repository/           Spring Data repositories
├── service/              learner, message log, and conversation logic
└── telegram/             the bot, command routing, outbound sending
```

`CommandRouter` is deliberately free of Spring and of the database, so command
wording and parsing are unit-tested without any infrastructure.

---

## Tests

```bash
mvn test
```

15 tests covering command parsing (including `/cmd@BotName` handling and
whitespace tolerance) and message splitting at Telegram's 4096-character limit.
No database required.

---

## What's next

- **Teaching behaviour.** The real work: a Socratic tutor loop instead of the
  current placeholder reply.
- **Session and topic tracking.** Tables for what a learner has covered and where
  they got stuck, so Sage can pick up where it left off.
- **Rich messages.** Inline keyboards for multiple choice, and handling of photos
  and documents — the bot currently ignores anything that isn't text.
- **Authentication on the REST API.** It is wide open right now; fine locally,
  not fine anywhere else.
- **Testcontainers** for a full context test against a real Postgres.
