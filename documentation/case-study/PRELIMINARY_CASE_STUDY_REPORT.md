# Preliminary Case Study Report

## Title Page

**Information Management Framework and System Plan for the Computer Repair Appointment and Service Management System**

- Course: [NEEDS INFORMATION: course name]
- Section: [NEEDS INFORMATION: section]
- Group members (4–5): [NEEDS INFORMATION: names]
- Instructor: [NEEDS INFORMATION: instructor's name]
- Date of submission: [NEEDS INFORMATION: submission date]
- Institution: Mariano Marcos State University (MMSU) is named in the template; confirm the correct institution and campus for this group.

> Preliminary draft, updated against the current repository SQL and implementation notes. The project owner reports that the Supabase database has been updated to the current schema; that deployment status has not been independently inspected in this report. Claims below distinguish repository evidence, owner-reported deployment, and proposed policies. Bracketed items require confirmation. The final report must be formatted to the instructor's A4, Calibri 11, 1.15 spacing, 10–15 page requirement.

## I. Introduction

This case study examines the Computer Repair Appointment and Service Management System, a student project intended to support customer sign-in, device intake, appointment scheduling, repair tracking, technician workflows, service pricing, and payments. The project includes a Java Swing application, Supabase Auth and Storage integration, a PostgreSQL business schema, and additive SQL migrations for repair workflows. The project owner reports that the Supabase database has been updated. This report does not independently verify the deployed schema, live records, or end-to-end application behavior.

Information management matters to a repair-service workflow because customer contact details, device identifiers and reported problems, appointment schedules, repair progress, assigned technicians, service prices, and payment records must be associated consistently. The PostgreSQL design separates these data areas into related tables and defines keys, row-level security (RLS), status checks, and workflow functions. The Java application uses Supabase Auth for accounts and a JDBC repository for business-data operations; the repository notes identify that JDBC path as using a separate database credential, not the signed-in user's RLS identity.

The purpose of this study is to describe the documented project information environment and propose an information management framework for data ownership, quality, privacy, security, integration, and decision support. The scope is limited to repository files and materials supplied by the project group. No interviews, operational observation, or independent live database inspection are documented in the available materials.

**Expected outcome:** a factual baseline of the prototype and SQL design, plus clearly labeled recommendations and the information needed to complete the case study. [NEEDS INFORMATION: confirm the real organization or unit represented by the system and whether the project models an actual repair shop or a hypothetical one.]

## II. Organizational Profile and Information Environment

### Organizational profile

The project files describe a computer repair service context, including device diagnosis and repair, appointment intake, technician assignment, service catalog, and payment tracking. The files do not identify an actual organization, its location, mission, staff structure, or operating policies. The project README gives the system name but does not establish that a particular repair business currently uses it.

- Organization/unit name and background: [NEEDS INFORMATION]
- Mission, service scope, location, and operating hours: [NEEDS INFORMATION]
- Actual personnel and roles: [NEEDS INFORMATION]
- Whether workflow details in `docs/full-unedited.pdf` describe a real organization or a proposal: [NEEDS INFORMATION]

### Data landscape and existing systems

The fresh PostgreSQL setup defines seven core business tables: `customers`, `computers`, `appointments`, `repair_requests`, `technicians`, `repair_services`, and `payments`. It also defines `crsm_profiles` and `crsm_settings`, reporting views, and private Storage buckets. The workflow and catalog migrations add customer/technician Auth links; account activity and status fields; device and service catalogs/prices; estimate options and reviews; repair history events, photos, and work entries; payment refunds; and replacement-part pricing. The Auth profile supports `CUSTOMER`, `STAFF`, and `ADMIN` roles. The earlier `create_tables.sql` is a legacy MySQL-style schema, not the current PostgreSQL setup.

The project retains the original Swing windows (`LoginFrame`, `DashboardFrame`, `DataFrame`, and `RepairRequestFrame`) as legacy prototype classes. `src/Main.java` launches the newer application in `src/crsm/`, with sign-in/sign-up, role-specific navigation for Customer/Staff/Admin, customer request and tracking screens, staff repair operations, dashboards, management lists, reports, and account tools. Supabase Auth handles sign-in and sign-up; business-data CRUD, dashboards, and reports also use `RepairRepository` over JDBC. Repository documentation describes the JDBC database credential as separate from the end-user Auth identity.

Sign-in and sign-up use Supabase Auth, and new profiles default to `CUSTOMER`; role promotion and account-management operations use server-side functions. Customer workflow operations use the signed-in user's JWT and database ownership/RLS checks. The project owner reports that the Supabase database is updated, but this report does not independently inspect the deployed schema or verify live Auth, Storage, JDBC, or complete workflow transactions. Legacy JDBC-backed operations use a shared database credential and do not inherit the signed-in user's RLS identity; this is an important access-control limitation for deployment.

The SQL materials include sample data, constraints, and seven reporting views: active repair status, appointment schedule, repair history, technician workload, repair revenue, pending repairs, and most-requested services. The newer Reports screen supports report selection, filtering, and CSV export through the application repository. Live report results were not independently verified for this case study.

### Information flow (implemented design; live operation not independently verified)

In the database design, a Customer Auth account can be linked to a customer record, which is associated with computers and appointments. A repair request is associated with a computer and may have an assigned technician and service. Workflow records capture estimates, decisions, work history, photos, payments, refunds, and pickup events. SQL views support operational summaries. The repository and project notes describe these application paths, but this case study does not independently verify them against the updated live Supabase project.

| Stage in the proposed data flow | Data involved | Evidence/status |
|---|---|---|
| Customer and device intake | Customer identity/contact, device specifications, intake condition/accessories, reported problem and photos | Customer workflow and device catalog are represented in the application and SQL migrations; live submission was not independently observed |
| Appointment scheduling | Customer, computer, date/time, purpose, status | Request workflow creates a linked appointment and repair request; booking rules are checked in the database function |
| Repair handling | Technician assignment, diagnosis, work entries, estimate options/decisions, photos, status, costs | Workflow migrations and application notes describe guarded Staff/Customer operations and repair history |
| Payment recording | Repair request, amount, method, receipt, status, refund | Payment and Admin refund functions record payments/refunds; printable receipt generation remains unimplemented |
| Operational summaries | Joined repair, schedule, workload, revenue, service data | Reporting views and application report/dashboard screens are present; live report output was not independently verified |

### Documented pain points and limits

The legacy prototype classes remain in the repository, but `Main` launches the newer application. The project documentation describes database-backed JDBC operations and JWT-authenticated customer/staff workflows; live operation is not independently verified here. The database contains repair-history events and login-activity records, but the project materials do not establish a complete audit policy, data-retention schedule, backup/recovery procedure, privacy notice, or incident-response process. These are documentation/evidence gaps, not proof that no such practices exist outside the project.

## III. Information Architecture

### Architecture overview

The project contains a Java Swing application and a PostgreSQL/Supabase relational design. `crsm.DatabaseConfig` reads `SUPABASE_JDBC_URL`, `SUPABASE_DB_USER`, and `SUPABASE_DB_PASSWORD`; `RepairRepository` uses prepared statements for the legacy business-data CRUD/dashboard/report path. Supabase Auth, Storage, and customer workflow calls use the Supabase client. The owner reports that the Supabase database has been updated. The earlier `docs/create_tables.sql` uses MySQL syntax and should not be treated as the authoritative deployed schema; the current setup and migrations are the PostgreSQL materials under `docs/`. The older `DatabaseConnection.java` remains legacy code and is not used by the new launcher.

The core relational model uses primary keys and foreign keys linking customers to computers/appointments, computers to appointments/repair requests, technicians and services to repair requests, and repair requests to payments. Workflow tables add estimate, history, photo, work-log, review, and refund relationships. See the Figure 2 placeholder: it should be rebuilt from the current PostgreSQL setup and migrations, not from the legacy MySQL SQL files or the older relational-schema PDF alone.

### Figure 1. Information Architecture Diagram — placeholder

> **[PLACEHOLDER: Insert Information Architecture Diagram.]** Show the Java Swing application, Supabase Auth/Storage and JWT-backed Customer/Staff workflows, and the JDBC repository path to PostgreSQL/Supabase. Distinguish implemented/repository-documented components from operations independently verified in the live project. Show Customer, Staff, and Admin as application roles; keep the actual shop/organizational responsibilities subject to confirmation.

### Figure 2. Current ERD / relational schema — placeholder

> **[PLACEHOLDER: Insert an ERD based on `supabase_fresh_setup.sql` and the workflow/catalog/pricing migrations confirmed as applied.]** Include the seven core business tables plus profile/settings, catalog/pricing, estimate, review, history, photo, work-entry, login-activity, and refund tables. Show primary/foreign keys, optional links, and delete actions. Use a schema export from the updated Supabase project to confirm the final deployed version before labeling the diagram as verified.

### Commentary

The relational design separates core entities from service transactions and uses foreign keys to express their associations. The PostgreSQL setup and migrations are compatible in dialect with the JDBC driver. The owner reports that the live Supabase schema has been updated; this report does not independently validate its exact migration version, data, or end-to-end persistence. The JDBC path uses a separate database credential, while customer workflow calls use the authenticated Supabase user and database checks.

## IV. Master Data Management (MDM) Framework

### Proposed master data scope

The project schema suggests `Customers`, `Computers`, `Technicians`, and `Repair_Services` as candidate reference/master data domains. Appointments, repair requests, and payments represent transactional records. This classification is a proposal based on table purpose; the project has no documented MDM hub, steward assignments, matching rules, or synchronization process.

### Proposed model and controls

For a single repair-service application, a centralized source of truth in the deployed relational database is a reasonable proposed starting point, subject to confirmation of the actual number of locations and systems. Under this proposal, unique table IDs identify records; foreign keys link related records; controlled status values and non-negative amount checks protect selected fields. The SQL does not currently show uniqueness rules for email or serial number, duplicate detection, standardized phone formats, a metadata catalog, or a process for merging duplicate customers.

### Proposed workflow

1. Assign an authorized staff role to create or correct each customer, device, technician, and service record.
2. Validate required fields and formats at both the application and database layers; decide whether email and serial number must be unique and how exceptions are handled.
3. Link new appointments and repair requests to existing customer/device records instead of re-entering descriptive details.
4. Record who changed important master and transaction data, when it changed, and why, if an audit mechanism is approved and implemented.
5. Review duplicates, invalid values, and stale records on a defined schedule. [NEEDS INFORMATION: owner, review frequency, exception process, and retention rules.]

> **[PLACEHOLDER: Insert a conceptual MDM control/workflow illustration if required by the group. Clearly mark it as proposed and keep it distinct from Figure 1's system architecture.]**

## V. Data Governance and Quality Policy

### Current evidence

The current PostgreSQL setup and migrations define required fields, primary and foreign keys, allowed appointment/repair/payment statuses, and non-negative price/cost/payment checks. RLS policies and guarded functions constrain authenticated workflow operations. These are schema controls, not evidence of a completed security audit or measured data-quality outcomes. `docs/Normalization.pdf` describes the earlier design's normalization rationale; it should not be presented as an independent normalization audit of the expanded current schema.

### Proposed governance roles and rules

The instructor's template asks for a RACI using Data Owner, Data Steward, Data Custodian, Data User, DPO/Compliance Administrator, and System Administrator. The actual organization has not supplied people or role assignments. Appendix A is therefore a blank/template-based proposed matrix until the group confirms accountable personnel. Every activity should have exactly one Accountable (A), as the instructor's template states.

Proposed quality dimensions are accuracy, completeness, consistency, uniqueness, and timeliness. Specific controls could include required-field checks, valid status lists, referential integrity, agreed contact/serial-number formats, duplicate review, and periodic correction workflows. Assign owners and measurable targets only after the organization confirms its procedures.

### Proposed adoption and monitoring

Confirm who may create, correct, approve, and export each data category; document definitions and allowed values; provide staff guidance; and review data-quality exceptions at an agreed interval. [NEEDS INFORMATION: existing policies, responsible roles, monitoring frequency, and quality targets.]

## VI. Privacy, Regulatory, and Compliance Considerations

The schema contains personal data fields including names, phone numbers, email addresses, and physical addresses. It also contains device serial numbers, reported problems, diagnoses, appointment schedules, and payment information. This creates privacy and confidentiality responsibilities for the organization operating the proposed system.

The instructor's template specifically requires discussion of Republic Act No. 10173 (Data Privacy Act of 2012). This preliminary draft does not claim compliance. The files provided do not include a privacy notice, consent record, lawful-purpose documentation, retention/deletion schedule, data-subject request process, breach procedure, encryption configuration, or documented privacy impact assessment. The prior `full-unedited.pdf` asserts explicit consent and restricted access, but these are unsupported by current application/database artifacts and need confirmation before inclusion as existing controls.

Recommended actions for the group to validate with the instructor/organization include documenting collection purposes and notice, limiting collected data to what is needed, assigning a privacy contact or DPO where applicable, defining access and retention rules, securing exports/backups, and preparing a breach escalation process. [NEEDS INFORMATION: actual notices, consent process, responsible privacy role, data location/hosting arrangement, retention practice, and any institution/shop privacy policy.]

## VII. Information Security and Risk Management

### Evidence-based observations

The new application uses Supabase Auth and role-specific workflows. The SQL setup and migrations define RLS policies, database-side role/ownership checks, guarded functions, and private Storage buckets. These controls are documented in the repository but have not been independently tested against the updated live project for this report. The separate JDBC business-data path uses a configured database credential and does not inherit the signed-in user's RLS identity; it should be restricted to a trusted deployment and replaced with a trusted backend/JWT-scoped design before distributing the app to untrusted users. The project files do not establish backup testing, endpoint protection, a complete monitoring process, or a privacy/incident-response policy.

### Proposed risk treatment

Potential risks for assessment include unauthorized viewing or alteration of customer/payment information, loss or corruption of records, malware on a workstation, accidental entry errors, and disclosure through backups or exports. Likelihood and impact scores must be assigned by the group based on the actual deployment, not guessed from source code alone. Appendix B provides a blank structure using the instructor's 1–5 scoring and risk grid. Recommended controls may include verified authentication, least-privilege database accounts, secure secret storage, encrypted transport/storage where supported, tested backups, endpoint patching, audit logging, and a documented incident and recovery procedure. These are recommendations, not existing features.

### Appendix B. Risk Matrix — placeholder

> **[PLACEHOLDER: Complete the instructor's risk matrix with asset, vulnerability, likelihood (1–5), impact (1–5), score, risk level, and controls. Have the group justify ratings. Do not reuse the example ratings as project findings.]**

## VIII. Integration and Use for Decision-Making

The SQL setup defines reports for active repairs, appointment schedules, repair history, technician workload, repair revenue and remaining balances, pending repairs, and most-requested services. The newer Reports screen supports querying reports and CSV export; Staff/Admin dashboards show business-data counts, status charts, and recent records. The repository describes these as live database-backed features, but their output was not independently checked against the updated Supabase project for this report. The legacy dashboard class remains in the repository but is no longer launched by `Main`.

In a proposed integrated workflow, an authorized staff member records a customer and device once, links an appointment and repair request, and updates the repair and payment records as work proceeds. A manager could then use validated reports to identify unassigned or delayed repairs and review payment balances. This is a proposed benefit and should not be described as a measured improvement without operational evidence.

### Appendix C. Integration Matrix — placeholder

> **[PLACEHOLDER: Complete the instructor's matrix with workflow step, unit/office, system/data source, data produced/shared, and service/decision impact. Distinguish existing SQL artifacts from planned system integration.]**

## IX. Summary of Findings and Recommendations

The repository establishes a Java Swing application, Supabase Auth/Storage integration, a PostgreSQL setup with seven core business tables and additional workflow/catalog/pricing tables, RLS and guarded functions, reporting views, and application paths for customer requests, repair handling, account management, payments/refunds, dashboards, and reports. The project owner reports that the Supabase database has been updated. This case study does not independently verify the deployed schema, live data, or end-to-end behavior. The JDBC business-data path remains separate from the signed-in Auth user's RLS identity and requires careful trusted deployment.

Recommended next steps are to obtain a schema export or migration history from the updated Supabase project; test representative Auth, Storage, customer ownership, repair, payment/refund, JDBC, and reporting workflows without exposing secrets; review the shared-credential JDBC risk; confirm the actual organization and operating policies; agree on data ownership, quality, privacy, retention, and recovery controls; and complete diagrams and matrices using confirmed group/organizational information.

Limitations include reliance on repository files and owner-reported database status rather than an independent live inspection; no documented interviews or operational observation; no supplied organizational privacy/security policies; and unresolved design-versus-implementation status for any workflows not yet demonstrated. The report should be finalized after the group supplies deployment evidence and resolves the remaining organizational and academic requirements.

## X. References

> References below identify materials actually available in the workspace. Verify publication details and add authoritative legal/institutional sources before final submission. The instructor's template requires APA 7th edition and references for RA 10173; no official legal source file was found in the project inventory.

- *CASE-STUDY-REPORT_Template* [Course template]. (n.d.). `docs/CASE-STUDY-REPORT_Template.pdf`.
- Computer Repair Appointment and Service Management System. (n.d.). *Java Swing UI prototype* [Source code and README]. `src/` and `README.txt`.
- Computer Repair Appointment and Service Management System. (n.d.). *Supabase PostgreSQL setup, security, customer workflow, catalog, and pricing migrations* [SQL files]. `docs/supabase_fresh_setup.sql`, `docs/customer_workflow_migration.sql`, `docs/customer_catalog_and_demo_seed.sql`, `docs/service_catalog_metadata_migration.sql`, `docs/device_specification_migration.sql`, `docs/repair_request_pricing_migration.sql`, and related SQL files.
- Computer Repair Appointment and Service Management System. (n.d.). *Relational schema* [PDF]. `docs/Relational_Schema.pdf`.
- Computer Repair Appointment and Service Management System. (n.d.). *Database normalization* [PDF]. `docs/Normalization.pdf`.
- Republic Act No. 10173, Data Privacy Act of 2012. [NEEDS INFORMATION: add verified official citation and access details in APA 7th edition.]

## XI. Appendices

### Appendix A. RACI Matrix

> **[PLACEHOLDER: Complete the instructor's RACI template with the confirmed organization roles. Required role columns: Data Owner, Data Steward, Data Custodian, Data User, DPO/Compliance Administrator, System Administrator. Assign exactly one A per activity.]**

### Appendix B. Risk Matrix

> **[PLACEHOLDER: Insert the completed, group-rated risk matrix described in Section VII.]**

### Appendix C. Integration Matrix

> **[PLACEHOLDER: Insert the completed integration matrix described in Section VIII.]**

### Appendix D. Supporting diagrams and UI evidence

- Figure 1: Information Architecture Diagram [PLACEHOLDER]
- Figure 2: Current ERD / relational schema [PLACEHOLDER]
- UI screen designs: supplied PNG files under `docs/design/`; select and caption relevant screens, distinguishing design references from verified application behavior.
- Color palette: supplied `docs/design/color_palette.jpg`; confirm its provenance and intended use before treating it as authoritative. The Java application has a theme implementation; confirm font availability on target machines if required.
- [NEEDS INFORMATION: any interview notes, survey instruments, privacy notices, or approved policies used as evidence.]
