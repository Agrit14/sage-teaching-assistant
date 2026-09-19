# Sage — Workflow Engine

A Spring Boot + PostgreSQL backend that runs multi-stage workflows. Each stage does
one fixed job, and the run only advances when the user approves what it produced.

No Telegram, no bot. Something else calls this API.

---

## The idea

A long job done in one shot is hard to check. Split it into stages, get a human to
sign off at each boundary, and the final output is far more likely to be right.

So the engine enforces one rule: **a stage's output must be approved before the next
stage runs.** Approving is the only way forward. Anything else is treated as
feedback and sends the stage back around.

```
stage 0 ──output──▶ user reviews ──approve──▶ stage 1 ──output──▶ user reviews ──approve──▶ stage 2 ──▶ done
                        │                                                       │
                     feedback                                                feedback
                        │                                                       │
                        └──────── stage 0 re-runs ────┘         └──── stage 1 re-runs ────┘
```

---

## The workflow

One workflow is registered: **`educational-notes`** — "Educational notes and worksheet".

| # | Stage key | Name | Job |
|---|---|---|---|
| 0 | `confirm` | Confirm the request | Restate what the user is asking and get a yes |
| 1 | `research` | Research and draft | Research the confirmed request, produce a Word draft |
| 2 | `template` | Apply template and export PDF | Lay the approved draft into the template, produce a PDF |

**Every stage is currently a stub.** Each one produces a realistic message and a
correctly-shaped output, but the actual work — web search, model call, PDF render —
is not implemented. Each stage has a `TODO` marking exactly where its tool goes.
That was the agreed split: build the engine first, choose the tools next.

---

## Running it

```bash
docker compose up -d          # PostgreSQL 17 on host port 5433
mvn spring-boot:run           # listens on 8081
```

Health check: `curl http://localhost:8081/actuator/health`

---

## Workflows

Sage has 4 registered workflows tailored for educational teaching assistance:

| Workflow Key | Name | Input | Stages |
|---|---|---|---|
| `worksheet-generation` | Worksheet Generator (Alpha Tutor) | Class + Topic | 1. Research & Outline $\rightarrow$ 2. Word Draft $\rightarrow$ 3. PDF Export |
| `test-generation` | Test Paper Generator (Alpha Tutor) | Class + Topic + Marks | 1. Blueprint $\rightarrow$ 2. Examination Word Draft $\rightarrow$ 3. PDF Export |
| `notes-generation` | Revision Notes Generator (Alpha Tutor) | Class + Topic | 1. Concept Scope $\rightarrow$ 2. Notes Word Draft $\rightarrow$ 3. PDF Export |
| `pdf-print` | Direct PDF Print (Alpha Tutor) | Uploaded `.docx` | 1. Format into Template & Export PDF |

---

## API

Base path `/api/v1/workflows`.

### `GET /api/v1/workflows`

Lists the workflows available to start, with their stages in order.

### `POST /api/v1/workflows/runs`

Starts a run and executes stage 0.

```json
{ "workflowKey": "worksheet-generation", "message": "Class 9 Physics - Laws of Motion" }
```

### `POST /api/v1/workflows/runs/{runId}/messages`

Moves the run forward.

| Message | Effect |
|---|---|
| `"yes"` | Approves the current stage, advances to the next one |
| `"yes"` on the last stage | Completes the run |
| anything else | Treats it as feedback, re-runs the current stage |

### `GET /api/v1/workflows/runs/{runId}`

Returns the run's status, latest stage output, and complete revision history.

### `GET /api/v1/workflows/runs/{runId}/files/docx`

Downloads the generated Alpha Tutor Word document (`.docx`).

### `GET /api/v1/workflows/runs/{runId}/files/pdf`

Downloads or views the finalized Alpha Tutor PDF document (`.pdf`).

### `POST /api/v1/workflows/runs/{runId}/upload`

Uploads an external `.docx` file for the `pdf-print` workflow (multipart form-data with part `file`).

### Errors

| Status | When |
|---|---|
| `400` | Blank `message`, missing `workflowKey`, or empty upload |
| `404` | Unknown workflow key, unknown run id, or file not found |
| `409` | Message sent to a run that has already finished |

