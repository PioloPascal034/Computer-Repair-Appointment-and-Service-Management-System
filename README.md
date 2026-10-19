# Computer Repair Appointment and Service Management System

“Repairing devices, not relationships.”

## Java Swing application

The application has native Java Swing sign-in and shared sign-up forms, with supplied PNGs retained as visual references rather than used as screen backgrounds. It provides role-specific navigation for Customer, Staff, and Admin accounts. Sign-up does not expose role selection; the additive Supabase SQL setup creates every new profile as `CUSTOMER`. An administrator can promote profiles using a server-side RPC. Profile photo and business-logo uploads use private Supabase Storage buckets. Business-domain tables and forms use a JDBC repository separate from the UI. The existing `lib/postgresql-42.7.12.jar` is the PostgreSQL JDBC driver.

### Build and run (PowerShell)

```powershell
Copy-Item .env.example .env
```

Edit `.env` with your Supabase Project URL and publishable key (from the project's Connect dialog or Settings > API Keys), plus the Session pooler host/username and database password (from Connect > Session pooler and Project Settings > Database). The JDBC URL uses port `5432` and `sslmode=require`. Keep the real `.env` local; it is ignored by Git. The app reads `.env` from the working directory, with process environment variables taking precedence.

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -cp "lib/postgresql-42.7.12.jar" -d out src\Main.java src\*.java src\crsm\*.java
Copy-Item -Recurse -Force src\crsm\assets out\crsm\assets
java -cp "out;lib/postgresql-42.7.12.jar" Main
```

For a new, empty Supabase project, run `docs/supabase_fresh_setup.sql` once in its SQL Editor. It creates the seven business tables, security-invoker report views, customer-first Auth profiles, restricted role-promotion functions, private image buckets, and Staff/Admin technician availability Realtime publication and policy. After creating the account designated as the first administrator, follow the commented bootstrap statement at the end of the script. For a project created with the earlier setup, first review and run `docs/harden_supabase_security.sql`; it applies hardening without dropping business data. Run `docs/enable_technician_realtime.sql` only if technician Realtime still needs enabling afterward. Do not run the older `docs/supabase_auth_profiles_and_storage.sql` as well; that file is the earlier auth-only setup. Keep all database credentials outside source code and version control.

## Scope and limitations

- Sign-in and sign-up call Supabase Auth; accounts are assigned the Customer role by a database trigger. The profile migration, Auth settings, Storage buckets, and initial Admin bootstrap must be configured in Supabase first. A live Supabase connection has not been verified here.
- Customer-owned business records are not loaded because the existing business schema has no authenticated customer-to-account mapping or row-level access rules.
- CRUD forms cover fields in the existing schema. Data outside those tables is not fabricated.
- Profile listing and Admin role promotion use the new profile migration. Avatar and logo uploads are wired to private Storage buckets, but existing uploaded images are not yet loaded into profile/logo previews.
- Staff/Admin technician views subscribe to Supabase Realtime and reload when technician rows change. The Java client reconnects after transient disconnections, but it does not refresh expired Supabase Auth tokens; sign in again if the session expires. Realtime refreshes the list, but database-side assignment locking is not implemented.
- The business-data JDBC connection uses a configured database credential. The current repository does not bind operations to the signed-in Supabase user or enforce role permissions in PostgreSQL. Do not expose this build to untrusted users until database authorization/RLS for the business tables is designed and enforced.
- Estimate approval history, pickup records, payment methods/refunds, and general settings persistence are not implemented because the current schema does not support them.
- The UCD and screen designs include additional statuses and dashboard values that are not represented by the SQL constraints or reporting tables. Existing database status values are used until those conflicts are resolved.
- No Supabase credentials were available for connection verification. Do not describe the live Auth, Storage, or JDBC workflows as tested until verified against the configured project.
- Security Advisor findings for RLS-enabled business tables without policies are intentionally left deny-by-default for Supabase API roles until customer ownership and role-specific access rules are designed. Direct JDBC uses a separate database credential and is not protected by the end-user Auth user's RLS identity.

## Existing prototype classes

`src/LoginFrame.java`, `src/DashboardFrame.java`, `src/DataFrame.java`, `src/RepairRequestFrame.java`, and `src/DatabaseConnection.java` are retained as legacy prototype code. `src/Main.java` now launches the new `crsm` application.
