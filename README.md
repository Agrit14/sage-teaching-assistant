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

## API

Base path `/api/v1/workflows`.

### `GET /api/v1/workflows`

Lists the workflows available to start, with their stages in order.

### `POST /api/v1/workflows/runs`

Starts a run and executes stage 0.

```json
{ "workflowKey": "educational-notes", "message": "notes and a worksheet on photosynthesis for class 8" }
```

```json
{
  "runId": "5766563a-2763-4cb4-91f9-79d73f0c8155",
  "workflowKey": "educational-notes",
  "status": "AWAITING_APPROVAL",
  "stageIndex": 0,
  "stageCount": 3,
  "stageKey": "confirm",
  "stageName": "Confirm the request",
  "message": "Before I build anything, let me check I've understood you...",
  "output": { "confirm.confirmedRequest": "notes and a worksheet on photosynthesis for class 8" },
  "completed": false
}
```

### `POST /api/v1/workflows/runs/{runId}/messages`

The one call that moves a run. What happens depends on the message:

| Message | Effect |
|---|---|
| `"yes"` | Approves the current stage, advances to the next one |
| `"yes"` on the last stage | Completes the run |
| anything else | Treats it as feedback, re-runs the current stage |

```json
{ "message": "yes" }
```

### `GET /api/v1/workflows/runs/{runId}`

The run's position plus its full execution history, including every revision.

### Errors

| Status | When |
|---|---|
| `400` | Blank `message`, or missing `workflowKey` |
| `404` | Unknown workflow key, or unknown run id |
| `409` | Message sent to a run that has already finished |

---

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
