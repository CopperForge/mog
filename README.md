# MOG — It is what you make it
See INSTALL for setup and distribution details: INSTALL.md
<img src="src/main/resources/assets/png/moglet.png" alt="Mog Mascot" width="200" />

A small, batteries-included CLI to define data-driven reports and export them to XLSX, plus simple AES encryption/decryption utilities for managing secrets. Built with Java 21 and Picocli; ships as a fat jar and optional RPM.

---

## Features
- Report generation from `.mog` definition files (see examples under `src/main/examples/reports`).
- XLSX writer with tables, styles, charts, and pivot tables.
- Flexible data sources: file / JSON / REST / JDBC / dotout.
- CLI subcommands via Picocli: `report generate`, `encrypt`, `decrypt`.
- Configurable search paths and site config; supports per-user secrets file.
- Cross-platform launchers (`src/main/bin/mog`, `src/main/bin/mog.cmd`) and RPM packaging.

---

## Requirements
- Java 21 (JDK). The Gradle wrapper is included; no local Gradle install required.
- OS: Windows, Linux, or macOS. (For RPM packaging, build on Linux.)

---

## Quickstart

Build the fat jar:
```bash
./gradlew clean build
```

Run a sample report (from project root):
```bash
# set MOG_HOME to the repo root so the default config path resolves
export MOG_HOME="$PWD"            # PowerShell: $env:MOG_HOME = (Get-Location).Path

# copy the sample report into ${MOG_HOME}/etc/reports
mkdir -p etc/reports               # PowerShell: New-Item -ItemType Directory -Path .\etc\reports
cp src/main/examples/reports/chart.sample.report.mog etc/reports/

# generate the XLSX
./gradlew run --args="report generate --report=chart.sample.report.mog"
```
The generated workbook will be written to the working directory. Open it in Excel to view the table and chart.

---

## REST API + Web Console

Spin up the JSON API (`mog-api`) alongside the new Bootstrap-powered “MOG Console” UI (`mog-web`):

```bash
# terminal 1
./gradlew :mog-api:bootRun

# terminal 2
./gradlew :mog-web:bootRun
```

- API base URL: `http://localhost:8080`
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - Key endpoints now include:
    - `GET /api/datasources`, `GET /api/datasources/{id}`
    - `GET /api/reports`, `GET /api/reports/{id}`
    - `GET /api/runs?limit=50`, `GET /api/runs/{id}`, `POST /api/runs`
- Web console: `http://localhost:8081/ui`
  - `/ui` dashboard with latest runs, status cards, and “Mog says…” callouts
  - `/ui/datasources` & `/ui/reports` for listing and uploading DSL JSON
  - `/ui/runs` to launch runs, view history, and download artifacts
  - `/ui/swagger` quick link to the API docs

All UI pages proxy through the server (no browser-direct calls), so you can use the console without extra CORS configuration.

---

## One‑liner Sample Generation

You can generate the sample report in a single Gradle task that assembles the app, seeds `${MOG_HOME}/etc`, and runs the generator:

```bash
./gradlew sampleReport            # Windows: .\gradlew.bat sampleReport
```

This writes a file like `chart-sample-<timestamp>.xlsx` to the project root.

---

## Usage

Run with Gradle (no install needed):
```bash
./gradlew run --args="report generate --report=chart.sample.report.mog"
```

Run the shaded jar (after build):
```bash
java -jar build/libs/mog.jar report generate --report=chart.sample.report.mog
```

Options accepted at the root command:
- `--config=<path>`: Path to site config (defaults to `${MOG_HOME}/etc/config.mog`).
- `--working-dir=<path>`: Set a working directory used for resolving relative report paths.

Examples:
```bash
# Explicit config path
java -jar build/libs/mog.jar --config=etc/config.mog report generate --report=chart.sample.report.mog

# Working directory (report path resolved relative to this directory)
java -jar build/libs/mog.jar --working-dir=./etc/reports report generate --report=chart.sample.report.mog
```

Resolution order for report paths:
1) Absolute path; 2) Relative to current working directory; 3) Relative to `--working-dir` (if provided);
4) Relative to `MOG_ETC`, then `MOG_HOME`; 5) Search configured report paths (`config.mog`).

---

## Coordinates and Indexing (1‑based)

Report coordinates are 1‑based to match Excel:
- A1 → `{ "row": 1, "col": 1 }`
- B2 → `{ "row": 2, "col": 2 }`

Chart table column indices are also 1‑based:
- First table column → `column: 1`
- Second table column → `column: 2`, etc.

Breaking change: Coordinates were switched to 1‑based in the Unreleased version noted in the CHANGELOG.

---

## Sample Report Definition (XLSX)

