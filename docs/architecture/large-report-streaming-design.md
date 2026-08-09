# MOG Large-Report Streaming Design

Date: 2026-08-09

Scope: architecture/discovery gate only. No production-code changes are included in this pass.

Note on supplied specification: I could not find an attached or checked-in file named `MOG Large-Report Streaming Architecture`, or matching content, in this workspace. The findings below are therefore verified against the current repository and the target capability/constraints from the request.

## 1. Verified current architecture

### Datasource lifecycle

`MogDataSource` is the abstract JSON-polymorphic datasource base. It currently exposes only a list-returning contract:

```java
public List<? extends MogFetchable> fetch(MogDataFilter filter) throws MogException
public abstract List<? extends MogFetchable> fetch(MogDataFilter filter, MogContext context) throws MogException
```

Evidence: `mog-core/src/main/java/org/copperforge/mog/data/MogDataSource.java:47-51`.

Current implementations:

| Class | Current behavior | Lifecycle/cleanup |
| --- | --- | --- |
| `MogJdbcDataSource` | Resolves URL through `MogVariableService`, decrypts password, optionally loads driver class, appends offset/limit SQL text, creates `Connection`, creates plain `Statement`, executes query, copies every `ResultSet` row into a new `MogFetchable`, returns `ArrayList<MogFetchable>`. Column names are lowercased. | JDBC resources are scoped to try-with-resources inside `fetch(...)`, so cleanup occurs only after the whole result has been materialized. |
| `MogJsonDataSource` | Reads the whole JSON file into a `String`, applies JsonPath, returns either one `MogFetchable` or a collected list from `List<Map<String,Object>>`. | File read is eager; no streaming JSON lifecycle. |
| `MogRestDataSource` | Extends `MogJsonDataSource`; performs a Java `HttpClient.send(..., BodyHandlers.ofString())`, returns the full response body string, then JsonPath materializes rows through the parent. | HTTP response and JSON body are eager. |
| `MogDotOutDataSource` | Reads binary dot-out records sequentially, applies optional `MogQueryFilter` expression, appends accepted rows to a list. | Reader lifecycle is local to `fetch(...)`, but rows are fully accumulated before return. |

`MogQueryFilter` stores a SQL/query string and tries to parse it as a JSqlParser expression first, then as a `PlainSelect`; it stores parsed `where` and selected columns. It has no parameter fields today. Evidence: `mog-core/src/main/java/org/copperforge/mog/data/filter/MogQueryFilter.java:19-74`.

`MogVariableService` has string-oriented substitution. Context variables are stored as `Map<String,Object>` in `MogContext`, but `MogVariableService.contextualValue(...)` converts values to `String.valueOf(value)`. Evidence: `MogContext.java:22,64,130-138`; `MogVariableService.java:92-121`.

All production callers of `fetch(...)`:

| Caller | Purpose |
| --- | --- |
| `XLSXTableWriter.write(...)` | Fetches table data before writing rows. Evidence: `XLSXTableWriter.java:51`. |

Tests also call `fetch(...)` directly in datasource tests and report tests.

### XLSX lifecycle

`ReportWriterService` registers only `"xlsx"` and builds `XLSXReportWriter`. `MogRuntime.generateReport(...)` selects by `definition.getType()`, calls `build(...)`, resolves filename, then `save(...)`.

`XLSXReportWriter`:

- Holds an `XSSFWorkbook`.
- Creates a new `XSSFWorkbook` during `buildReport(...)`.
- Creates one `XSSFSheet` per report sheet using `sheet.getTitle()`.
- Dispatches each element to an `XLSXElementWriter`.
- Writes to `FileOutputStream`, closes the workbook, then nulls it.

Evidence: `XLSXReportWriter.java:19,31,38-42,47-57`.

`XLSXElementWriter` is XSSF-specific: it stores `XSSFWorkbook` and `XSSFSheet`, so every current XLSX element writer is coupled to XSSF types. Evidence: `XLSXElementWriter.java:3-28`.

`XLSXTableWriter` lifecycle:

1. Resolve datasource from report-local list, then global `DataSourcesCatalog` fallback.
2. Call `mogDataSource.fetch(...)`.
3. Compute `rowCount = data.size()` or `1` for no data.
4. Build `AreaReference` using known final dimensions.
5. Create `XSSFTable` with `sheet().createTable(reference)`.
6. Set table name/display name/style.
7. Write headers and rows.
8. Add table CT autofilter when enabled.

