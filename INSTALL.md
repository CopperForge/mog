**Overview**
- MOG requires Java 21. Distributions ship with cross‑platform launchers: `bin/mog` (Unix) and `bin/mog.cmd` (Windows).
- Use the Gradle wrapper to build and to generate ready‑to‑run distributions.

**Prerequisites**
- Java 21 (JDK). Ensure `java -version` reports 21.x.
- On Windows, ensure `java.exe` is on PATH or set `JAVA_HOME`.

**Build From Source**
- Clean build the project jar and distributions:
  - `./gradlew clean build` (Windows: `.\gradlew.bat clean build`)
- Optional: run tests only:
  - `./gradlew test`

**Install (Local Directory)**
- Create an installable directory under `build/install/mog`:
  - `./gradlew installDist`
- Add the launcher to PATH (Unix/macOS):
  - `export PATH="$PWD/build/install/mog/bin:$PATH"`
- On Windows (PowerShell):
  - `$env:Path = "$PWD\build\install\mog\bin;" + $env:Path`

**Install (Zip/Tar Distribution)**
- Build archives:
  - `./gradlew distZip` (Zip at `build/distributions/mog-<ver>.zip`)
  - `./gradlew distTar` (Tar.gz at `build/distributions/mog-<ver>.tar.gz`)
- Unpack to a destination folder, e.g., `/opt/mog` or `C:\mog`.
- Set `MOG_HOME` to that folder and add `bin/` to PATH.

**Install (RPM, Linux)**
- Build RPM (on Linux):
  - `./gradlew build rpm`
- Install RPM (example):
  - `sudo rpm -Uvh build/distributions/*.rpm`
- Files install under `/opt/mog` with launchers in `/opt/mog/bin`.

**Environment Variables**
- `MOG_HOME` (recommended): install root. Auto‑detected by launchers if not set.
- `JAVA_HOME`: JDK install (optional if `java` on PATH).
- `MOG_HEAP_MB`: override app heap (MB) for launchers; otherwise auto‑sized.
- `MOG_JAVA_OPTS`: extra JVM options passed through by launchers.

**First‑Run Configuration**
- Minimal site config is expected at `${MOG_HOME}/etc/config.mog`.
- Seed sample config, datasources, and data via Gradle:
  - `./gradlew sampleReport` (seeds `${MOG_HOME}/etc` and generates a sample workbook)
  - With environment profile: `./gradlew sampleReport -Penv=dev`

**Global Datasources (Catalog)**
- Place catalog files in `${MOG_HOME}/etc` (or pass `--datasources=<path>`):
  - `datasources.mog`, `datasources/*.mog`
  - Env: `datasources.<env>.mog`, `datasources/<env>/*.mog` (when `--env` or `MOG_ENV` is set)
- Inline report datasources take precedence; later‑loaded catalog files override earlier by name.

**Run The App**
- From an installed distribution (Unix/macOS):
  - `mog --config=${MOG_HOME}/etc/config.mog report generate --report=chart.sample.report.mog`
- Windows (PowerShell):
  - `.\mog.cmd --config=$env:MOG_HOME\etc\config.mog report generate --report=chart.sample.report.mog`
- With env/catalog:
  - `mog --env=dev --datasources=${MOG_HOME}/etc report generate --report=chart.sample.report.mog`

**Verification**
- Expect an XLSX like `chart-sample-<timestamp>.xlsx` in the working directory.
- If Excel reports “Repaired Records”, update to the latest build; writers validate table ranges, chart bounds, and sanitize display names.

**Uninstall**
- Directory install: remove the unpacked folder and PATH entry.
- RPM: `sudo rpm -e mog` (or use your distro’s package manager).

**Notes**
- Coordinates in report definitions are 1‑based (A1 is `{ row: 1, col: 1 }`).
- Launchers auto‑size JVM heap (25% of RAM, clamped 512–4096 MB). Override with `MOG_HEAP_MB` if needed.
