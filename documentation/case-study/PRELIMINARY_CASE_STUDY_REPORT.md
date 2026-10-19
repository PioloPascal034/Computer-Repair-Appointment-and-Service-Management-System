# Preliminary Case Study Report

## Title Page

**Information Management Framework and System Plan for the Computer Repair Appointment and Service Management System**

- Course: [NEEDS INFORMATION: course name]
- Section: [NEEDS INFORMATION: section]
- Group members (4–5): [NEEDS INFORMATION: names]
- Instructor: [NEEDS INFORMATION: instructor's name]
- Date of submission: [NEEDS INFORMATION: submission date]
- Institution: Mariano Marcos State University (MMSU) is named in the template; confirm the correct institution and campus for this group.

> Preliminary draft. Claims below distinguish artifacts already in the workspace from proposed policies and functions. Bracketed items require confirmation. The final report must be formatted to the instructor's A4, Calibri 11, 1.15 spacing, 10–15 page requirement.

## I. Introduction

This case study examines the Computer Repair Appointment and Service Management System, a student project intended to support the recording of customers, computers, appointments, repair requests, technicians, repair services, and payments. The project includes a Java Swing user-interface prototype and SQL materials describing a relational database. The README explicitly describes the application as UI-only, using sample data and not connected to Supabase.

Information management matters to a repair-service workflow because customer contact details, device identifiers and reported problems, appointment schedules, repair progress, assigned technicians, service prices, and payment records must be associated consistently. The SQL design separates these data areas into related tables and defines keys and selected value constraints. The new Java screens are wired to a JDBC repository for these tables, but persistence has not been verified against a configured PostgreSQL database; legacy prototype screens remain static.

The purpose of this study is to describe the documented project information environment and propose an information management framework for data ownership, quality, privacy, security, integration, and decision support. The scope is limited to files available in the project and the separate `UI_design` folder. No interviews, operational observation, live database inspection, or implementation testing are documented in the available materials.

**Expected outcome:** a factual baseline of the prototype and SQL design, plus clearly labeled recommendations and the information needed to complete the case study. [NEEDS INFORMATION: confirm the real organization or unit represented by the system and whether the project models an actual repair shop or a hypothetical one.]

## II. Organizational Profile and Information Environment

### Organizational profile

The project files describe a computer repair service context, including device diagnosis and repair, appointment intake, technician assignment, service catalog, and payment tracking. The files do not identify an actual organization, its location, mission, staff structure, or operating policies. The project README gives the system name but does not establish that a particular repair business currently uses it.

- Organization/unit name and background: [NEEDS INFORMATION]
- Mission, service scope, location, and operating hours: [NEEDS INFORMATION]
- Actual personnel and roles: [NEEDS INFORMATION]
- Whether workflow details in `docs/full-unedited.pdf` describe a real organization or a proposal: [NEEDS INFORMATION]

### Data landscape and existing systems

The SQL scripts define seven relational tables: `Customers`, `Computers`, `Appointments`, `Repair_Requests`, `Technicians`, `Repair_Services`, and `Payments`. These fields represent personal/contact information, device details and reported problems, appointment information, diagnoses and repair statuses, technician profiles, service descriptions and prices, and payment amounts/statuses/dates. The schema does not define authentication or user/role tables.

The project retains the original Swing windows (`LoginFrame`, `DashboardFrame`, `DataFrame`, and `RepairRequestFrame`) as legacy prototype classes. `src/Main.java` now launches a newer Swing interface in `src/crsm/`: a shared palette-based shell, role-preview navigation for Customer/Staff/Admin, dashboard totals, schema-based data tables and add/edit/delete forms, report selection, and input validation. The newer tables and dashboard call `RepairRepository`, which uses JDBC and reads connection values from environment variables. No sample rows are supplied by these new screens.

The sign-in screen is explicitly a UI preview. It does not authenticate credentials or securely enforce role permissions. Customer-owned records are blocked in Customer preview because the schema has no authenticated customer identity or row-scoping mechanism. User-account management and customer signup are shown as unavailable because there is no user/authentication schema. The newer Java source compiles with JDK 21 and the supplied PostgreSQL JDBC JAR; a Supabase connection or database workflow has not been verified because connection environment variables were not configured for this review.

The SQL scripts include sample data and CRUD examples. `docs/Repairs_&_Views.sql` defines views for active repair status, appointment schedule, repair history, technician workload, repair revenue, pending repairs, and most-requested services. These are SQL artifacts; the available Java code does not show them being queried or presented as live reports.

### Information flow (documented design, not verified live operation)

In the database design, a customer may be linked to computers and appointments. A computer may be linked to repair requests and appointments. A repair request may be assigned to a technician and service, and may have payment records. SQL views join these records for operational summaries. The Java prototype presently displays hard-coded examples, so this database flow is not demonstrated as a running application flow.

| Stage in the proposed data flow | Data involved | Evidence/status |
|---|---|---|
| Customer and device intake | Customer and computer fields | New Java data panels and generic CRUD forms target schema fields; live persistence is unverified |
| Appointment scheduling | Customer, computer, date/time, purpose, status | Appointment panel/form targets `Appointments`; UCD-specific customer scheduling flow and relational selectors remain incomplete |
| Repair handling | Computer, technician, service, problem, diagnosis, status, costs | Repair panel/form targets `Repair_Requests`; estimate approval history, photos, and pickup workflow are not represented in the current schema |
| Payment recording | Repair request, amount, date, status | Payment panel/form targets `Payments`; payment methods and receipt generation are not supported by current fields |
| Operational summaries | Joined repair, schedule, workload, revenue, service data | Reports screen can query defined views; live results remain unverified |

### Documented pain points and limits

The legacy prototype uses static sample data; the new UI has removed those hard-coded rows and attempts database-backed loads and CRUD through JDBC. Persistence remains unverified until connected to a PostgreSQL-compatible schema. The preview role selector is not authentication or production access control. No audit trail, data retention process, backup procedure, privacy notice, or incident response process is evidenced in the inspected Java/SQL materials. These are observed documentation gaps, not proof that no such practices exist outside this project.

## III. Information Architecture

### Architecture overview

The project now contains a Java Swing interface and a separate SQL relational design. The new `crsm.DatabaseConfig` expects `SUPABASE_JDBC_URL`, `SUPABASE_DB_USER`, and `SUPABASE_DB_PASSWORD`; `RepairRepository` uses prepared statements against the seven defined tables, and its report method reads from a whitelist of SQL views. The PostgreSQL JDBC JAR is present. This connection path is prepared but is not verified against a live Supabase instance. The older `DatabaseConnection.java` remains legacy code and is not used by the new launcher. The SQL table script uses MySQL-style `AUTO_INCREMENT`, `CREATE DATABASE`, and `USE`, so target schema compatibility remains unresolved and requires a PostgreSQL-compatible schema before live database use.

The documented relational model has primary keys on each of the seven entities and foreign keys linking customers to computers/appointments, computers to appointments/repair requests, technicians and services to repair requests, and repair requests to payments. See Figure 2 placeholder and `docs/Relational_Schema.pdf` for the available relational schema reference.

### Figure 1. Information Architecture Diagram — placeholder

> **[PLACEHOLDER: Insert Information Architecture Diagram.]** Show the Java Swing prototype as a presentation layer using static sample rows today. Show the proposed JDBC connection to a PostgreSQL/Supabase database as planned, not operational. Inside the proposed database layer, show the seven documented tables and optional SQL views. Show staff/users only as proposed actors until the actual organization and roles are confirmed. Label existing and proposed components distinctly.

### Figure 2. Existing ERD / relational schema — placeholder

> **[PLACEHOLDER: Insert an ERD based on the actual `create_tables.sql` / `complete_pack(compressed).sql` definitions and cross-check against `docs/Relational_Schema.pdf`.** Preserve the optional technician/service relationships and the specified `ON DELETE` actions. Identify the SQL dialect and reconcile any differences before calling the diagram the implemented schema.]

### Commentary

The relational design separates core entities from service transactions and uses foreign keys to express their associations. The new Java repository is written for PostgreSQL and provides table CRUD and SQL-view reads, but the current SQL scripts have not been shown to create a compatible PostgreSQL schema. Until schema conversion and a live connection are verified, the system cannot be said to persist staff entries or generate live schedules, queues, and reports.

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

The SQL schema marks several fields as `NOT NULL`, defines primary and foreign keys, and includes checks for repair, appointment, and payment statuses and non-negative service prices, costs, and payment amounts. The SQL exercises include attempted invalid values, but they are examples in a script, not recorded test results. `docs/Normalization.pdf` describes the design as targeting 1NF, 2NF, and 3NF. The report should characterize this as the project's documented normalization rationale, not an independently verified database audit.

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

The new sign-in screen is labeled as a preview and does not verify identity or enforce permissions. The older `DatabaseConnection.java` contains a placeholder password and is unused by the new launcher. The new connection code reads environment variables, but no database connection is shown as working. SQL foreign keys and selected checks help protect relational integrity but do not provide user access control, encryption, backups, endpoint protection, or monitoring.

### Proposed risk treatment

Potential risks for assessment include unauthorized viewing or alteration of customer/payment information, loss or corruption of records, malware on a workstation, accidental entry errors, and disclosure through backups or exports. Likelihood and impact scores must be assigned by the group based on the actual deployment, not guessed from source code alone. Appendix B provides a blank structure using the instructor's 1–5 scoring and risk grid. Recommended controls may include verified authentication, least-privilege database accounts, secure secret storage, encrypted transport/storage where supported, tested backups, endpoint patching, audit logging, and a documented incident and recovery procedure. These are recommendations, not existing features.

### Appendix B. Risk Matrix — placeholder

> **[PLACEHOLDER: Complete the instructor's risk matrix with asset, vulnerability, likelihood (1–5), impact (1–5), score, risk level, and controls. Have the group justify ratings. Do not reuse the example ratings as project findings.]**

## VIII. Integration and Use for Decision-Making

The SQL view script defines reports for active repairs, appointment schedules, repair history, technician workload, repair revenue and remaining balances, pending repairs, and most-requested services. The new Reports screen can query these views through JDBC, and dashboard totals are loaded from the tables. The application has not been connected to a configured PostgreSQL database in this review, so no report output or live dashboard values have been verified. The legacy dashboard still contains hard-coded sample rows but is no longer launched by `Main`.

In a proposed integrated workflow, an authorized staff member records a customer and device once, links an appointment and repair request, and updates the repair and payment records as work proceeds. A manager could then use validated reports to identify unassigned or delayed repairs and review payment balances. This is a proposed benefit and should not be described as a measured improvement without operational evidence.

### Appendix C. Integration Matrix — placeholder

> **[PLACEHOLDER: Complete the instructor's matrix with workflow step, unit/office, system/data source, data produced/shared, and service/decision impact. Distinguish existing SQL artifacts from planned system integration.]

## IX. Summary of Findings and Recommendations

The available files establish that the project has a seven-table relational schema, sample SQL data and CRUD examples, integrity constraints, reporting views, and a newer Swing UI with JDBC repository code for table CRUD and report reads. Compilation is verified; live persistence, dashboard values, and report output are not. The preview login does not authenticate users. The SQL script's MySQL-style syntax still needs reconciliation with the PostgreSQL JDBC target.

Recommended next steps are to confirm the real organization and workflow; produce and verify a PostgreSQL-compatible schema; configure and verify the Supabase JDBC connection; test the implemented CRUD and reporting workflows; resolve UCD requirements that need new schema support; implement authentication and role permissions before handling real personal data; agree on data ownership, quality, privacy, retention, and recovery controls; and complete the diagrams and matrices using confirmed group/organizational information.

Limitations include reliance on project files rather than interviews or a live deployment; unverified sample data; compile-only verification of the new source; no configured database connection during this review; no supplied privacy/security policies; and unresolved design-versus-implementation status for several functions. The report should be finalized only after the group resolves these items and supplies the missing evidence.

## X. References

> References below identify materials actually available in the workspace. Verify publication details and add authoritative legal/institutional sources before final submission. The instructor's template requires APA 7th edition and references for RA 10173; no official legal source file was found in the project inventory.

- *CASE-STUDY-REPORT_Template* [Course template]. (n.d.). `docs/CASE-STUDY-REPORT_Template.pdf`.
- Computer Repair Appointment and Service Management System. (n.d.). *Java Swing UI prototype* [Source code and README]. `src/` and `README.txt`.
- Computer Repair Appointment and Service Management System. (n.d.). *Database schema, constraints, sample data, CRUD examples, and reporting views* [SQL files]. `docs/create_tables.sql`, `docs/constraints.sql`, `docs/insert_sample_data.sql`, `docs/Repairs_&_Views.sql`, and `docs/complete_pack(compressed).sql`.
- Computer Repair Appointment and Service Management System. (n.d.). *Relational schema* [PDF]. `docs/Relational_Schema.pdf`.
- Computer Repair Appointment and Service Management System. (n.d.). *Database normalization* [PDF]. `docs/Normalization.pdf`.
- Republic Act No. 10173, Data Privacy Act of 2012. [NEEDS INFORMATION: add verified official citation and access details in APA 7th edition.]

## XI. Appendices

### Appendix A. RACI Matrix

> **[PLACEHOLDER: Complete the instructor's RACI template with the confirmed organization roles. Required role columns: Data Owner, Data Steward, Data Custodian, Data User, DPO/Compliance Administrator, System Administrator. Assign exactly one A per activity.]

### Appendix B. Risk Matrix

> **[PLACEHOLDER: Insert the completed, group-rated risk matrix described in Section VII.]

### Appendix C. Integration Matrix

> **[PLACEHOLDER: Insert the completed integration matrix described in Section VIII.]

### Appendix D. Supporting diagrams and UI evidence

- Figure 1: Information Architecture Diagram [PLACEHOLDER]
- Figure 2: Existing ERD / relational schema [PLACEHOLDER]
- UI screen designs: supplied PNG files in the separate `UI_design/ADMIN/` folder; select and caption relevant screens, and state that they are design references rather than proof of implemented functionality.
- Color palette: supplied `UI_design/color_palette.jpg`; the group confirmed that its colors apply to this system. The new Swing theme uses the palette swatches. The image also shows Raleway font samples; verify that font availability on the target machines is acceptable.
- [NEEDS INFORMATION: any interview notes, survey instruments, privacy notices, or approved policies used as evidence.]
