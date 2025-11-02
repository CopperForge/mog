# 🪐 MOG — It is what you make it
<img src="src/main/resources/assets/png/moglet.png" alt="Mog Mascot" width="200" />

A small, batteries-included CLI to define data-driven reports and export them (XLSX), plus simple AES encryption/decryption utilities for managing secrets.  
Built with Java 21 and Picocli; ships as a fat jar and optional RPM.

---

## ⚙️ Features
- Report generation from `.mog` definition files (see examples under `src/main/examples/reports`).
- XLSX writer with tables, styles, and pivot tables.
- Flexible data sources: file / JSON / REST / JDBC / dotout.
- CLI subcommands via Picocli: `report generate`, `encrypt`, `decrypt`.
- Configurable search paths and site config; supports per-user secrets file.
- Cross-platform launchers (`src/main/bin/mog`, `src/main/bin/mog.cmd`) and RPM packaging.

---

## 🧰 Requirements
- **Java 21 (JDK)** – The Gradle wrapper is included; no local Gradle install required.  
- **OS:** Windows, Linux, or macOS. (For RPM packaging, build on Linux.)

---

## 🚀 Quickstart

Build the fat jar:
```bash
./gradlew clean build
