# MOG API

`mog-api` is a Spring Boot 3 service that exposes HTTP endpoints for storing MOG DSL artifacts and executing reports using `mog-core`. The runtime currently emits XLSX artifacts (CSV/JSON formats are reserved for future writers).

## Configuration

- `mog.api.storeDir` – Root directory for persisted DSL and run artifacts. Defaults to `${MOG_ETC}/api-store` if `MOG_ETC` is set, otherwise `./etc/api-store` relative to the project.
- `mog.api.environment` – Optional override for the runtime environment passed to `MogContext` (defaults to `${MOG_ENV}`).
- `mog.api.mogHome` / `mog.api.mogEtc` – Optional overrides for `MOG_HOME`/`MOG_ETC` resolution inside the runtime context.

Run the service locally with:

```bash
./gradlew clean :mog-api:bootRun
```

Swagger/OpenAPI docs are available once the app is running at:

- JSON spec: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Example usage

The following `curl` commands assume the service is running on `http://localhost:8080` and that `${MOG_HOME}` includes any reference files your reports need.

1. **Upload a datasource DSL**

```bash
curl -X POST http://localhost:8080/api/datasources \
  -H "Content-Type: application/json" \
  -d '{
        "id": "inline-sales",
        "datasources": [
          { "name": "inline", "type": "json", "file": "${MOG_HOME}/etc/sales.json" }
        ]
      }'
```

2. **Upload a report DSL**

```bash
curl -X POST http://localhost:8080/api/reports \
  -H "Content-Type: application/json" \
  -d '{
        "id": "sample-report",
        "type": "xlsx",
        "name": "Sales Sample",
        "filename": "${MOG_HOME}/var/sales_sample_${timestamp}.xlsx",
        "sheets": [
          {
            "title": "Summary",
            "elements": [
              {
                "type": "table",
                "name": "sales_table",
                "upperLeft": { "row": 1, "col": 1 },
                "columns": [
                  { "title": "Region", "key": "region" },
                  { "title": "Revenue", "key": "revenue" }
                ],
                "dataSource": { "name": "inline", "filter": { "jsonPath": "$.data[*]" } }
              }
            ]
          }
        ]
      }'
```

3. **Execute the report**

```bash
curl -X POST http://localhost:8080/api/runs \
  -H "Content-Type: application/json" \
  -d '{
        "reportId": "sample-report",
        "datasourceId": "inline-sales",
        "params": { "year": 2025 },
        "output": { "format": "XLSX" }
      }'
# => { "runId": "<uuid>", "status": "COMPLETED" }
```

4. **Download the generated artifact**

```bash
curl -L -o run.xlsx http://localhost:8080/api/runs/<uuid>/artifact
```

5. **Inspect run metadata**

```bash
curl http://localhost:8080/api/runs/<uuid>
```

The metadata response includes timestamps, status, parameters, and artifact details (filename, content type, size). Artifacts and metadata are stored under `${mog.api.storeDir}/runs/{runId}`.
