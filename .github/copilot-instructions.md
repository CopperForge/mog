# Copilot Instructions for MOG

## Project Overview

MOG is a batteries-included CLI tool for generating data-driven reports (primarily XLSX) with support for tables, charts, and pivot tables. It includes AES encryption/decryption utilities for managing secrets. Built with Java 21 and Picocli, it ships as a fat jar and optional RPM.

## Technology Stack

- **Language**: Java 21
- **Build Tool**: Gradle 8.8 (use the wrapper: `./gradlew`)
- **CLI Framework**: Picocli 4.7.6
- **Testing**: JUnit 5
- **Logging**: SLF4J with Logback
- **Excel Library**: Apache POI 5.4.0

## Project Structure

- `src/main/java/org/copperforge/mog/` - Main source code
  - `reporting/` - Report generation logic (XLSX, charts, tables, pivot tables)
  - `security/` - Encryption/decryption commands
  - `config/` - Configuration management
  - `data/` - Data source handling (JSON, JDBC, REST, file)
- `src/main/resources/` - Resources including assets and examples
- `src/main/examples/reports/` - Sample report definitions
- `src/test/java/` - Test suites
- `src/install/` - Installation resources (RPM, config templates)
- `etc/` - Runtime configuration directory (created at runtime)

## Build and Test Commands

### Build
```bash
./gradlew clean build
```

### Run Tests
```bash
./gradlew test
```

### Run Sample Report
```bash
./gradlew sampleReport
```

### Create Distributions
```bash
./gradlew installDist    # Local install under build/install/mog
./gradlew distZip        # Create ZIP distribution
./gradlew distTar        # Create TAR.GZ distribution
./gradlew rpm            # Create RPM (Linux only)
```

## Coding Standards

### Java Conventions
- **Java Version**: Always use Java 21 features and APIs
- **Naming**: Use descriptive names; follow standard Java camelCase conventions
- **Logging**: Use SLF4J logger pattern: `private static Logger log = LoggerFactory.getLogger(ClassName.class);`
- **Null Safety**: Check for null before dereferencing; use Optional where appropriate
- **Exceptions**: Throw meaningful exceptions with descriptive messages

### Coordinates and Indexing
**CRITICAL**: All coordinates in MOG are **1-based** to match Excel conventions:
- A1 corresponds to `{ "row": 1, "col": 1 }`
- B2 corresponds to `{ "row": 2, "col": 2 }`
- First table column is `column: 1`, second is `column: 2`, etc.

Never use 0-based indexing for user-facing coordinates or Excel operations.

### Report Definition Files
- Reports are defined in `.mog` files (JSON format)
- Must include: `name`, `type`, `filename`, `sheets`
- Data sources can be: `json`, `jdbc`, `api`, `file`, `dotout`
- Elements include: `table`, `chart`, `pivotTable`, `spannedText`
- All file paths should support variable substitution: `${MOG_HOME}`, `${timestamp}`, environment variables

### Testing
- Use JUnit 5 (`@Test`, `@BeforeEach`, `@AfterEach`)
- Test classes should be named `*Test.java`
- Integration tests should be named `*IntegrationTest.java`
- Place test resources in `src/test/resources/`
- Validate XLSX output by reading it back with Apache POI
- Test both success and error cases

## Environment Variables

- `MOG_HOME` - Installation root directory
- `MOG_ETC` - Configuration directory (defaults to `${MOG_HOME}/etc`)
- `MOG_ENV` - Environment name (e.g., dev, test, prod)
- `JAVA_HOME` - JDK installation path
- `MOG_HEAP_MB` - JVM heap size override
- `MOG_JAVA_OPTS` - Additional JVM options

## Configuration

### Site Configuration
Default location: `${MOG_HOME}/etc/config.mog`

### Data Sources Catalog
Locations (in priority order):
1. `--datasources=<path>` command-line option
2. `${MOG_ETC}`
3. `${MOG_HOME}/etc`

File patterns (loaded in order, later overrides earlier):
- `datasources.mog`
- `datasources/*.mog`
- Environment-specific: `datasources.<env>.mog`, `datasources/<env>/*.mog`

## Common Pitfalls

### Excel Repair Warnings
- Always validate table ranges (row/col ≥ 1)
- Sanitize table display names (no special chars)
- Validate chart column indices against table width
- Ensure data source names exist before referencing them

### Data Source Resolution
- Inline report `dataSources` take precedence over catalog entries
- Later-loaded catalog files override earlier ones
- Use `${VAR}` syntax for environment variable substitution
- Log datasource overrides with old and new source paths

### Path Resolution
Report paths are resolved in this order:
1. Absolute path
2. Relative to current working directory
3. Relative to `--working-dir` (if provided)
4. Relative to `MOG_ETC`, then `MOG_HOME`
5. Search configured report paths in `config.mog`

## Making Changes

### Adding New Features
1. Follow the existing package structure
2. Add appropriate logging at INFO and DEBUG levels
3. Write unit tests for new functionality
4. Update documentation (README.md, INSTALL.md if relevant)
5. Ensure backward compatibility with existing report definitions

### Modifying Excel Writers
- Changes to `XLSXTableWriter`, `XLSXChartWriter`, `XLSXPivotTableWriter` must maintain 1-based indexing
- Validate output by generating test workbooks and opening in Excel
- Test with edge cases: empty tables, single-row tables, maximum column counts
- Always validate ranges to avoid "Repaired Records" messages

### Adding Data Source Types
1. Implement in `org.copperforge.mog.data` package
2. Register in data source factory/resolver
3. Support variable substitution for sensitive fields
4. Add catalog and inline configuration examples
5. Document in README.md under appropriate sections

## Security Considerations

- Never commit secrets or passwords to source control
- Use `${VAR}` variable substitution for sensitive values
- Encryption/decryption commands are in `org.copperforge.mog.security`
- Support per-user secrets files
- Secrets should be loaded from environment or secure storage

## Package Dependencies

Avoid adding unnecessary dependencies. Current major dependencies:
- Apache POI (Excel generation)
- Jackson (JSON processing)
- Picocli (CLI framework)
- Commons libraries (IO, Compress, Lang3, Codec)
- SLF4J/Logback (Logging)
- Database drivers (H2, SQLite, SQL Server)
- JsonPath (JSON querying)

When adding dependencies, ensure:
- No conflicts with existing versions
- Compatible with Java 21
- No security vulnerabilities
- Properly scoped (implementation vs. testImplementation)

## Mascot: Moglet

The project mascot is **Moglet** (see `src/main/resources/assets/png/moglet.png` and `DESIGN_NOTES.md`):
- Bright neon-lime fuzzy creature with three RGB eyes
- Wears a midnight-blue jumpsuit with an "M" insignia
- Never modify the mascot image without understanding the design specifications
- See DESIGN_NOTES.md for the full (humorous) specification

## Additional Resources

- **README.md** - Usage examples, quickstart, feature overview
- **INSTALL.md** - Installation procedures for all platforms
- **DESIGN_NOTES.md** - Mascot design specifications (mostly for fun)
- **CHANGELOG.md** - Version history and breaking changes
- Sample reports: `src/main/examples/reports/`