Evidence: `XLSXTableWriter.java:35-65,107-122,133-148,158-177,197-226,244-284`.

Current XLSX features observed:

- XSSF workbook/sheet/table.
- `AreaReference` based on known `data.size()`.
- Table style via `XSSFTableStyleInfo` and CT table style metadata.
- Column widths.
- Header cells and row cells with number/boolean/string values.
- Optional column-level `CellStyle`.
- CT table autofilter.
- Merged regions and styled spanned text.
- Charts through XSSF/XDDF APIs, table-backed or explicit cell ranges.
- Pivot tables through XSSF pivot APIs.
- No current production formula-writing path was found.

## 2. Corrections to assumptions in the supplied specification

Because the named spec file was not present, these corrections are against likely target assumptions implied by the request:

- MOG does not have a datasource streaming abstraction today. Every datasource returns `List<? extends MogFetchable>`.
- JDBC resources are cleaned up by `fetch(...)`, but only after full materialization. A streaming design cannot return a raw `ResultSet`; the datasource-owned cursor must own `ResultSet`, `Statement`, and `Connection`.
- `MogFetchable` is not tied to JDBC. It is a map-backed `MogBean` row wrapper and can represent JSON, REST, dot-out, or JDBC rows.
- The current XLSX implementation cannot be converted by replacing `XSSFWorkbook` with `SXSSFWorkbook`. Writer base classes and chart/pivot/table code are XSSF-specific.
- `XSSFTable` is central today. Current table creation requires the final table area before rows are written, because row count is derived from `data.size()`.
- Existing report DSL examples include some zero-based coordinates in old test/sample resources, but current `XLSXTableWriter.validateTableInputs(...)` requires one-based row/column coordinates greater than or equal to 1.
- The API run path is synchronous. `POST /api/runs` calls `runService.execute(...)`, generates the report inline, and returns after completion/failure.

## 3. "too many bytes" investigation

Repository search found no production source or test source that throws the exact message `java.lang.IllegalArgumentException: too many bytes`.

Dependency evidence:

- MOG uses Apache POI `5.4.0`, POI OOXML `5.4.0`, Commons Compress `1.27.1`, XMLBeans `5.3.0`.
- Searching cached source jars for POI, POI OOXML, Commons Compress, and XMLBeans found the exact phrase only as a Commons Compress GZIP comment, not as a thrown exception string.
- Searching local JDK 21.0.10 sources found `IllegalArgumentException("too many bytes")` in `java.net.http/jdk/internal/net/http/common/Utils.java`, in overloads that sum `ByteBuffer` remaining bytes against a maximum.

Implication: repository/dependency evidence does not support claiming the XLSX writer itself throws this exact message. If the observed stack trace contains `jdk.internal.net.http.common.Utils.remaining(...)`, it is likely from Java's HTTP client/request-body path, not from POI. If the stack contains POI/OOXML packaging classes, the current evidence still needs a full stack trace before assigning origin.

Practical diagnostic follow-up before implementation:

- Capture the full stack trace at the API boundary and CLI boundary.
- Log report id, datasource id, output filename, output size if known, and top-level cause class/message.
- Avoid logging SQL parameter values, passwords, tokens, or full report payloads.

No diagnostic code change was necessary in this pass because the repository itself did not contain the failure trace.

## 4. Proposed datasource streaming API

Recommendation: add a closeable cursor contract, not callback-first and not Java `Stream` as the primary API.

### Options compared

| Option | Pros | Cons | Fit for MOG |
| --- | --- | --- | --- |
| Callback/consumer | Strong datasource ownership of cleanup; easy to scope JDBC resources inside the datasource method. | Harder to compose with table writers; awkward row counts/metadata; cancellation and partial progress need side channels; exceptions inside consumer need wrapping. | Viable but too restrictive for report writer evolution. |
| Closeable iterator/cursor | Explicit lifecycle; datasource can return a handle that owns JDBC resources; checked `MogException` can be preserved; writer can stream rows naturally and close in try-with-resources. | Callers must close; needs tests to prove cleanup on writer exceptions. | Best fit. |
| Java `Stream<MogFetchable>` | Familiar lazy pipeline syntax. | `Stream` close is commonly forgotten; checked SQL exceptions do not fit; lifecycle ownership is easier to obscure; parallel stream would be unsafe for JDBC cursors. | Not recommended as core contract. Optional adapter later. |

