# MOG — It is what you make it

A small, batteries‑included CLI to define data‑driven reports and export them (XLSX), plus simple AES encryption/decryption utilities for managing secrets. Built with Java 21 and Picocli; ships as a fat jar and optional RPM.

## Features

- Report generation from `.mog` definition files (see examples under `src/main/examples/reports`).
- XLSX writer with tables, styles, and pivot tables.
- Flexible data sources: file/JSON/REST/JDBC/dotout.
- CLI subcommands via Picocli: `report generate`, `encrypt`, `decrypt`.
- Configurable search paths and site config; supports per‑user secrets file.
- Cross‑platform launchers (`src/main/bin/mog`, `src/main/bin/mog.cmd`) and RPM packaging.

## Requirements

- Java 21 (JDK). The Gradle wrapper is included; no local Gradle install required.
- Windows, Linux, or macOS. For RPM packaging, build on Linux.

## Quickstart

Build the fat jar:

```bash
./gradlew clean build
```

Run the CLI directly from the jar (uses the sample config):

```bash
java -jar build/libs/mog.jar --config src/install/resources/config.mog --help
```

Generate the sample CCR report (writes an `.xlsx` file):

```bash
java -jar build/libs/mog.jar --config src/install/resources/config.mog \
  report generate --report src/main/examples/reports/ccr/ccr.report.mog
```

You should see a log message with the output file path.

### Using the launch scripts

- Linux/macOS:
  1) Set `MOG_HOME` to your install root and ensure `mog.jar` is under `"$MOG_HOME/lib"`.
  2) Run `src/main/bin/mog`.

- Windows:
  1) Set `MOG_HOME` to your install root and ensure `mog.jar` is under `%MOG_HOME%\lib`.
  2) Run `src/main/bin/mog.cmd`.

Example:

```bash
export MOG_HOME=$PWD/build/install/mog
# or put mog.jar at $MOG_HOME/lib/mog.jar and copy bin scripts
$MOG_HOME/bin/mog --config $MOG_HOME/etc/config.mog --help
```

You can also produce an application distribution with launchers using the Gradle Application Plugin:

```bash
./gradlew installDist
# Launchers under build/install/mog/bin
```

## Configuration

By default, MOG loads its site configuration from `--config` (if provided) or from `${MOG_HOME}/etc/config.mog`.

Sample config: `src/install/resources/config.mog`. It shows search paths for commands and reports, optional DevOps metadata, and includes support for additional config fragments via `includes`.

Security note: Do not commit real tokens or credentials. Replace any tokens in example configs with placeholders before sharing publicly.

### Per‑user secrets (`.mog`)

MOG can read a per‑user secrets file to find encryption settings and API tokens. The loader checks:

1) `${user.home}/.mog`
2) `${MOG_HOME}/.mog`

Minimal example (`~/.mog`):

```json
{
  "encryptionPassword": "change-me-strong-passphrase"
}
```

## CLI Overview

Top‑level command: `mog`

Global options (subset):

- `--config <path>`: Path to site config (defaults to `${MOG_HOME}/etc/config.mog`).
- `--working-dir <path>`: Working directory to execute the command.
- `-h`, `--help`: Show help.

### Reports

Generate a report from a definition file:

```bash
mog report generate --report <path-to-report.mog>
```

Examples are available under:

- `src/main/examples/reports/ccr/ccr.report.mog`
- `src/main/examples/reports/fin_checks.report.mog`
- `src/main/examples/reports/msgs.report.mog`

The XLSX writer saves to a filename derived from the report definition.

### Encryption

Encrypt a value:

```bash
mog encrypt --value "secret" [--password "passphrase"]
```

Decrypt a value:

```bash
mog decrypt --value "<cipher-text>" [--password "passphrase"]
```

If `--password` is omitted, MOG falls back to the `encryptionPassword` in your `.mog` file.

## Build and Package

- Fat jar: `./gradlew clean build` → `build/libs/mog.jar`
- App distribution: `./gradlew installDist` → `build/install/mog/`
- RPM (Linux): `./gradlew rpm` (packs `bin/` and `lib` under `/opt/mog` and config under `/etc` per `build.gradle`).

Java toolchain is configured for Java 21; the Gradle wrapper will provision it if needed.

## Project Structure

- Entry point: `src/main/java/org/copperforge/mog/Mog.java`
- Global options: `src/main/java/org/copperforge/mog/MogOptions.java`
- Reporting CLI: `src/main/java/org/copperforge/mog/reporting/ReportingCommand.java`
- Report options: `src/main/java/org/copperforge/mog/reporting/ReportOptions.java`
- XLSX writer: `src/main/java/org/copperforge/mog/reporting/xlsx/`
- Security commands: `src/main/java/org/copperforge/mog/security/`
- Config models: `src/main/java/org/copperforge/mog/config/`
- Example configs: `src/install/resources/` and `src/main/examples/`

## Troubleshooting

- Could not determine encryption key: Provide `--password` or configure `~/.mog` with `encryptionPassword`.
- Config not found: Set `--config` explicitly or define `MOG_HOME` so the default `${MOG_HOME}/etc/config.mog` resolves.
- Java version issues: Ensure `java -version` reports 21+. The Gradle toolchain builds with 21.

## License

See `LICENSE` for details.

---

Contributions and issues are welcome. If you’d like CI, tests, or more report examples added, open an issue or PR.

