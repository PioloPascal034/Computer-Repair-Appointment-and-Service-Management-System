# Case Study Completion Checklist

Use this list to complete the preliminary draft against `docs/CASE-STUDY-REPORT_Template.pdf`. The project owner reports that the Supabase database has been updated to the current schema; obtain deployment evidence before describing the live schema or workflows as independently verified. Do not treat template examples or the prior `docs/full-unedited.pdf` as verified project facts without confirmation.

## Report and formatting

- [ ] Locate and review the Level 1, Level 2, and Level 3 use-case diagrams in `docs/design/`; confirm whether the supplied images are the authoritative diagrams.
- [ ] Confirm title, course, section, 4–5 member names, instructor, submission date, institution, and campus.
- [ ] Confirm whether the case is based on an actual repair organization or a hypothetical project scenario.
- [ ] Gather factual organization background, mission, services, location, roles, and operating workflow.
- [ ] Resolve the distinction between repository-implemented Java workflows, database design, owner-reported Supabase deployment, and independently verified operation.
- [ ] Obtain a schema export or migration history from the updated Supabase project; use the PostgreSQL setup and migrations as the current repository schema, not legacy MySQL-style `docs/create_tables.sql`.
- [ ] Verify representative live workflows (Auth, customer ownership, request/appointment submission, Storage, Staff repair actions, payments/refunds, JDBC operations, and reports) and record evidence without including credentials or personal data.
- [ ] Apply A4, Calibri 11, 1.15 spacing, exact I–XI section numbering, and 10–15 pages in the final document.
- [ ] Check each figure/table number, caption, cross-reference, and source.
- [ ] Add verified APA 7th edition references, including an authoritative RA 10173 source and any applicable institutional policies.
- [ ] Ensure final length is achieved through substantive evidence and required visuals, not invented content or filler.

## Required diagrams, screenshots, and appendices

- [ ] **Figure 1: Information Architecture Diagram.** Show the Java Swing app, Supabase Auth/Storage, JWT-backed Customer/Staff workflows, and the separate JDBC business-data path. Distinguish repository-documented functionality, owner-reported deployment, and independently verified operation.
- [ ] **Figure 2: Current ERD / relational schema.** Derive from `docs/supabase_fresh_setup.sql` and the applied workflow/catalog/pricing migrations; confirm against an export from the updated Supabase project. Show keys, cardinalities, optional foreign keys, and delete actions accurately.
- [ ] **Appendix A: RACI Matrix.** Complete the instructor's provided activity list with confirmed roles; exactly one A per activity.
- [ ] **Appendix B: Risk Matrix.** Rate each risk based on the actual environment using the template's likelihood/impact scale and risk grid; add controls and owners as appropriate.
- [ ] **Appendix C: Integration Matrix.** Map actual/proposed workflow steps to units, systems/data, shared outputs, and service/decision impact.
- [ ] **UI screenshots.** Select relevant PNGs from `docs/design/`; captions must distinguish design references from verified runtime screenshots.
- [ ] **Color palette reference.** Include or cite `docs/design/color_palette.jpg` only after confirming its provenance and relevance; document its source and designer notes.
- [ ] Add any approved policy excerpts, interview notes, or survey instrument used to substantiate organizational claims.
- [ ] Confirm whether the instructor expects a separate MDM workflow illustration in addition to the Information Architecture Diagram.

## Group information still needed

- [ ] Actual organization/unit identity and whether it is operating, hypothetical, or a proposed client.
- [ ] Mission, services, location, workflow, staff roles, information flow, and known pain points.
- [ ] Which application screens and workflows have been demonstrated against the updated Supabase project; distinguish repository code, design-only screens, and live test evidence.
- [ ] Map each UCD workflow to its role, Swing screen, model/DAO, table/operation, and verification evidence; record unsupported requirements separately.
- [ ] Confirm which workflow migrations are applied in Supabase and reconcile their deployed version with the repository. Current repository migrations include Auth ownership, RLS/guarded functions, repair photos, intake condition/accessories, estimate options/reviews, pickup, payment methods/receipts/refunds, and device-aware pricing; verify actual behavior and identify any remaining gaps rather than listing these as absent by default.
- [ ] Confirm whether customer and staff screens may follow the admin PNG theme/layout where dedicated role-specific PNG designs are not supplied.
- [ ] Database platform/version, schema version, hosting/data location, and tested connection status. The owner reports the Supabase database is updated; collect a schema export or migration history and non-sensitive test evidence (do not share passwords or secrets).
- [ ] Confirmed data owners, stewards, custodians, data users, system administrator, and privacy/DPO contact where applicable.
- [ ] Existing access-control, privacy notice/consent, retention/deletion, backup, incident-response, and data-sharing policies; identify policy owner and evidence.
- [ ] Actual data-quality checks and whether uniqueness/duplicate detection exists for customers, emails, phone numbers, serial numbers, or other fields.
- [ ] Group-justified risk likelihood and impact values, current controls, and proposed control owners.
- [ ] Reports/dashboards verified against live data or desired, decisions they support, and any evidence of operational outcomes.
- [ ] Designer confirmation that palette and PNG screens in `docs/design/` belong to this system, plus source files or notes if available.
- [ ] Any group interviews, survey data, screenshots of a running app, schema export, or deployment evidence intended for appendices.