Recommended signatures:

```java
package org.copperforge.mog.data;

import java.util.List;

public interface MogFetchCursor extends AutoCloseable {
    List<String> columns() throws MogException;

    boolean next() throws MogException;

    MogFetchable current() throws MogException;

    long rowNumber();

    @Override
    void close() throws MogException;
}
```

```java
public abstract class MogDataSource {
    public List<? extends MogFetchable> fetch(MogDataFilter filter) throws MogException {
        return fetch(filter, null);
    }

    public abstract List<? extends MogFetchable> fetch(MogDataFilter filter, MogContext context)
            throws MogException;

    public MogFetchCursor openCursor(MogDataFilter filter, MogContext context) throws MogException {
        return MogFetchCursors.fromList(fetch(filter, context));
    }
}
```

Rationale:

- Keep `fetch(...)` as the backward-compatible small-report path.
- Give existing JSON/REST/dot-out datasources a default list-backed cursor immediately.
- Override `openCursor(...)` in `MogJdbcDataSource` for true JDBC streaming.
- Use `next()/current()` instead of `Iterator<MogFetchable>` because `Iterator.next()` cannot throw checked `MogException`.
- Require datasource-owned cleanup: `MogJdbcDataSource.openCursor(...)` returns a cursor whose `close()` closes `ResultSet`, `Statement`/`PreparedStatement`, and `Connection` in reverse order.

## 5. JDBC streaming design

JDBC streaming path:

```text
MogJdbcDataSource.openCursor(filter, context)
  -> resolve URL/user/password/driver
  -> parse named parameters into PreparedStatement SQL
  -> open Connection
  -> configure read-only/forward-only/fetch-size where supported
  -> execute PreparedStatement
  -> return MogJdbcFetchCursor

StreamingXLSXTableWriter
  -> try (MogFetchCursor cursor = dataSource.openCursor(...)) {
       write header
       while (cursor.next()) {
           write one MogFetchable row to SXSSFSheet
       }
       finalize range metadata from final row count
     }
```

Concrete JDBC recommendations:

- Use `PreparedStatement`, not `Statement`, in the streaming path.
- Use `ResultSet.TYPE_FORWARD_ONLY` and `ResultSet.CONCUR_READ_ONLY`.
- Add datasource options for fetch sizing:

```json
{
  "name": "sales_db",
  "type": "jdbc",
  "url": "${SALES_JDBC_URL}",
  "user": "${SALES_USER}",
  "password": "${SALES_PASS}",
  "fetchSize": 1000,
  "queryTimeoutSeconds": 300
}
```

- Default `fetchSize` should be conservative and driver-neutral. Permit report/datasource authors to tune for SQL Server/H2/SQLite behavior.
- Keep offset/limit behavior initially compatible, but move it behind query preparation. The current `addOffset/addLimit` string append is dialect-specific and should not expand during streaming work.
- Preserve column-name lowercasing for backward compatibility unless a future compatibility flag changes it.

Exact blockers today:

- `MogJdbcDataSource.fetch(...)` closes JDBC resources before returning, forcing materialization.
- Current table writer asks for `data.size()` before creating the table area.
- `XSSFTable` is created before row writing and requires a known `AreaReference`.
- Chart and pivot writers discover tables from XSSF workbook/sheet state after table creation.
- Current writer types are XSSF-specific and cannot accept `SXSSFWorkbook`/`SXSSFSheet`.

## 6. Parameterized-query DSL proposal

Recommended DSL:

```json
{
  "type": "query",
  "query": "select * from claim where cycle_id = :cycleId and id > :afterId",
  "parameters": {
    "cycleId": "${cycleId}",
    "afterId": "${afterId}"
  }
}
```

Optional explicit JDBC typing for nulls or ambiguous strings:

```json
{
  "type": "query",
  "query": "select * from claim where cycle_id = :cycleId and created_at >= :fromDate",
  "parameters": {
    "cycleId": { "value": "${cycleId}", "jdbcType": "BIGINT" },
    "fromDate": { "value": "${fromDate}", "jdbcType": "DATE" }
  }
}
```

Model additions:

