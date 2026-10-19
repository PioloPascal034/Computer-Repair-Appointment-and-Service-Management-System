COMPUTER REPAIR APPOINTMENT AND SERVICE MANAGEMENT SYSTEM

The current Java Swing application is launched from src/Main.java. See README.md for build/run steps, local Supabase environment configuration, implemented scope, and unresolved database access requirements.

Sign-in and sign-up use Supabase Auth. New profiles are assigned the Customer role by a database trigger; Admin promotion is performed through a server-side function. Sign-up does not offer role selection. For a new empty Supabase project, run docs/supabase_fresh_setup.sql once in the SQL Editor. It sets up the business tables, reports, account profiles, image buckets, and Staff/Admin technician Realtime subscription. If the project already has the earlier schema, run docs/enable_technician_realtime.sql. Do not also run the older auth-only docs/supabase_auth_profiles_and_storage.sql file.

No live Supabase Auth, Storage, or JDBC connection has been verified. The business-data JDBC repository currently uses a shared configured database credential and is not scoped to the signed-in user's role. Customer records remain unavailable until authenticated ownership and database access controls are implemented.

Staff/Admin technician screens subscribe to Realtime and reload after technician table changes. Refresh-token handling and database-side locking for competing repair assignments are not implemented.
