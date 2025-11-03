# MOG — It is what you make it
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

## One‑liner Sample Generation

You can generate the sample report in a single Gradle task that assembles the app, seeds `${MOG_HOME}/etc`, and runs the generator:

```bash
./gradlew sampleReport            # Windows: .\gradlew.bat sampleReport
```

This writes a file like `chart-sample-<timestamp>.xlsx` to the project root.

---

## Coordinates and Indexing (1‑based)

Report coordinates are 1‑based to match Excel:
- A1 → `{ "row": 1, "col": 1 }`
- B2 → `{ "row": 2, "col": 2 }`

Chart table column indices are also 1‑based:
- First table column → `column: 1`
- Second table column → `column: 2`, etc.

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