```java
public class MogQueryFilter extends MogDataFilter {
    private String query;
    private Map<String, MogQueryParameter> parameters = new LinkedHashMap<>();
}

public class MogQueryParameter {
    private Object value;
    private String jdbcType; // optional java.sql.JDBCType name
}
```

Parameter resolution rules:

- Parse `:name` tokens outside string literals, quoted identifiers, line comments, and block comments.
- Replace each token with `?`.
- Preserve an ordered list of parameter names, including repeats.
- Fail fast if the query references a name not present in `parameters`.
- Fail fast if `parameters` contains unused names, unless a future `allowUnusedParameters` flag is added.
- Do not substitute parameter values into SQL text.

Value typing:

- If parameter value is exactly a single placeholder like `"${cycleId}"`, resolve from `MogContext.getVariables()` and retain the original Java object type.
- If parameter value is an embedded string like `"prefix-${cycleId}"`, resolve through string substitution and bind as `String`.
- Environment fallback remains string-valued.
- JSON request params arriving through `RunRequest.params` should retain Jackson's Java types (`Integer`, `Long`, `Double`, `Boolean`, `String`, lists/maps as rejected unless explicitly supported).
- For `null`, require explicit `jdbcType` or bind with `Types.NULL` as a fallback with a documented portability warning.

Binding:

```java
PreparedSql prepared = NamedSqlParser.parse(query);
PreparedStatement ps = connection.prepareStatement(
        prepared.sql(),
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY);

for (int i = 0; i < prepared.parameters().size(); i++) {
    Object value = resolver.resolve(prepared.parameters().get(i), filter.getParameters(), context);
    binder.bind(ps, i + 1, value, optionalJdbcType);
}
```

## 7. XLSX feature compatibility matrix

Apache POI references used:

- SXSSF is a streaming extension of XSSF that keeps only a row window in memory and flushes older rows to disk: https://poi.apache.org/components/spreadsheet/
- `SXSSFWorkbook` notes that merged regions/comments can still consume memory, inline strings are default, shared strings use more memory, row window controls accessibility, temp files must be disposed, and Zip64 is always default since POI 5.0.0: https://poi.apache.org/apidocs/dev/org/apache/poi/xssf/streaming/SXSSFWorkbook.html
- `SXSSFSheet` supports merged regions, column widths, autofilters, drawings, and autosizing with tracking, but `shiftRows` is not implemented: https://poi.apache.org/apidocs/4.0/org/apache/poi/xssf/streaming/SXSSFSheet.html
- POI documents XSSF chart support and pivot support as limited: https://poi.apache.org/components/spreadsheet/limitations.html

| Existing feature | Current implementation | Streaming classification | Notes |
| --- | --- | --- | --- |
| Workbook creation/save | `XSSFWorkbook` | Supported directly with separate writer | Use `SXSSFWorkbook(rowWindowSize)` and always `dispose()` in `finally`. Do not replace XSSF globally. |
| Sheet creation | `XSSFSheet` | Supported directly | `SXSSFWorkbook.createSheet(...)` returns `SXSSFSheet`. |
| Row/cell values | `XSSFRow`/`XSSFCell` | Supported directly with adaptation | Use `org.apache.poi.ss.usermodel.Row/Cell` or SXSSF-specific types. |
| Numeric/boolean/string cell values | `setCellValue(...)` | Supported directly | Keep current coercion behavior. Watch Excel 32,767-character cell text limit separately. |
| Column widths | `sheet.setColumnWidth(...)` | Supported directly | Current fixed widths are fine. |
| Table headers | XSSF table columns plus header row cells | Supportable with adaptation | Header cells are easy; XSSF table column metadata is not. |
| `XSSFTable` creation | `sheet.createTable(area)` before writing rows | Incompatible/problematic | `SXSSFSheet` does not expose the same XSSF table workflow. Current design also requires final row count before table creation. |
| `AreaReference` based on `data.size()` | Known before writing because list is materialized | Supportable with adaptation | Track rows written and compute final range after cursor drains. This supports autofilter/ranges, but not necessarily `XSSFTable`. |
| Table style metadata | `XSSFTableStyleInfo`/CT table style | Incompatible/problematic | Depends on `XSSFTable`. Streaming mode should initially reject table styles or degrade to cell styles only by explicit policy. |
| Table autofilter | CT table autofilter | Supportable with adaptation | Use `Sheet.setAutoFilter(CellRangeAddress)` after final row is known. This is range autofilter, not table autofilter. |
| Cell styles | `XLSXCellStyle.xssfCellStyle(XSSFWorkbook)` | Supportable with adaptation | Change future style factory to accept `Workbook` and return `CellStyle`; cache styles per workbook. |
| Column-level `CellStyle` | Direct `CellStyle` field on `Column` | Supported directly | It is not currently a clean JSON DSL feature; preserve existing object model behavior. |
| Merged/spanned regions | `addMergedRegion(...)` | Supported directly, bounded use only | POI keeps merged regions in memory; acceptable for sparse headers/text, not for per-row merges. |
| Spanned text | Merged region + row/cell + style | Supported directly with adaptation | Must ensure spanned text rows are written before flushed rows need mutation. |
| Charts with explicit ranges | XSSF/XDDF drawing/chart | Requires further experiment | `SXSSFSheet.createDrawingPatriarch()` exists, but current writer requires XSSF types. Need proof with POI 5.4.0 before supporting. |
| Charts backed by tables | Finds `XSSFTable`, derives column ranges | Incompatible/problematic initially | Without `XSSFTable`, derive ranges from streaming table registry after row count is known. Chart creation likely must occur after table data is complete. |
| Pivot tables | `XSSFPivotTable` from `XSSFTable` | Incompatible/problematic initially | XSSF pivot creation is limited and table-dependent. Reject in streaming mode until a hybrid/post-process experiment succeeds. |
| Formula creation | No current production writer found | Requires further experiment | SXSSF can write formula strings, but formula evaluation is not supported for flushed rows. |
| Formula evaluation | Not current | Incompatible/problematic | Do not evaluate formulas in streaming mode. |
| Autosize columns | Not current | Supportable with adaptation | Requires tracking columns before rows are written and can be expensive. |