---

## Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `GEMINI_API_KEY` | *(empty)* | Google Gemini API key (with Google Search grounding). Uses fallback if not set. |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Gemini model name. |
| `SAGE_STORAGE_PATH` | `./data/files` | Directory path where generated docx and pdf files are saved. |
| `SAGE_PORT` | `8081` | Server listening port. |
| `SAGE_DB_URL` | `jdbc:postgresql://localhost:5433/sage` | PostgreSQL JDBC connection URL. |
| `SAGE_DB_USER` | `sage` | PostgreSQL user. |
| `SAGE_DB_PASSWORD` | `sage` | PostgreSQL password. |

## Design notes

**Stages are independent.** A stage never calls another stage and never reads
another stage's code. It receives a `StageContext` and returns a `StageResult`.
Anything it needs from earlier stages arrives in `context.priorOutputs()`.

**The contract is one file.** `WorkflowPayload` holds the agreed output keys
(`confirm.confirmedRequest`, `research.draftDocument`, `template.finalPdf`). Stages
reference those names, not each other, so any stage can be rewritten in isolation
as long as it honours the contract.

**Ordering lives in one place.** `WorkflowCatalog` declares the sequence. Stages
carry no ordering, so the sequence cannot drift out of step with the code. The
definition validates at startup that every stage's `workflowKey` matches and that
no two stages share a key.

**Approval is deliberately strict.** `KeywordApprovalClassifier` approves only when
the message is, on its own, an unambiguous yes. `"yes but add more on mitosis"` is
treated as feedback. Being strict costs one extra round trip; being loose silently
skips work the user wanted changed. It is behind an `ApprovalClassifier` interface
because this is a judgement call that will eventually want a model behind it.

**The engine is not `@Transactional`.** Each repository call is its own short
transaction, so a stage runs with no transaction open. That matters once these
stages do real work — a web search or a model call held open across a database
connection would exhaust the pool.

---

## Database

Two tables, created by `src/main/resources/db/migration/V1__workflow_engine.sql`:

- **`workflow_runs`** — one row per run: the workflow, the status, and the current
  stage index. The run holds its position and nothing else.
- **`stage_executions`** — one row per stage execution, including every revision.
  Cascades on run delete. This is the audit trail.

Hibernate runs with `ddl-auto=validate`, so entities and migrations must change
together. Add `V2__*.sql` rather than editing `V1`.

---

## Project layout

```
src/main/java/com/sage/teachingassistant/
├── api/
│   ├── WorkflowController.java        the four endpoints
│   ├── WorkflowExceptionHandler.java  workflow errors -> HTTP status
│   └── dto/
├── domain/                            WorkflowRun, StageExecution
├── repository/
└── workflow/
    ├── WorkflowEngine.java            advances runs on approval
    ├── WorkflowStage.java             what a stage must implement
    ├── StageContext.java              what a stage receives
    ├── StageResult.java               what a stage returns
    ├── WorkflowDefinition.java        an ordered sequence of stages
    ├── WorkflowCatalog.java           the registered workflows
    ├── WorkflowPayload.java           the key contract between stages
    ├── WorkflowRegistry.java
    ├── ApprovalClassifier.java        the seam for yes/no judgement
    ├── KeywordApprovalClassifier.java
    └── stage/
        ├── ConfirmRequestStage.java   stage 0
        ├── ResearchStage.java         stage 1
        └── TemplateStage.java         stage 2
```

---

## Adding a stage

1. Implement `WorkflowStage` in `workflow/stage/`, return a `StageResult`.
2. Add it to the list in `WorkflowCatalog`.

Nothing else needs to change. The engine handles approval, history, retries and
passing data forward.

---

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home mvn verify
```

Two things about this machine:

- The `JAVA_HOME` override matters — `java` on PATH is 17, but Maven picks up JDK 25.
- Do **not** use `mvn clean`. macOS tags build output with a `com.apple.provenance`
  attribute that blocks the Spring Boot repackage rename. Instead:

  ```bash
  rm -rf target && xattr -rc . && mvn verify
  ```
