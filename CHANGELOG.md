# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Changed
- DSL coordinates are now 1‑based (rows and columns) to align with Excel. Example: A1 is `{ row: 1, col: 1 }` and the first table column is `column: 1`.

### Fixed
- Eliminated Excel “Repaired Records” warnings by:
  - Offsetting table header cell writes to match the table’s upper‑left column.
  - Sanitizing table display names to valid Excel identifiers.
  - Validating chart series/category column indices against table width.
- Stabilized chart legend handling: legend is now removed after plotting
  the series (instead of before) to avoid POI flakiness that could throw
  IndexOutOfBoundsExceptions in some environments.

### Added
- Sample JSON dataset (`src/main/examples/reports/sales.json`) and updated `chart.sample.report.mog` to use it via JSONPath `$.data[*]`.

### Build
- Added dependencies required by existing code paths: `commons-lang3`, `commons-codec`.