## 8. Proposed streaming XLSX architecture

Do not change the existing `XLSXReportWriter` path. Add an opt-in streaming path:

```text
ReportWriterService
  "xlsx" -> XLSXReportWriterSelector
      normal mode    -> existing XLSXReportWriter
      streaming mode -> StreamingXLSXReportWriter
```

Alternative: keep `ReportWriterService` registering `"xlsx"` to a selector/wrapper that checks `XLSXReport.getXlsx().getMode()`. This preserves `type: "xlsx"` and old reports.

New writer classes:

```text
org.copperforge.mog.reporting.xlsx.streaming.StreamingXLSXReportWriter
org.copperforge.mog.reporting.xlsx.streaming.StreamingXLSXTableWriter
org.copperforge.mog.reporting.xlsx.streaming.StreamingXLSXSpannedTextWriter
org.copperforge.mog.reporting.xlsx.streaming.StreamingSheetContext
org.copperforge.mog.reporting.xlsx.streaming.StreamedTableRangeRegistry
```

Core behavior:

- `StreamingXLSXReportWriter` creates `SXSSFWorkbook` with configured row window, compression, and shared-string mode.
- It writes sheets/elements in declared order.
- For streaming tables, it writes header, opens a cursor, writes one row at a time, tracks final row count, and registers the final range by table name.
- It does not create `XSSFTable` in phase 1.
- It applies `Sheet.setAutoFilter(...)` to the final rectangular range if enabled.
- It rejects unsupported elements in streaming mode with precise `MogException` messages rather than silently falling back to XSSF.
- It closes/disposes SXSSF temp files in `finally`.

Initial supported streaming subset:

- Sheets.
- Tables with explicit columns, fixed widths, primitive cell values, and range autofilters.
- Sparse spanned text/merged regions, provided they are not placed over rows that have already flushed.

Initial rejected subset:

- Pivot tables.
- Table styles that require actual Excel table objects.
- Table-backed charts.
- Explicit-range charts until a POI 5.4.0 experiment proves creation works with SXSSF.

## 9. Proposed DSL changes

Default remains normal XSSF mode:

```json
{
  "name": "Small Report",
  "type": "xlsx",
  "filename": "small-${timestamp}.xlsx"
}
```

Opt-in streaming:

```json
{
  "name": "Large Claim Report",
  "type": "xlsx",
  "filename": "large-claim-${timestamp}.xlsx",
  "xlsx": {
    "mode": "streaming",
    "rowAccessWindowSize": 500,
    "compressTempFiles": true,
    "useSharedStringsTable": false,
    "unsupportedFeaturePolicy": "fail"
  }
}
```

