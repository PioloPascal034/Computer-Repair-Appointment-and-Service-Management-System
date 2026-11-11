# CRSM Failure-Risk Audit

Date: 2026-11-11  
Scope: JavaFX desktop client, legacy compatibility code, local test/tooling scripts, configuration handling, and SQL migration documentation.  
Database policy: read-only inspection only; no database edits were executed during this audit.

## Executive result

The project builds successfully and all existing local regression suites pass. Four actionable client/tooling risks were fixed:

1. Repair intake now rejects missing device details and malformed appointment dates/times before making a network request.
2. Storage uploads and downloads now have a 60-second request timeout.
3. The legacy JDBC helper no longer contains a dead hard-coded connection; it uses the same `.env`-based configuration as maintenance tooling.
4. The maintenance script now compiles its required support sources directly, so it works from a clean output directory instead of relying on stale class files.

The previously reported service-picker reset was also retained as fixed: the JavaFX intake service selector is non-editable and no longer mutates its item list during selection.

## Validation performed

- JavaFX compilation: passed.
- Email guard checks: passed.
- Workflow contract checks: passed, including new no-network validation checks for invalid intake data.
- Account/photo checks: passed.
- Database maintenance `inspect`: passed read-only; required RPCs and tables were present, and the active profile/link inventory was returned.
- No `apply` action, SQL statement, seed operation, insert, update, delete, or migration was run during this audit.

## Fixed findings

### High — intake failures reached the backend late

`CustomerWorkflowService.submit` previously forwarded null/blank device fields and malformed dates or times to the RPC. This produced avoidable server errors and made the form appear unreliable. Required-field, date, time, customer ID, session, and service ID validation now happens before HTTP submission. Regression coverage was added to `tests/WorkflowChecks.java`.

### Medium — service selection could destabilize the form

The JavaFX service picker used an editable self-filtering combo box that replaced its item list while JavaFX was committing a selection. It is now a stable non-editable combo box in `src/crsm/fx/WorkspaceController.java`.

### Medium — network operations could hang indefinitely

Storage upload/download requests had no per-request timeout. They now fail predictably after 60 seconds in `src/crsm/SupabaseStorageService.java`.

### Medium — legacy JDBC helper was misleading and nonfunctional

`src/DatabaseConnection.java` contained a placeholder host, username, and password. It now delegates to `crsm.DatabaseConfig`, which validates and reads the existing environment configuration.

### Medium — maintenance tooling depended on stale compiled classes

`database-maintenance.ps1` previously compiled only `DatabaseMaintenance.java`, which could fail after a clean build because package support classes were not guaranteed to be available. It now compiles the required support sources explicitly.

## Remaining risks and recommended follow-up

- The JavaFX controller still contains a second, older customer-intake implementation alongside `CustomerRequestPage`. Maintaining two intake paths increases regression risk; consolidate them or add an explicit compatibility test for both.
- The live database has multiple migration files that redefine related functions. Keep a single documented migration order and run migrations in a disposable test project before production changes.
- End-to-end UI automation is not currently present. Add a JavaFX UI test that signs in with a test session, selects a service, fills intake fields, submits, and verifies the success state without using production data.
- Storage tests currently validate image parsing and error propagation, but not a real upload/download round trip. Add that test against a disposable storage bucket.
- The project intentionally keeps `.env` ignored. Continue treating all values in it as secrets and rotate any credential that has been pasted into chat, tickets, screenshots, or logs.

## Files changed by this audit

- `src/crsm/CustomerWorkflowService.java`
- `src/crsm/SupabaseStorageService.java`
- `src/DatabaseConnection.java`
- `database-maintenance.ps1`
- `tests/WorkflowChecks.java`
- `docs/audit-report-2026-11-11.md`

No database state was changed.
