# Case Study Completion Checklist

Use this list to complete the preliminary draft against `docs/CASE-STUDY-REPORT_Template.pdf`. Do not treat template examples or the prior `docs/full-unedited.pdf` as verified project facts without confirmation.

## Report and formatting

- [x] Locate and review the Level 1, Level 2, and Level 3 UCD diagrams in `UI_design/`.
- [ ] Confirm title, course, section, 4–5 member names, instructor, submission date, institution, and campus.
- [ ] Confirm whether the case is based on an actual repair organization or a hypothetical project scenario.
- [ ] Gather factual organization background, mission, services, location, roles, and operating workflow.
- [ ] Resolve the distinction between implemented Java prototype, database design, planned functionality, and any deployed system.
- [ ] Confirm target DBMS and reconcile MySQL-style schema syntax with the README's planned PostgreSQL/Supabase connection.
- [ ] Apply A4, Calibri 11, 1.15 spacing, exact I–XI section numbering, and 10–15 pages in the final document.
- [ ] Check each figure/table number, caption, cross-reference, and source.
- [ ] Add verified APA 7th edition references, including an authoritative RA 10173 source and any applicable institutional policies.
- [ ] Ensure final length is achieved through substantive evidence and required visuals, not invented content or filler.

## Required diagrams, screenshots, and appendices

- [ ] **Figure 1: Information Architecture Diagram.** Mark UI prototype, proposed application/database integration, schema, SQL views, and users; label current vs planned components.
- [ ] **Figure 2: Existing ERD / relational schema.** Derive from the authoritative SQL schema and reconcile with `docs/Relational_Schema.pdf`; show keys, cardinalities, optional foreign keys, and delete actions accurately.
- [ ] **Appendix A: RACI Matrix.** Complete the instructor's provided activity list with confirmed roles; exactly one A per activity.
- [ ] **Appendix B: Risk Matrix.** Rate each risk based on the actual environment using the template's likelihood/impact scale and risk grid; add controls and owners as appropriate.
- [ ] **Appendix C: Integration Matrix.** Map actual/proposed workflow steps to units, systems/data, shared outputs, and service/decision impact.
- [ ] **UI screenshots.** Select PNG design screens from `UI_design/ADMIN/`; captions must identify them as designer-provided design references unless implementation is separately evidenced.
- [ ] **Color palette reference.** Include or cite `UI_design/color_palette.jpg` only after confirming it applies to this system; document its source and designer notes.
- [ ] Add any approved policy excerpts, interview notes, or survey instrument used to substantiate organizational claims.
- [ ] Confirm whether the instructor expects a separate MDM workflow illustration in addition to the Information Architecture Diagram.

## Group information still needed

- [ ] Actual organization/unit identity and whether it is operating, hypothetical, or a proposed client.
- [ ] Mission, services, location, workflow, staff roles, information flow, and known pain points.
- [ ] Which application screens are implemented, which are design-only, and whether any database is deployed or populated with real records.
- [ ] Map each UCD workflow to its role, Swing screen, model/DAO, table/operation, and verification evidence; record unsupported requirements separately.
- [ ] Resolve current schema gaps for authentication/roles, customer-owned record scope, damage-photo storage, intake condition/items received, estimate/upgrade approvals, pickup, receipts/payment methods/refunds, and mockup status values.
- [ ] Confirm whether customer and staff screens may follow the admin PNG theme/layout where dedicated role-specific PNG designs are not supplied.
- [ ] Database platform/version, schema version, hosting/data location, and tested connection status (do not share passwords or secrets).
- [ ] Confirmed data owners, stewards, custodians, data users, system administrator, and privacy/DPO contact where applicable.
- [ ] Existing access-control, privacy notice/consent, retention/deletion, backup, incident-response, and data-sharing policies; identify policy owner and evidence.
- [ ] Actual data-quality checks and whether uniqueness/duplicate detection exists for customers, emails, phone numbers, serial numbers, or other fields.
- [ ] Group-justified risk likelihood and impact values, current controls, and proposed control owners.
- [ ] Reports/dashboards actually used or desired, decisions they support, and any evidence of operational outcomes.
- [ ] Designer confirmation that palette and PNG screens belong to this system, plus source files or notes if available.
- [ ] Any group interviews, survey data, screenshots of a running app, schema export, or deployment evidence intended for appendices.