Recommended model:

```java
public class XLSXReport extends Report {
    private List<XLSXStyle> styles;
    private XLSXOptions xlsx = new XLSXOptions();
}

public class XLSXOptions {
    private String mode = "normal"; // normal | streaming
    private Integer rowAccessWindowSize = 500;
    private Boolean compressTempFiles = true;
    private Boolean useSharedStringsTable = false;
    private String unsupportedFeaturePolicy = "fail";
}
```

Do not silently change existing reports to streaming. Do not use `type: "xlsx-streaming"` as the main DSL because it conflates file format and writer mode, though registering it as an alias for tests could be acceptable later.

## 10. Async run architecture proposal

Current `RunService.execute(...)` is synchronous. It creates the run directory and metadata, runs MOG inline, marks completed/failed, and returns. `ApiController.startRun(...)` calls it directly.

Later async design for `mog-api` only:

- Keep `mog-core` Spring-free.
- Introduce `RunExecutionService` in `mog-api`.
- Configure a bounded `ThreadPoolTaskExecutor` or Java `ThreadPoolExecutor` with:
  - fixed/small max pool size,
  - bounded queue,
  - explicit rejection behavior that marks or returns `429/503`,
  - no unbounded executor.
- Change `POST /api/runs` to create metadata with queued/running status and return immediately with `runId`.
- Worker updates metadata transitions: `QUEUED -> RUNNING -> COMPLETED/FAILED/CANCELLED`.
- Store artifact only under the run directory.
- Add cancellation later by interrupting workers and ensuring datasource cursors and SXSSF workbooks close on interruption.
- Keep CLI synchronous unless explicitly enhanced; existing CLI behavior must continue.

Acceptance for later async work should include race-safe metadata persistence, queue saturation tests, failure persistence tests, and no executor creation in `mog-core`.

## 11. Testing strategy

Datasource tests:

- `MogDataSource.openCursor(...)` default list-backed cursor preserves `fetch(...)` behavior.
- `MogJdbcDataSource.openCursor(...)` streams H2 rows without accumulating a list.
- Cursor closes `ResultSet`, `PreparedStatement`, and `Connection` on normal completion and on writer-thrown exception.
- Offset/limit compatibility tests for existing `fetch(...)`.
- Named-parameter parser tests for repeated parameters, missing parameters, unused parameters, string literals, quoted identifiers, line comments, block comments, and adjacent casts/syntax.
- Parameter binding tests for `Integer`, `Long`, `BigDecimal`, `Boolean`, `String`, date/time values, and typed nulls.

XLSX tests:

- Existing XSSF tests remain unchanged and green.
- Streaming report with a small H2 datasource produces an XLSX readable by `XSSFWorkbook`.
- Streaming table writes expected row count and cell values.
- Streaming range autofilter covers header through final row.
- SXSSF temp files are disposed after success and failure.
- Unsupported feature tests assert clear failures for pivot tables/table styles/table-backed charts.
- Large-row smoke test verifies bounded heap behavior and no intermediate collection in the JDBC streaming writer.

API tests:

- Existing synchronous run tests remain green until async phase.
- Later async phase adds queued/running/completed/failed status transition tests and bounded queue rejection tests.

Manual/diagnostic:

- Reproduce the observed `too many bytes` failure with full stack trace before making claims about origin.
- For SXSSF chart/table experiments, inspect generated XLSX with Excel/LibreOffice and POI reopen tests.

## 12. Ordered implementation phases

### Phase 1: Streaming cursor contract, list-backed default

Changes:

- Add `MogFetchCursor`.
- Add `MogDataSource.openCursor(...)` default implementation backed by existing `fetch(...)`.
- Add tests for default cursor behavior and close idempotency.

Acceptance criteria:

- `./gradlew :mog-core:test --console=plain` passes.
- No production behavior changes for existing reports.
- No JDBC streaming implementation yet.

### Phase 2: Named SQL parser and parameter model

Changes:

- Add `parameters` to `MogQueryFilter`.
- Add named SQL parser producing JDBC SQL plus ordered bindings.
- Add typed parameter resolver that preserves exact `MogContext` Java values for single-placeholder values.
- Add binder using `PreparedStatement`.

Acceptance criteria:

