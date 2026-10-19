# Computer Repair Appointment and Service Management System

“Repairing devices, not relationships.”

## Java Swing application

The application now has a palette-based Swing shell, role-based UI preview navigation, database-backed tables and forms for the seven entities in `docs/create_tables.sql`, and a JDBC repository separate from the UI. The existing `lib/postgresql-42.7.12.jar` is the PostgreSQL JDBC driver.

### Build and run (PowerShell)

```powershell
$env:SUPABASE_JDBC_URL = "jdbc:postgresql://<host>:<port>/<database>?sslmode=require"
$env:SUPABASE_DB_USER = "<database-user>"
$env:SUPABASE_DB_PASSWORD = "<database-password>"

New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -cp "lib/postgresql-42.7.12.jar" -d out src\Main.java src\*.java src\crsm\*.java
java -cp "out;lib/postgresql-42.7.12.jar" Main
```

Supply connection values in the local environment; do not commit credentials. `SUPABASE_JDBC_URL` must use the PostgreSQL JDBC URL format. The repository expects the current seven-table schema to exist in PostgreSQL. The SQL files use MySQL-specific constructs such as `AUTO_INCREMENT`, `CREATE DATABASE`, and `USE`; convert and verify the schema for PostgreSQL before using the application.

## Scope and limitations

- The login screen is a **UI preview**, not authentication. Selecting Customer, Staff, or Admin does not verify identity or securely enforce permissions.
- Customer-owned records are not loaded in Customer preview because the current schema has no authenticated customer-to-account mapping or row-level access rules.
- CRUD forms cover fields in the existing schema. Data outside those tables is not fabricated.
- Account management, customer signup/authentication, photo uploads, estimate approval history, pickup records, payment methods/refunds, and settings persistence are not implemented because the current schema does not support them.
- The UCD and screen designs include additional statuses and dashboard values that are not represented by the SQL constraints or reporting tables. Existing database status values are used until those conflicts are resolved.
- A working Supabase connection has not been verified in this repository environment. Set credentials locally and verify against the agreed PostgreSQL schema before using live data.

## Existing prototype classes

`src/LoginFrame.java`, `src/DashboardFrame.java`, `src/DataFrame.java`, `src/RepairRequestFrame.java`, and `src/DatabaseConnection.java` are retained as legacy prototype code. `src/Main.java` now launches the new `crsm` application.