`src/main/examples/reports/chart.sample.report.mog`
```json
{
  "name": "chart-sample",
  "type": "xlsx",
  "filename": "chart-sample-${timestamp}.xlsx",
  "dataSources": [
    { "name": "inline", "type": "json", "file": "src/main/examples/reports/sales.json" }
  ],
  "sheets": [
    {
      "name": "Charting",
      "title": "Charting",
      "elements": [
        {
          "type": "table",
          "name": "t_sales",
          "title": "Sales",
          "upperLeft": { "row": 1, "col": 1 },
          "columns": [
            { "title": "Region",  "key": "region"  },
            { "title": "Revenue", "key": "revenue" },
            { "title": "Units",   "key": "units"   }
          ],
          "dataSource": { "name": "inline", "filter": { "type": "json", "jsonPath": "$.data[*]" } }
        },
        {
          "type": "chart",
          "name": "sales_chart",
          "chartType": "bar",
          "title": "Revenue by Region",
          "upperLeft": { "row": 1, "col": 8 },
          "width": 10, "height": 16,
          "category": { "table": "t_sales", "column": 1 },
          "series": [
            { "name": "Revenue", "table": "t_sales", "column": 2 }
          ]
        }
      ]
    }
  ]
}
```

Companion sample data:
`src/main/examples/reports/sales.json`
```json
{ "data": [
  { "region": "North", "revenue": 100, "units": 20 },
  { "region": "South", "revenue": 120, "units": 30 },
  { "region": "West",  "revenue":  90, "units": 25 },
  { "region": "East",  "revenue": 140, "units": 35 }
]}
```

---

## Configuration

- Default config path is `${MOG_HOME}/etc/config.mog`. Set the `MOG_HOME` environment variable or pass `--working-dir` to control process working directory. The root CLI currently discovers config via `MOG_HOME`.
- The sample site config lives at `src/install/resources/config.mog` — copy it to `${MOG_HOME}/etc/config.mog` to get started.

---

## Notes

- Excel repair warnings: The writers validate table ranges and chart column indices, and sanitize table display names to prevent Excel “Repaired Records” messages.
- Logging: SLF4J + Logback are in use. If you see a Log4j bridge warning, it’s harmless; a bridge can be added if desired.

Validation highlights:
- Tables require `upperLeft` (row/col ≥ 1) and at least one column.
- If a table declares a `dataSource` name not present in the report’s `dataSources`, generation fails with a clear error.
- Chart column indices are validated against the table width; out‑of‑range indices fail fast.

---

## Global Datasources (Catalog)

Reports can reference datasources by name, resolved from an external catalog, so you can reuse and manage them centrally.

- Locations (in priority order):
  1. `--datasources=<file-or-dir>` if provided
  2. `${MOG_ETC}`
  3. `${MOG_HOME}/etc`

- File patterns in each location (loaded in the following order; later overrides earlier by name):
  - `datasources.mog`
  - `datasources/*.mog` (sorted by filename)
  - Environment-specific (when `--env=<name>` or `MOG_ENV` is set):
    - `datasources.<env>.mog`
    - `datasources/<env>/*.mog` (sorted)

- Catalog schema example (datasources.mog):
```json
{
  "datasources": [
    { "name": "sales_db", "type": "jdbc", "url": "${SALES_JDBC_URL}", "user": "${SALES_USER}", "password": "${SALES_PASS}" },
    { "name": "sales_api", "type": "api",  "baseUrl": "${SALES_API_URL}" },
    { "name": "inline",   "type": "json", "file": "${MOG_HOME}/etc/sales.json" }
  ]
}
```

- Referencing a catalog datasource from a report element:
```json
{
  "type": "table",
  "name": "t_sales",
  "upperLeft": { "row": 1, "col": 1 },
  "columns": [ { "title": "Region", "key": "region" }, { "title": "Revenue", "key": "revenue" } ],
  "dataSource": { "name": "inline", "filter": { "type": "json", "jsonPath": "$.data[*]" } }
}
```

- Precedence and overrides:
  - Inline `report.dataSources` take precedence over catalog entries with the same name.
  - Later-loaded catalog files override earlier ones; overrides are logged with the old and new source file paths.

- Tips:
  - Use env vars for secrets (`${VAR}`) — values are substituted at runtime.
  - Use `--env=dev|test|prod` (or `MOG_ENV`) to switch datasources per environment.
  - Organize per-environment files under `datasources.<env>.mog` or `datasources/<env>/...`.

Seeded examples:
- A starter catalog is provided at `src/install/resources/etc/datasources.mog` and a sample dataset at `src/install/resources/etc/sales.json`.
- The `sampleReport` task seeds `${MOG_HOME}/etc/datasources.mog` and `${MOG_HOME}/etc/sales.json` if missing.

Getting started quickly:
- Default (generic):
  - `./gradlew sampleReport` (Windows: `.\u0067radlew.bat sampleReport`)
  - Seeds `${MOG_HOME}/etc` with a starter `datasources.mog` and `sales.json`.
- Environment-specific (e.g., dev):
  - `./gradlew sampleReport -Penv=dev`
  - Also seeds `${MOG_HOME}/etc/datasources.dev.mog` (template provided under `src/install/resources/etc`).
  - The task passes `--env=dev` and `--datasources=${MOG_HOME}/etc` to the app.
