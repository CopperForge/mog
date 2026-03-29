# Orion Phase 1 MOG Integration Plan

## Purpose

This document is a handoff brief for implementing the first Orion-to-MOG integration.

Phase 1 assumes:

- Orion is the host/orchestrator system.
- MOG is a separate service.
- Orion calls `mog-api` over HTTP.
- No in-process embedding of `mog-core`.
- No Orion-hosted report designer yet.
- No datasource/report sync automation yet beyond the minimum needed for execution.

## Primary Goal

Allow Orion to trigger MOG report runs and retrieve generated artifacts with a clean, supportable integration.

## Recommended Workspace Setup

- Keep Orion as the primary workspace/repo.
- Add MOG as a sibling folder in the same IDE workspace for reference only.
- Do not nest one repo inside the other.

Example:

- `c:\develop\copperforge\orion`
- `c:\develop\copperforge\mog`

## Phase 1 Scope

Implement these capabilities in Orion:

1. Configure the base URL for `mog-api`.
2. Trigger a MOG run from Orion.
3. Poll or query for run status.
4. Download the generated artifact.
5. Surface success/failure in Orion.
6. Optionally store the returned `runId` and artifact metadata in Orion for audit/tracking.

Do not implement yet:

- Orion-hosted report designer
- datasource synchronization UI
- report synchronization UI
- webhook/event-driven completion
- deep auth integration inside MOG
- in-process Java integration with `mog-core`

## Existing MOG API Surface

Available endpoints already support Phase 1:

- `GET /api/datasources`
- `GET /api/datasources/{id}`
- `POST /api/datasources`
- `GET /api/reports`
- `GET /api/reports/{id}`
- `POST /api/reports`
- `GET /api/runs?limit=50`
- `GET /api/runs/{id}`
- `POST /api/runs`
- `GET /api/runs/{id}/artifact`

Swagger UI is available from `mog-api` at:

- `/swagger-ui.html`

## MOG Reference Points

These are the most useful MOG files for the Orion-side agent to inspect while wiring Phase 1:

- `mog-api/src/main/java/org/copperforge/mog/api/web/ApiController.java`
- `mog-api/src/main/java/org/copperforge/mog/api/run/RunService.java`
- `mog-web/src/main/java/org/copperforge/mog/web/client/MogApiClient.java`
- `mog-core/src/main/java/org/copperforge/mog/runtime/MogRuntime.java`

Use `ApiController` for the HTTP contract, `RunService` for execution semantics, and `MogApiClient` as the closest existing example of how a client should call `mog-api`.

## Recommended Orion Architecture

Create a small MOG integration slice inside Orion:

### 1. Config

Add Orion configuration values such as:

- `mog.baseUrl`
- `mog.connectTimeout`
- `mog.readTimeout`
- optional `mog.enabled`

If Orion has environments:

- dev/test/prod values should point to the correct `mog-api` instance

### 2. Client Layer

Create an Orion-side client for MOG, for example:

- `MogClient`
- `MogRunClient`

Responsibilities:

- build HTTP requests
- serialize payloads
- parse responses
- translate HTTP failures into Orion-specific exceptions

Keep this layer thin. Do not mix business workflow logic into the HTTP client.

### 3. Service Layer

Create an Orion orchestration service, for example:

- `MogReportingService`

Responsibilities:

- decide which `reportId` and `datasourceId` to call
- assemble run `params`
- call `POST /api/runs`
- poll `GET /api/runs/{id}` if needed
- fetch artifact bytes from `GET /api/runs/{id}/artifact`
- map MOG failures into Orion workflow/user-facing failures

### 4. Controller / Job / Workflow Hook

Wire Orion entry points to the orchestration service:

- admin button
- scheduler job
- workflow action
- batch close event

Phase 1 should start with one concrete use case, not a generic framework.

## Recommended Phase 1 Runtime Flow

### Trigger

Orion decides a report should run.

Inputs:

- `reportId`
- `datasourceId`
- `params`
- output format, currently `XLSX`

### Start Run

Request:

```json
POST /api/runs
{
  "reportId": "monthly-recon",
  "datasourceId": "orion-prod",
  "params": {
    "month": "2026-03",
    "businessUnit": "claims"
  },
  "output": {
    "format": "XLSX"
  }
}
```

Response:

```json
{
  "runId": "8f5c...123",
  "status": "COMPLETED"
}
```

### Check Status

Request:

```json
GET /api/runs/{runId}
```

