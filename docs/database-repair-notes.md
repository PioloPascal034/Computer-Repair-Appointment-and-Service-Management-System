# Database repair and demo content ? November 11, 2026

The live database was inspected through the configured JDBC connection. Existing accounts and records were retained. No Auth account was created, deleted, reset or promoted.

## Repaired deployment

The following SQL files were applied successfully:

1. `service_catalog_metadata_migration.sql`
2. `device_specification_migration.sql`
3. `repair_request_pricing_migration.sql`
4. `dashboard_statistics_migration.sql`
5. `repair_status_compatibility_migration.sql`
6. `demo_workspace_seed.sql`

The dashboard RPC and parts table/quote function were missing. A legacy `chk_repair_status` constraint also rejected Awaiting Payment; the compatibility migration replaces it with the current workflow status set. PostgREST was notified to reload its schema.

## Demo content

The repeatable demo seed adds eight clearly marked demonstration devices/repairs, seven sample payments/receipts spread across six months, an upcoming appointment and four sample compatible parts. Demo work is associated with existing linked Customer/Technician records; Auth accounts and existing ownership links are unchanged. No new technical workload is assigned. Part compatibility uses a fictional CRSM Demo brand and Training laptop model. Sample prices are not verified shop prices or live supplier prices. Dashboard totals include demo records, as stated in the interface.

The first seed attempt failed on the old status check and rolled back. It was retried successfully after the compatibility migration.

## Signup behavior

The authentication provider reports signup enabled, email signup enabled and email confirmation required. The profile-creation trigger exists. The screenshot's provider rejection of customer@gmail.com is distinct from the missing database migrations: that address is syntactically valid, but the project cannot guarantee the provider accepts or delivers to a particular inbox.

The signup dialog now stays open on validation/provider errors, preserves entries, prevents duplicate requests while saving, validates usernames/email/passwords, and explains provider email/rate-limit responses. Use an email address you own and confirm it before signing in. Seeding business records does not create login credentials. No live registration email was sent during verification.

## Verification and reruns

Build with `powershell -ExecutionPolicy Bypass -File build-javafx.ps1`, then run local checks with `powershell -ExecutionPolicy Bypass -File test-workflows.ps1`.

`powershell -ExecutionPolicy Bypass -File database-maintenance.ps1 -Action inspect` checks function presence and counts without printing credentials.

`powershell -ExecutionPolicy Bypass -File database-maintenance.ps1 -Action verify` checks dashboard, profile, parts, quote, availability and billing using the real linked identities under the authenticated database role. The Technician payment test uses only a DEMO repair and is rolled back, although the payment sequence can advance. These checks simulate database identity; they do not validate browser/mobile sign-in, SMTP delivery or JWT verification. The anonymous HTTP check confirms the dashboard route exists and denies anonymous access.

For an explicitly requested schema repair or seed, use `-Action apply -SqlFiles` with the specific project SQL paths. Do not apply old full setup scripts to reset this database. All credentials are loaded from the local configuration and must stay out of source control.


## Email provider rate limits

A signup response with `over_email_send_rate_limit` or HTTP 429 is an authentication-provider email/request limit. Database seeding and schema migrations do not clear it. The app never automatically resubmits signup/reset requests. It now preserves `Retry-After` metadata, pauses email requests for the stated interval, or uses a 60-second application pause when no retry interval is provided. This local pause is not a promise that the provider quota has reset. Signup and password-reset share this guard; existing-account sign-in remains available.

For email-based registration, wait for the provider limit to clear and avoid repeated signup/reset clicks. If the limit keeps blocking legitimate registration, the project owner must review or configure authentication email delivery (for example, custom SMTP). No SMTP credentials or management API token are configured by this change; email confirmation and provider limits are not bypassed. No live signup/reset requests were sent while testing this fix; localhost checks cover rate responses and Retry-After handling.
