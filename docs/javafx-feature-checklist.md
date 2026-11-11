# Profile and damage photo verification

Deployment update: see `docs/database-repair-notes.md` for the migrations and demo content applied on November 11, 2026, plus the live database verification scope.

Apply `docs/profile_and_photo_access_migration.sql` after `docs/customer_workflow_migration.sql` in the configured Supabase database before launching. This adds self-only account RPCs and restricts both attachment metadata and private storage reads. No database migration was applied automatically.

Build: `powershell -ExecutionPolicy Bypass -File build-javafx.ps1`.

Local HTTP contract checks: compile `tests/AccountPhotoChecks.java` with `javac -encoding UTF-8 -cp out-fx -d out-fx tests/AccountPhotoChecks.java`. Set SUPABASE_URL to http://127.0.0.1:18764 and SUPABASE_ANON_KEY to local-contract-test in a separate terminal, then run `java -cp out-fx crsm.AccountPhotoChecks`. These checks use a localhost server, not a production database. They verify request payloads, authentication headers, validation, download wiring and error propagation. They do not prove deployed RLS or database persistence.

## Signed-in manual checks

- For Customer, Technician and Admin: open Profile from the sidebar; verify saved name, username, contact and address. Email is explicitly read only. Save edits, reopen Profile and sign out/in; confirm persistence and the updated header name. Discard should reload saved values.
- Check blank name/username, invalid phone, missing Customer phone and an address over 255 characters. A failed save must show an error without reporting success. Check a duplicate username is rejected by the database.
- Record customer_id, technician_id and auth_user_id before and after editing; verify they remain unchanged, with linked customer/technician names and contact updated. Specialization, availability, role and status must remain unchanged.
- Change password: reject short and mismatched values; save a valid password, sign out and verify the new password works. Backend password-policy failures must display an error.
- Customer: open My Repairs -> View details / photos. Technician: My Assigned Repairs -> choose an assigned repair -> View details / photos. Admin: Repair Requests -> View details, or Repair Workflow -> View details / photos.
- Test saved repairs with zero, one and multiple photos, including photos uploaded before this change. Open thumbnails and navigate Previous/Next; verify boundary buttons, photo counts and preserved aspect ratio.
- Test unavailable storage objects, corrupt image data, network failure and expired sessions. Verify error text, reload/retry, and navigation past a failed photo.
- Using another Customer and an unassigned Technician, attempt attachment metadata and direct storage reads for the same repair with their JWTs: no metadata or image bytes should be returned. Reassign the repair and verify the old Technician loses access and the newly assigned Technician gains access. Verify a deactivated account cannot read attachments or save its profile.
- Confirm role and activation changes remain available only through Admin account management.

Visual layout follows the downloaded Figma personal-information cards, security section, damage-photo strip, palette and rounded controls. The app was not launched for visual verification.


## Technician availability and shop payments

Apply `docs/availability_and_payment_migration.sql` after the profile/photo migration. This migration was prepared but not applied by the agent. Customers pay at the shop; their assigned Technician or Admin records funds actually received. Refunds remain Admin-only.

- Admin assigns repairs. Assignment accepts only Available technicians with no unfinished technical repair. Busy, Unavailable, Off Duty and inactive linked accounts cannot accept new work. A technician already assigned to the same repair is a no-op.
- Duty status is manually managed; effective availability and active repair count are derived from saved repairs. Awaiting Payment, Ready for Pickup, Completed and Cancelled do not consume technical capacity. Existing multiple assignments are retained; a technician cannot accept another until their technical workload clears.
- Finish technical work with a final bill. Check the technician becomes eligible for another repair before payment/pickup. Check off-duty status is preserved.
- As the assigned Technician, open My Assigned Repairs, select a repair and Record payment. Check the final bill, net paid and balance; record only received funds. Customers and other technicians must be rejected by the RPC even if they invoke it directly.
- For a PHP 500 bill, record PHP 200 then PHP 300. Verify a distinct receipt per transaction, a remaining PHP 300 after the first payment, and Ready for Pickup after full settlement. Verify pickup changes the status to Completed.
- Reject zero/negative payment, excessive decimal precision, unsupported method, payment before finalization, overpayment, additional payment after settlement and pickup with an outstanding balance. A zero-cost finished repair needs no payment.
- Customer Payments & Receipts must include repairs with no payments, pending final costs, net payments after refunds, current balances and all receipts. Refresh after another signed-in user records payment.
- Try concurrent assignments to the same technician and concurrent payments against the same remaining balance from two sessions. Only one conflicting operation should succeed; the other must display a backend error.
- Generic repair/payment CRUD is disabled in JavaFX so assignment, completion and payment transitions go through their validated services.

Run `powershell -ExecutionPolicy Bypass -File test-workflows.ps1` after building for local service checks. `tests/workflow_regression.sql` is a separate, rollback-based database regression suite for a TEST Supabase database; it was not run against the configured database. Local HTTP checks verify contracts and error propagation, not deployed SQL behavior.


## Digital receipts and mock QR

- In Customer Payments & Receipts, select a saved payment and choose View selected receipt. Verify the receipt number, payment date/method, amount received, refunds and net payment. The current bill balance is labeled separately from this individual transaction.
- Use Save receipt (.txt), cancel once, then save and reopen the text file. Check it matches the on-screen receipt. A partial payment receipt must show the remaining balance.
- For a finalized unpaid bill, choose Show demo payment QR. The same button is available in the Technician/Admin payment dialog. Verify the amount and repair ID, plus the DEMO ONLY label. This pattern is illustrative and not scannable; it has no payment destination.
- Close the demo QR and refresh the bill: balances, payment rows and receipts must be unchanged. Only a confirmed payment recorded by the assigned Technician or Admin changes payment records. The Customer demo button is disabled for unfinalized or fully paid bills.


## Charts, icons and parts prices

Apply `docs/dashboard_statistics_migration.sql` after the existing profile/photo migration. It was applied during the November 11 database repair. The parts browser also requires `docs/repair_request_pricing_migration.sql` and its listed prerequisites.

- Dashboard: check status-chart counts against saved repairs and the latest eight repair rows. Empty accounts should show zero metrics and a clear empty chart.
- Check six monthly payment bars, including zero months, against recorded payments less refunds. Refunds are attributed to the original payment month. The aggregate net-paid metric includes all dates; the chart covers only six months.
- Customer dashboards must include only owned repairs; Technician dashboards only assigned repairs; Admin dashboards cover the shop. Try an unrelated technician and a deactivated account. Backend RPCs must enforce the same restrictions.
- Keyboard through sidebar and action buttons. Verify descriptive labels, distinct icons, focus borders and tooltips; labels remain visible alongside icons.
- Parts & Prices: search by name, brand, model, specification and device type. Confirm inactive parts are excluded and labor is not included in part prices. No supplier/photo/price samples are fabricated.
- Select a part and use Check selected part on Shopee. Confirm the browser search contains the selected part details. Shopee search provides live listings on the supplier site; CRSM does not scrape or automatically import their photos/prices. A supported API/feed connection is still needed for automatic import.

Service contract checks include dashboard requests, the active-parts query, and safe supplier-link encoding. The database regression suite adds owner/assignment statistics checks but has not been executed against a database.