Expected response fields:

- `runId`
- `reportId`
- `datasourceId`
- `status`
- `startedAt`
- `completedAt`
- `message`
- `artifact.fileName`
- `artifact.contentType`
- `artifact.size`

### Download Artifact

Request:

- `GET /api/runs/{runId}/artifact`

Orion should preserve:

- filename
- content type
- raw bytes

## How Orion Should Treat MOG

Treat MOG as:

- a report execution engine
- a source of generated artifacts
- a source of run status

Do not treat MOG as:

- the source of truth for Orion business state
- the owner of Orion auth/workflow logic
- a replacement for Orion scheduling/audit

## Suggested Orion Data to Store

For each triggered run, Orion should consider storing:

- `mogRunId`
- requested `reportId`
- requested `datasourceId`
- run `params`
- trigger source
- requesting user or system principal
- Orion record/workflow/job correlation id
- final MOG status
- artifact filename/content type/size
- timestamp of request and completion

This is useful even if MOG also stores run metadata.

## Error Handling Rules

### Treat as configuration/integration error

- `404` for missing report or datasource
- `400` due to invalid report DSL or datasource issues
- repeated connection failures to `mog-api`

### Treat as execution failure

- run metadata status `FAILED`
- response message indicates bad input data or datasource access issues

### Treat as transport failure

- timeouts
- connection refused
- `5xx` responses from `mog-api`

Orion should distinguish these categories in logs and user/admin messaging.

## Polling Guidance

Even though runs currently complete quickly, Orion should still use `runId` as the source of truth.

Recommended Phase 1 polling model:

- trigger run
- check status immediately once
- if not terminal, poll on a short interval
- stop after a bounded timeout

Suggested starting values:

- interval: 2 to 5 seconds
- timeout: 30 to 60 seconds for interactive use
- longer timeout or async handling for scheduled jobs

## Security / Deployment Guidance

For Phase 1:

- keep `mog-api` internal
- place it behind Orion’s existing network/gateway boundary if available
- do not expose `mog-api` directly to end users
- keep auth at the Orion edge for now

If auth is later needed at MOG itself, treat that as a separate phase.

## How to Choose the First Use Case

Pick one report with:

- clear business value
- limited parameters
- stable datasource mapping
- XLSX as the desired output

Avoid starting with:

- multi-report orchestration
- user-authored DSL
- complex chart/pivot-heavy reports
- highly dynamic datasource setup

## Recommended Implementation Order in Orion

1. Add configuration for `mog-api` base URL.
2. Build a typed Orion MOG client.
3. Add one orchestration service for one report use case.
4. Trigger `POST /api/runs`.
5. Query `GET /api/runs/{id}`.
6. Download `GET /api/runs/{id}/artifact`.
7. Attach/store the artifact in Orion.
8. Add logging, correlation ids, and failure mapping.
9. Add tests around client serialization and orchestration behavior.

## Testing Expectations

At minimum in Orion:

- client serialization test for run payload
- client response mapping test
- service test for success flow
- service test for failed run status
- service test for HTTP 4xx/5xx handling
- controller/job test for the initial trigger point

If Orion has integration-test infrastructure:

- run against a real local `mog-api`
- verify end-to-end artifact retrieval

## Suggested Acceptance Criteria

Phase 1 is done when:

1. Orion can trigger one MOG report run from a real workflow or admin action.
2. Orion captures the returned `runId`.
3. Orion can determine terminal run status.
4. Orion can download the artifact and attach/store it.
5. Orion logs enough detail to diagnose failures.
6. No manual MOG console interaction is required for normal execution of the selected use case.

## Non-Goals for This Phase

Explicitly out of scope:

- Orion visual report designer
- bidirectional DSL editing
- datasource sync UI
- generalized report marketplace/catalog
- webhook callbacks from MOG
- replacing MOG artifact storage
- replacing MOG execution with Orion-local generation

## Future Phase Hooks

The following later phases remain compatible with this Phase 1 design:

- Orion-hosted report designer that emits MOG DSL
- Orion-owned datasource admin that publishes to MOG
- report/datasource sync workflows
- queued or async run processing
- webhook/event-based completion
- stronger auth between Orion and MOG

## Final Recommendation

Implement Phase 1 from Orion outward:

- Orion owns the trigger
- Orion owns the business decision to run
- MOG owns report execution
- Orion consumes the artifact

Do not start by trying to unify both systems’ models. Start with one report flow and prove the orchestration path first.