- Parser/binder unit tests cover literals/comments/repeated names/nulls.
- No current unparameterized query behavior changes.
- No string substitution for SQL values.

### Phase 3: JDBC cursor implementation

Changes:

- Override `MogJdbcDataSource.openCursor(...)`.
- Use `PreparedStatement`, forward-only/read-only result set, optional fetch size/query timeout.
- Ensure datasource cursor owns and closes all JDBC resources.
- Keep `fetch(...)` by draining `openCursor(...)` into a list, or keep existing `fetch(...)` temporarily and test parity.

Acceptance criteria:

- H2 streaming tests pass.
- Cleanup tests prove close on success and failure.
- Existing JDBC `fetch(...)` tests pass.

### Phase 4: Streaming XLSX writer skeleton

Changes:

- Add `XLSXOptions` DSL model.
- Add selector/wrapper so `type: "xlsx"` and absent options use existing XSSF writer.
- Add `StreamingXLSXReportWriter` with sheet creation, save, close, and SXSSF `dispose()`.
- Add unsupported-feature validation.

Acceptance criteria:

- Existing reports still use normal mode by default.
- Streaming mode with no elements produces valid XLSX.
- Unsupported elements fail clearly.

### Phase 5: Streaming table writer

Changes:

- Add `StreamingXLSXTableWriter`.
- Write headers and rows from `MogFetchCursor` without intermediate collection.
- Track final row range and apply range autofilter.
- Preserve fixed widths and primitive cell coercion.

Acceptance criteria:

- Streaming H2 report writes expected XLSX.
- Test proves JDBC cursor rows are consumed one at a time.
- No `List<MogFetchable>` materialization in streaming table path.

### Phase 6: Streaming styles and spanned text

Changes:

- Adapt style creation to `Workbook` interface.
- Support cell styles and sparse spanned text/merged regions in streaming mode.
- Document or enforce restrictions for merged regions over flushed rows.

Acceptance criteria:

- Existing XSSF style tests pass.
- Streaming style/spanned text tests pass.
- Memory-risk behavior is documented and guarded.

### Phase 7: SXSSF compatibility experiments for charts/tables/pivots

Changes:

- Create isolated experiments/tests for explicit-range charts, table-like metadata, and pivot creation strategies.
- Do not enable production support until generated workbooks are verified.

Acceptance criteria:

- Each feature has a documented support decision.
- Unsupported features remain fail-fast.

### Phase 8: Async API execution

Changes:

- Add bounded executor in `mog-api`.
- Split run creation/submission from execution.
- Add queued/running statuses if needed.

Acceptance criteria:

- `POST /api/runs` returns promptly with accepted run id.
- Queue saturation is bounded and observable.
- Existing artifact download and metadata APIs still work.
- `mog-core` remains Spring-free.

## 13. Risks/open questions

- The supplied spec file was not available in the workspace, so any spec-specific nuance still needs review against this document.
- Need the full observed `too many bytes` stack trace before attributing the failure.
- SXSSF table support is the largest XLSX compatibility risk. Current MOG charts/pivots are table-centered.
- Range autofilter is not equivalent to Excel table metadata; users expecting styled/filterable Excel tables may see a deliberate streaming-mode difference.
- Shared strings can improve client compatibility but may reintroduce large memory growth for text-heavy reports.
- Merged regions are stored in memory by SXSSF/POI; streaming mode should not support per-row merged regions for large datasets.
- JDBC fetch-size behavior differs by driver. SQL Server, H2, and SQLite need driver-specific verification.
- Existing coordinate validation and older sample resources disagree on one-based coordinates; do not broaden streaming scope until this compatibility behavior is decided.

## 14. Initial assessment

Assessment: CONDITIONAL GO.

Reasons:

- The core target path is architecturally feasible: JDBC `ResultSet` can be exposed as a datasource-owned closeable cursor, and SXSSF can write rows with bounded row-window memory and temp-file spooling.
- Existing small reports can remain on the current XSSF/list-backed `fetch(...)` path.
- The implementation should be phased and opt-in.

Conditions before full implementation:

- Review this design against the missing supplied spec.
- Capture and inspect the full `too many bytes` stack trace.
- Accept that initial streaming XLSX mode will support table-shaped row output plus range autofilter, not full `XSSFTable`, pivots, or table-backed charts.
- Run a small SXSSF proof for any chart support before committing to chart compatibility.
