# Investigation: Central Application and Application Detail Tables

> **Status: Proposed.** Nothing in this document has been implemented. Written against
> `develop` at `4f14542f6`, which includes the `created_datetime` rename (V130).

## Problem

The two application families each have their own pair of tables:

| Family | Application table | Detail table |
|---|---|---|
| Extension and amendment (`SCHEDULE_AMENDMENT_APPLICATION`) | `schedule_work_programme_applications` | `schedule_work_programme_application_details` |
| Continuation (`CONTINUATION_APPLICATION`) | `licence_continuation_applications` | `licence_continuation_application_details` |

Most of the columns are the same in both. The Java side shares only two thin interfaces
(`LicenceApplication`, `LicenceApplicationDetail`), so every shared field is declared twice
and has already drifted: `created_date_time` and `created_datetime` (fixed in V130),
`INTEGER` versus `BIGINT` for WUA ids, `@Column` on some fields and not others, and `NOT NULL`
on one family's `created_datetime` only.

Several tables already refer to "an application" generically, as an `(application_type,
application_id)` pair with no foreign key, because there is no single table to point at:

- `application_decision_details.application_type` / `application_id`
- `licence_event_cache.application_type` / `application_id`
- `teams.scope_type` / `scope_id` for external contributor teams

Code that has to handle either family branches on `ApplicationType` (`ApplicationService`,
`DocumentLinkingService`, `ApplicationLetterService`, `SelectApplicationTypeService`,
`TeamManagementController`). The work area runs two services that do almost the same thing.

## Recommendation: root tables plus type-specific tables

Add two root tables, `applications` and `application_details`, holding every shared column and
an `application_type` discriminator. Columns used by only one family stay in that family's own
table. Three of the existing tables become those type-specific tables: they keep their names and
ids, lose the shared columns, and their `id` becomes both primary key and foreign key to the
root. `licence_continuation_applications` would be left with no columns except `id`, so it is
dropped.

```
applications (shared columns, application_type)
└── schedule_work_programme_applications   id → applications.id   steward_wua_id
    (continuation applications have no type-specific table)

application_details (shared columns, application_type)
├── schedule_work_programme_application_details  id → application_details.id
│     all_licensees_permission_confirmed, decision_date
└── licence_continuation_application_details     id → application_details.id
      current_term_id, current_phase_id, next_term_id, next_phase_id
```

In Java the four entity classes become subclasses of two new abstract roots, with an
`application_type` `@DiscriminatorColumn` on each root. The two hierarchies use different JPA
strategies, because Hibernate gives every subclass in a joined hierarchy its own table:

| Hierarchy | Strategy | Why |
|---|---|---|
| `application_details` | `@Inheritance(strategy = InheritanceType.JOINED)` | Both families have type-specific columns. Follows the existing precedent in `ScheduleEvent`. |
| `applications` | `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)`, with `schedule_work_programme_applications` as a `@SecondaryTable` of the extension subclass | Continuation applications have no table of their own, so they can't be a joined subclass. A secondary table gives the extension subclass its own table without needing one for continuations. |

The discriminator values are the `ApplicationType` enum names. Those strings are already stored
in `teams.scope_type`, `application_decision_details.application_type` and
`licence_event_cache.application_type`, so every existing generic reference stays valid as it is.

### Why keep the existing tables as the type-specific tables

- **Only one child foreign key moves.** Of the 15 tables that hang off an application or detail
  (request purpose, extension and amendment requests, record of decision, continuation
  requirements, supporting information, external contributor requests), 14 keep pointing at the
  table they point at today. Because those are type-specific tables, the database still
  guarantees that, for example, a continuation requirement row belongs to a continuation detail.
  The exception is `licence_continuation_external_contributor_request`, which points at the
  dropped `licence_continuation_applications` and is re-pointed to `applications` (Phase 2, step 3).
- **No nullable-by-type columns.** Every column in a type-specific table applies to every row in
  it, so nothing needs `CHECK` constraints to keep the other family's columns empty.
- **Smaller migration.** Shared columns are copied up into the root and dropped from the
  existing table. Ids and type-specific data are not moved.

The type-specific tables keep their existing names (decision 6).

### Why type-specific tables rather than the alternatives

| Option | Verdict |
|---|---|
| **Root tables plus type-specific tables** (recommended) | Shared columns in one place, type-specific columns in their own tables, and 14 of the 15 child foreign keys still point at type-specific tables. Cost: loading a detail or an extension application joins the root and its type table. |
| Everything in the two root tables | Two tables only, but the type-specific columns must be nullable for the other family, every child foreign key moves to a root table, and the database can no longer tell which family a child row belongs to. |
| One flat entity with a type field | Every `getScheduleWorkProgrammeApplication()`-style call, every per-type resolver and access rule, and every derived query name changes, and the journeys lose type safety. About 170 main classes touched. |

### Footprint today

| Class | Main files | Test files |
|---|---|---|
| `ScheduleWorkProgrammeApplication` | 12 | 29 |
| `ScheduleWorkProgrammeApplicationDetail` | 97 | 75 |
| `LicenceContinuationApplication` | 9 | 16 |
| `LicenceContinuationApplicationDetail` | 63 | 51 |

The subclasses keep their names, so almost none of these files change. The exception is the
link from detail to application, which moves to the root (Phase 3, step 3). That touches
about 10 derived queries in the two detail repositories and their callers.

## Target schema

```
applications
  id                                   UUID PK
  application_type                     TEXT NOT NULL      discriminator
  licence_schedule_id                  UUID NOT NULL → licence_schedules.id
  submitted_licence_schedule_detail_id UUID          → licence_schedule_details.id
  application_reference                TEXT UNIQUE
  withdrawal_reason                    TEXT               set when withdrawn, any type
  UNIQUE (id, application_type)                           target for the composite FK below

schedule_work_programme_applications
  id                                   UUID PK → applications.id
  steward_wua_id                       BIGINT             was INTEGER

licence_continuation_external_contributor_request         (child table, FK moves)
  application_id                       UUID → applications.id   was licence_continuation_application_id
                                                                → licence_continuation_applications.id

application_details
  id                                   UUID PK
  application_id                       UUID NOT NULL
  application_type                     TEXT NOT NULL      discriminator
  version_number                       INTEGER
  status                               TEXT
  created_datetime                     TIMESTAMPTZ NOT NULL  (see Phase 0, check 3)
  created_by_wua_id                    BIGINT
  submitted_datetime                   TIMESTAMPTZ
  submitted_by_wua_id                  BIGINT             was INTEGER in both tables
  responsible_organisation_unit_id     INTEGER
  update_deadline_date                 DATE
  update_reason                        TEXT
  FOREIGN KEY (application_id, application_type) → applications (id, application_type)
  UNIQUE (application_id, version_number)

schedule_work_programme_application_details
  id                                   UUID PK → application_details.id
  all_licensees_permission_confirmed   BOOLEAN
  decision_date                        DATE

licence_continuation_application_details
  id                                   UUID PK → application_details.id
  current_term_id                      UUID → licence_schedule_terms.id   (FK unchanged)
  current_phase_id                     UUID → licence_schedule_phases.id  (FK unchanged)
  next_term_id                         UUID → licence_schedule_terms.id   (FK unchanged)
  next_phase_id                        UUID → licence_schedule_phases.id  (FK unchanged)
```

`withdrawal_reason` lives on the root because withdrawal is being built for every application
type. Today only continuations can be withdrawn, so after the migration extension and amendment
rows have it null. That leaves `licence_continuation_applications` with nothing but its `id`, so
it is dropped. If a continuation-only application column is ever needed, it would bring back a
continuation table (as a secondary table, like the extension one).

With the table gone, the database no longer guarantees that
`licence_continuation_external_contributor_request` points at a continuation application rather
than an extension one. Phase 4 covers how to restore that.

Every child table's foreign key column is renamed to one of two names, whichever table it points
at: `application_detail_id` for the 13 tables keyed on a detail, and `application_id` for the two
external contributor request tables keyed on an application (Phase 2, step 5).

The link from detail to application lives on the root, because `UNIQUE (application_id,
version_number)` needs both columns in one table. The composite foreign key means the database
guarantees a detail has the same type as its application.

## Migration plan

The service is not live yet, so there is no production data to protect. The migration still runs
against every existing development and test database, so the pre-checks and row-count comparisons
below still apply there.

### Delivery: one PR per phase

Each phase goes in its own PR so each can be reviewed separately. The PRs cascade: each one
targets the previous PR's branch rather than `develop`, and only the first targets `develop`.

| PR | Contents | Targets |
|---|---|---|
| 1 | Phase 0 data fixes (only if the checks find anything) | `develop` |
| 2 | Phase 1: shared Java superclasses | PR 1's branch, or `develop` if there is no PR 1 |
| 3 | Phase 2: Flyway migration | PR 2's branch |
| 4 | Phase 3: JPA mapping switch, documentation | PR 3's branch |

Merge from the top of the stack down: PR 4 into PR 3's branch, then PR 3 into PR 2's, and so on,
so `develop` receives the whole change in one merge. That matters because Phases 2 and 3 can't
exist apart. After the migration alone, the entities no longer match the schema. After the
mapping alone, the code needs tables that don't exist. Neither state ever reaches `develop`.

Things to know when working the stack:

- Each PR's diff shows only its own phase, because it's compared against the branch below it.
- PR 3's branch on its own won't pass the integration tests, since its entities don't match the
  migrated schema. Review PR 3 as SQL. PR 4's branch is the first where the full suite must pass,
  and the cascade is ready to merge once it does.
- A review change lower in the stack has to be merged up into every branch above it before those
  are re-reviewed.
- If `develop` moves while the stack is open, merge it into PR 1's branch (or PR 2's) and cascade
  upwards. Watch for new Flyway migrations on `develop` taking the version number the Phase 2
  migration uses.
- The documentation update goes in PR 4, so the documents never disagree with the code they
  arrive with.

### Phase 0 — Pre-checks against existing development and test databases

Run these before writing the migration. Every one should return zero rows or zero.

```sql
-- 1. UUID collisions between the families (should be impossible, but the root tables depend on it)
SELECT id FROM schedule_work_programme_applications
INTERSECT SELECT id FROM licence_continuation_applications;

SELECT id FROM schedule_work_programme_application_details
INTERSECT SELECT id FROM licence_continuation_application_details;

-- 2. Application reference collisions across the families (prefixes differ, so expected none)
SELECT application_reference FROM schedule_work_programme_applications
INTERSECT SELECT application_reference FROM licence_continuation_applications;

-- 3. Nulls that would block NOT NULL on the root columns
SELECT COUNT(*) FROM licence_continuation_application_details WHERE created_datetime IS NULL;
SELECT COUNT(*) FROM schedule_work_programme_application_details WHERE schedule_work_programme_application_id IS NULL;
SELECT COUNT(*) FROM licence_continuation_application_details WHERE licence_continuation_application_id IS NULL;
```

If check 3 finds continuation rows with no `created_datetime`, either backfill them from the
first audit revision (as V127 did for `created_by_wua_id`) or leave the root column nullable.

**Also check the V66 backfill.** V66 filled in
`licence_continuation_applications.submitted_licence_schedule_detail_id` with a subquery that
compared an application id with a detail id (`lcad2.licence_continuation_application_id =
lcad.id`). It almost certainly matched nothing, so continuations submitted before V66 may have
a null there. The migration copies the column as it is. Fix it with a separate data fix first,
so the new tables aren't blamed for it:

```sql
SELECT a.id, a.application_reference
FROM licence_continuation_applications a
WHERE a.submitted_licence_schedule_detail_id IS NULL
  AND EXISTS (
    SELECT 1 FROM licence_continuation_application_details d
    WHERE d.licence_continuation_application_id = a.id AND d.status <> 'DRAFT'
  );
```

### Phase 1 — Shared Java superclasses, no schema change

Its own PR (PR 2). With no schema change, the existing tests cover it, so it is the one PR in
the stack that can be checked on its own. It removes the duplicated Java, so Phase 3 is left
with mapping annotations, the detail-to-application link and the renamed join columns.

1. Add `@MappedSuperclass` `AbstractLicenceApplication` with the shared fields: `id`,
   `licenceSchedule`, `submittedLicenceScheduleDetail`, `applicationReference`. Leave
   `withdrawalReason` in `LicenceContinuationApplication` for now: the extension table has no
   column for it until Phase 2, and it moves up to the root in Phase 3.
2. Add `@MappedSuperclass` `AbstractLicenceApplicationDetail` with `id`, `versionNumber`,
   `status`, `createdDatetime`, `createdByWuaId`, `submittedDatetime`, `submittedByWuaId`,
   `responsibleOrganisationUnitId`, `updateDeadlineDate`, `updateReason`.
3. Make the four entities extend them and delete the duplicated fields and accessors.
4. Leave the detail-to-application association in each subclass for now. A mapped superclass
   cannot hold a `@ManyToOne` to another mapped superclass, so it moves in Phase 3.
5. Envers only audits a mapped superclass's fields if the superclass is `@Audited` too (or the
   subclass declares `@AuditOverride(forClass = ...)`). Annotate the superclasses and check
   with an integration test that an update to a shared field (for example `status`) still
   writes an audit row.
6. Fold `getCreatedDatetime()` and the other shared getters into the existing
   `LicenceApplication` / `LicenceApplicationDetail` interfaces, or replace the interfaces with
   the superclasses.

`DuplicationUtil` uses `ReflectionUtil.getAllFields`, which includes superclass fields, so
duplication is unaffected. The detail duplication services build new details field by field
and need no change.

### Phase 2 — Flyway migration

Its own PR (PR 3), targeting the Phase 1 PR's branch (see Delivery). One migration,
`V<n>__application_root_tables.sql`. Outline:

```sql
-- 1. Root tables
CREATE TABLE applications (
  id                                   UUID PRIMARY KEY,
  application_type                     TEXT NOT NULL,
  licence_schedule_id                  UUID NOT NULL,
  submitted_licence_schedule_detail_id UUID,
  application_reference                TEXT,
  withdrawal_reason                    TEXT,
  CONSTRAINT applications_licence_schedule_fk FOREIGN KEY (licence_schedule_id) REFERENCES licence_schedules (id),
  CONSTRAINT applications_submitted_licence_schedule_detail_fk FOREIGN KEY (submitted_licence_schedule_detail_id) REFERENCES licence_schedule_details (id),
  CONSTRAINT applications_application_reference_unique UNIQUE (application_reference),
  CONSTRAINT applications_id_type_uk UNIQUE (id, application_type)
);

CREATE INDEX applications_licence_schedule_idx ON applications (licence_schedule_id);
CREATE INDEX applications_submitted_licence_schedule_detail_idx ON applications (submitted_licence_schedule_detail_id);

CREATE TABLE application_details (
  id                               UUID PRIMARY KEY,
  application_id                   UUID NOT NULL,
  application_type                 TEXT NOT NULL,
  version_number                   INTEGER,
  status                           TEXT,
  created_datetime                 TIMESTAMPTZ NOT NULL,
  created_by_wua_id                BIGINT,
  submitted_datetime               TIMESTAMPTZ,
  submitted_by_wua_id              BIGINT,
  responsible_organisation_unit_id INTEGER,
  update_deadline_date             DATE,
  update_reason                    TEXT,
  CONSTRAINT application_details_application_fk FOREIGN KEY (application_id, application_type)
    REFERENCES applications (id, application_type),
  CONSTRAINT application_details_version_uk UNIQUE (application_id, version_number)
);
-- application_details_version_uk's index covers lookups by application_id

-- 2. Copy the shared columns up, keeping every id
INSERT INTO applications (id, application_type, licence_schedule_id, submitted_licence_schedule_detail_id, application_reference)
SELECT id, 'SCHEDULE_AMENDMENT_APPLICATION', licence_schedule_id, submitted_licence_schedule_detail_id, application_reference
FROM schedule_work_programme_applications;

INSERT INTO applications (id, application_type, licence_schedule_id, submitted_licence_schedule_detail_id,
                          application_reference, withdrawal_reason)
SELECT id, 'CONTINUATION_APPLICATION', licence_schedule_id, submitted_licence_schedule_detail_id,
       application_reference, withdrawal_reason
FROM licence_continuation_applications;

INSERT INTO application_details (id, application_id, application_type, version_number, status,
    created_datetime, created_by_wua_id, submitted_datetime, submitted_by_wua_id,
    responsible_organisation_unit_id, update_deadline_date, update_reason)
SELECT id, schedule_work_programme_application_id, 'SCHEDULE_AMENDMENT_APPLICATION', version_number, status,
    created_datetime, created_by_wua_id, submitted_datetime, submitted_by_wua_id,
    responsible_organisation_unit_id, update_deadline_date, update_reason
FROM schedule_work_programme_application_details;

INSERT INTO application_details (id, application_id, application_type, version_number, status,
    created_datetime, created_by_wua_id, submitted_datetime, submitted_by_wua_id,
    responsible_organisation_unit_id, update_deadline_date, update_reason)
SELECT id, licence_continuation_application_id, 'CONTINUATION_APPLICATION', version_number, status,
    created_datetime, created_by_wua_id, submitted_datetime, submitted_by_wua_id,
    responsible_organisation_unit_id, update_deadline_date, update_reason
FROM licence_continuation_application_details;

-- 3. Link each type-specific table to its root and drop the columns that moved.
--    Dropping a column also drops the constraints and indexes on it (swpa_licence_schedule_fk,
--    the *_version_uk constraints, the detail→application FKs, ...).
ALTER TABLE schedule_work_programme_applications
  ADD CONSTRAINT schedule_work_programme_applications_application_fk FOREIGN KEY (id) REFERENCES applications (id),
  DROP COLUMN licence_schedule_id,
  DROP COLUMN submitted_licence_schedule_detail_id,
  DROP COLUMN application_reference,
  ALTER COLUMN steward_wua_id TYPE BIGINT;

-- licence_continuation_applications has nothing left of its own. Re-point the external
-- contributor request table here; the continuation details' reference goes when their
-- licence_continuation_application_id column is dropped below. Then the table is dropped.
ALTER TABLE licence_continuation_external_contributor_request
  DROP CONSTRAINT licence_continuation_external_contributor_request_fk,
  ADD CONSTRAINT licence_continuation_external_contributor_request_fk
    FOREIGN KEY (licence_continuation_application_id) REFERENCES applications (id);

ALTER TABLE schedule_work_programme_application_details
  ADD CONSTRAINT schedule_work_programme_application_details_application_detail_fk FOREIGN KEY (id) REFERENCES application_details (id),
  DROP COLUMN schedule_work_programme_application_id,
  DROP COLUMN version_number,
  DROP COLUMN status,
  DROP COLUMN created_datetime,
  DROP COLUMN created_by_wua_id,
  DROP COLUMN submitted_datetime,
  DROP COLUMN submitted_by_wua_id,
  DROP COLUMN responsible_organisation_unit_id,
  DROP COLUMN update_deadline_date,
  DROP COLUMN update_reason;

ALTER TABLE licence_continuation_application_details
  ADD CONSTRAINT licence_continuation_application_details_application_detail_fk FOREIGN KEY (id) REFERENCES application_details (id),
  DROP COLUMN licence_continuation_application_id,
  DROP COLUMN version_number,
  DROP COLUMN status,
  DROP COLUMN created_datetime,
  DROP COLUMN created_by_wua_id,
  DROP COLUMN submitted_datetime,
  DROP COLUMN submitted_by_wua_id,
  DROP COLUMN responsible_organisation_unit_id,
  DROP COLUMN update_deadline_date,
  DROP COLUMN update_reason;

-- Dropping licence_continuation_application_id above removed the last reference to it.
DROP TABLE licence_continuation_applications;

-- 4. Audit tables. Envers puts revtype, the discriminator and the shared columns in the root
--    audit table. A joined subclass's audit table, and the extension application's secondary
--    audit table, hold only rev, id and their own columns. (rev, id) cannot collide in the root
--    audit tables because ids are unique across families (Phase 0, check 1).
CREATE TABLE applications_aud (
  rev                                  INTEGER NOT NULL,
  revtype                              NUMERIC,
  id                                   UUID NOT NULL,
  application_type                     TEXT,
  licence_schedule_id                  UUID,
  submitted_licence_schedule_detail_id UUID,
  application_reference                TEXT,
  withdrawal_reason                    TEXT,
  CONSTRAINT applications_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT applications_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);
CREATE INDEX applications_aud_rev_idx ON applications_aud (rev);

INSERT INTO applications_aud (rev, revtype, id, application_type, licence_schedule_id,
                              submitted_licence_schedule_detail_id, application_reference)
SELECT rev, revtype, id, 'SCHEDULE_AMENDMENT_APPLICATION', licence_schedule_id,
       submitted_licence_schedule_detail_id, application_reference
FROM schedule_work_programme_applications_aud;
-- ...and the same from licence_continuation_applications_aud, also copying withdrawal_reason.
DROP TABLE licence_continuation_applications_aud;

-- application_details_aud: same pattern, copied from both detail _aud tables.

ALTER TABLE schedule_work_programme_applications_aud
  DROP COLUMN revtype,
  DROP COLUMN licence_schedule_id,
  DROP COLUMN submitted_licence_schedule_detail_id,
  DROP COLUMN application_reference,
  ALTER COLUMN steward_wua_id TYPE BIGINT;
-- ...and drop revtype plus the moved columns from the two detail _aud tables.
```

```sql
-- 5. Rename the child foreign key columns, in each table and its _aud table. Constraints and
--    indexes follow a column rename, so only the column names change.
ALTER TABLE swp_application_request_purpose            RENAME COLUMN schedule_work_programme_application_detail_id  TO application_detail_id;
ALTER TABLE swp_application_request_purpose_aud        RENAME COLUMN schedule_work_programme_application_detail_id  TO application_detail_id;
-- ...and the same for every row of the table below.
```

| Child table | Current column | New column | References |
|---|---|---|---|
| `swp_application_request_purpose` | `schedule_work_programme_application_detail_id` | `application_detail_id` | `schedule_work_programme_application_details` |
| `swp_record_of_decision` | `schedule_work_programme_application_detail_id` | `application_detail_id` | `schedule_work_programme_application_details` |
| `swp_record_of_decision_extension` | `schedule_work_programme_application_detail_id` | `application_detail_id` | `schedule_work_programme_application_details` |
| `swp_record_of_decision_reduction` | `schedule_work_programme_application_detail_id` | `application_detail_id` | `schedule_work_programme_application_details` |
| `swp_record_of_decision_work_programme` | `schedule_work_programme_application_detail_id` | `application_detail_id` | `schedule_work_programme_application_details` |
| `licence_schedule_extension_request` | `schedule_work_programme_application_details_id` | `application_detail_id` | `schedule_work_programme_application_details` |
| `licence_work_programme_amendment_request` | `schedule_work_programme_application_details_id` | `application_detail_id` | `schedule_work_programme_application_details` |
| `licence_work_programme_amendment_summary` | `schedule_work_programme_application_details_id` | `application_detail_id` | `schedule_work_programme_application_details` |
| `licence_schedule_overall_request` | `schedule_work_programme_application_details_id` | `application_detail_id` | `schedule_work_programme_application_details` |
| `licence_continuation_wpa_requirement_request` | `licence_continuation_application_detail_id` | `application_detail_id` | `licence_continuation_application_details` |
| `licence_continuation_other_requirement_request` | `licence_continuation_application_detail_id` | `application_detail_id` | `licence_continuation_application_details` |
| `licence_continuation_licence_operators_request` | `licence_continuation_application_detail_id` | `application_detail_id` | `licence_continuation_application_details` |
| `licence_continuation_supporting_information` | `licence_continuation_application_detail_id` | `application_detail_id` | `licence_continuation_application_details` |
| `swp_external_contributor_request` | `schedule_work_programme_application_id` | `application_id` | `schedule_work_programme_applications` |
| `licence_continuation_external_contributor_request` | `licence_continuation_application_id` | `application_id` | `applications` |

The 13 detail-level children keep their foreign keys to the type-specific detail tables, so the
rename doesn't loosen any type guarantee. The column name says "a detail" and the constraint says
which family.

### Phase 3 — JPA mapping switch

Its own PR (PR 4), targeting the Phase 2 PR's branch (see Delivery).

1. Turn the Phase 1 superclasses into entity roots. The existing convention of
   `@Entity(name = <table>)` carries over. The detail root is joined:

   ```java
   @Audited
   @Entity(name = "application_details")
   @Inheritance(strategy = InheritanceType.JOINED)
   @DiscriminatorColumn(name = "application_type", discriminatorType = DiscriminatorType.STRING)
   public abstract class AbstractLicenceApplicationDetail implements LicenceApplicationDetail { ... }
   ```

   The application root is single-table, and the extension subclass maps its own table as a
   secondary table:

   ```java
   @Audited
   @Entity(name = "applications")
   @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
   @DiscriminatorColumn(name = "application_type", discriminatorType = DiscriminatorType.STRING)
   public abstract class AbstractLicenceApplication implements LicenceApplication { ... }

   @Audited
   @Entity(name = "schedule_work_programme_applications")
   @DiscriminatorValue("SCHEDULE_AMENDMENT_APPLICATION")
   @SecondaryTable(name = "schedule_work_programme_applications", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id"))
   @SecondaryRow(table = "schedule_work_programme_applications", optional = false)
   @SecondaryAuditTable(secondaryTableName = "schedule_work_programme_applications",
       secondaryAuditTableName = "schedule_work_programme_applications_aud")
   public class ScheduleWorkProgrammeApplication extends AbstractLicenceApplication {

     @Column(table = "schedule_work_programme_applications")
     private Long stewardWuaId;
     ...
   }

   @Audited
   @Entity
   @DiscriminatorValue("CONTINUATION_APPLICATION")
   public class LicenceContinuationApplication extends AbstractLicenceApplication { ... }
   ```

   `@SecondaryRow(optional = false)` (Hibernate 6.2+, the project is on 6.6) makes Hibernate
   always insert the extension row, even while `steward_wua_id` is null. Without it, an
   unallocated extension application gets no row, and `swp_external_contributor_request`'s
   foreign key to `schedule_work_programme_applications` fails.

2. The detail subclasses and the extension application keep their `@Entity(name = ...)`, which
   is still the name of their own table (for the extension application, its secondary table),
   and gain `@DiscriminatorValue` with their `ApplicationType` name. Keep the existing `getApplicationType()` overrides. Because the detail entity names don't
   change, the JPQL `FROM licence_continuation_application_details d` and
   `FROM schedule_work_programme_application_details d` queries still resolve.
   `LicenceContinuationApplication` drops `name = "licence_continuation_applications"`, since that
   table no longer exists. Nothing in JPQL uses that entity name.
3. Move the detail-to-application link to the root as
   `@ManyToOne @JoinColumn(name = "application_id") AbstractLicenceApplication application`.
   In each subclass, keep `getLicenceContinuationApplication()` /
   `getScheduleWorkProgrammeApplication()` as typed getters that cast `getApplication()`, so the
   other files that call them don't change. The association is eager today; keep it eager or
   use `Hibernate.unproxy` in the getter, because a lazy proxy of the abstract root can't be cast
   to a subclass.
4. Update what used the old property names:
   - about 10 derived query methods in `LicenceContinuationApplicationDetailRepository` and
     `ScheduleWorkProgrammeApplicationDetailRepository`. Either rename them to the `application`
     property and update their callers, or give them `@Query` annotations so the method names
     can stay.
   - the two JPQL summary queries (`JOIN d.licenceContinuationApplication application`,
     `WHERE latest.licenceContinuationApplication = application`, and the extension equivalents).
5. Keep `steward_wua_id`, `all_licensees_permission_confirmed`, `decision_date` and the four
   term/phase associations in the subclasses (where they already are). Move `withdrawalReason`
   from `LicenceContinuationApplication` to the root, which leaves the continuation subclass with
   no fields of its own. `submittedByWuaId` and `stewardWuaId` are already `Long`, matching
   `BIGINT`.
6. In the 15 child entities, change only the `@JoinColumn` name to `application_detail_id` or
   `application_id` (see the Phase 2, step 5 table). The `@ManyToOne` fields keep their names and
   subclass types, so derived queries on the child repositories don't change. Nothing outside the
   `@JoinColumn`s uses these column names. `LicenceContinuationExternalContributorRequest` still
   maps to `LicenceContinuationApplication`; only the database foreign key behind it now points at
   `applications`.

### Phase 4 — Optional follow-ups

Each of these is independent and can be done later.

- **Real foreign key from `application_decision_details`** to `applications (id,
  application_type)`. The pair already matches the composite key.
- **Stop a row pointing at a root row of the other family.** After the migration,
  `licence_continuation_external_contributor_request.application_id` and
  `schedule_work_programme_applications.id` only have to exist in `applications`, whatever its
  type. To close that gap, give each table an unmapped column with a fixed value and point a
  composite foreign key at the root:
  ```sql
  ALTER TABLE licence_continuation_external_contributor_request
    ADD COLUMN application_type TEXT NOT NULL DEFAULT 'CONTINUATION_APPLICATION'
      CHECK (application_type = 'CONTINUATION_APPLICATION'),
    DROP CONSTRAINT licence_continuation_external_contributor_request_fk,
    ADD CONSTRAINT licence_continuation_external_contributor_request_fk
      FOREIGN KEY (application_id, application_type)
      REFERENCES applications (id, application_type);
  ```
  Hibernate never writes the column, so the default fills it in. The same applies to
  `schedule_work_programme_applications` and the two type-specific detail tables (those point at
  `application_details`, which needs its own `UNIQUE (id, application_type)`).
- **Collapse the type switches.** `ApplicationService.getApplicationByIdOrThrow` can become one
  repository lookup on the root. `DocumentLinkingService` and the two work area services are the
  next candidates.
- **Leave `licence_event_cache` without a foreign key.** It is a rebuilt read model, and a
  constraint would make rebuilds order-dependent.

## Documentation

Under the `changing-documented-licence-areas` skill, the documentation change ships with the code
that makes it true, in the Phase 3 PR (PR 4):

- `licence-applications-data-model.md`: add the two root tables, remove
  `licence_continuation_applications`, cut the other three table blocks down to their
  type-specific columns, redraw the ERD with the root layer on top, rewrite the column reference
  section, and update the caveat about WUA id types. Delete §11.3 (`created_datetime` becomes one
  `NOT NULL` column on the root) and the §11.4 caveat that the extension detail foreign key is
  spelled two ways, and rename the child foreign key columns throughout (Phase 2, step 5).
  Document the discriminator values, that each type-specific row shares its id with its root row, that every
  extension application has a `schedule_work_programme_applications` row even with no steward,
  and that `licence_continuation_external_contributor_request` now references `applications`.
- `application-decisions-data-model.md` and `licence-event-cache-data-model.md`: update the
  passages that say there is no single application table to reference.
- `.claude/skills/changing-documented-licence-areas/SKILL.md`: its table and examples describe
  the four tables as holding everything.
- `documentation/adr/0007-event-tracker-data-retrieval-and-materialisation.md` has SQL against
  the old columns. ADRs record a decision at a point in time, so leave it, or add a one-line note
  that the shared columns have moved.

Anyone with draft reporting queries or test data scripts against the four tables needs telling
when the stack merges to `develop`: `licence_continuation_applications` is gone, the other three
keep their names but status, version, timestamps and references now need a join to the root, and
the child foreign key columns are renamed.

## What does not change

- Every UUID, so `teams.scope_id`, `application_decision_details.application_id`,
  `licence_event_cache.application_id` and file usage ids stay valid without updates.
- The names of the three remaining existing tables, and their type-specific columns.
- All 15 child tables, and every column in them except the foreign key column, which is renamed.
  14 of the foreign keys still point at the same table; only
  `licence_continuation_external_contributor_request`'s moves.
- `ApplicationType`: its names become the discriminator values.
- Application reference formats (`LMS/EAA/...`, `LMS/CA/...`) and how they're numbered, as long
  as the check below passes.
- jOOQ codegen, which only generates `licences` and `licence_event_cache`.
- Controllers, templates, validators, access rule annotations and argument resolvers.

## Risk areas — prove with tests before the stack merges to `develop`

**Reference numbering must stay per family.** Each service numbers references with
`countByVersionNumberAndSubmittedDatetimeBetween` on its own detail repository. On a joined
subclass Hibernate inner-joins the subclass table, so the count should stay per family. Write an
integration test that submits one of each type in the same year and asserts both get `/1`.

**An id of the wrong type must give a 404, not a 500 or the wrong page.** The argument
resolvers call `findById` on subclass repositories. Requesting a continuation detail id through
an extension URL should find nothing, because the inner join to the subclass table finds no
row. Applications are single-table, so a lookup by id on
`LicenceContinuationApplicationRepository` or `ScheduleWorkProgrammeApplicationRepository` relies
on Hibernate filtering by the discriminator instead. Write an integration test for each resolver
and for each application-by-id lookup.

**Every extension application must have its secondary row.** Check that creating an extension
application with no steward writes a `schedule_work_programme_applications` row (the
`@SecondaryRow(optional = false)` above), and that adding an external contributor request to it
then succeeds.

**Envers audit tables.** Envers expects `revtype`, the discriminator and the shared columns in
each root audit table. For the joined detail hierarchy, each subclass audit table holds only
`rev`, `id` and the subclass's columns; the extension application's `@SecondaryAuditTable` follows
the same shape. Generate the
expected schema once (`spring.jpa.hibernate.ddl-auto=validate` on a scratch run of the
integration tests) to confirm the hand-written `_aud` changes match.

**Casting the application link.** The typed getters in step 3 cast the root association. If it is
ever made lazy, the cast fails on a proxy. Cover each typed getter with a test that loads a
detail from the database.

**Extra joins.** Every detail load and every extension application load now joins the root and
a second table, and any query against `application_details` outer-joins both type tables.
Continuation applications are read from `applications` alone. At this data volume that's cheap, but check the work area
and search queries, and the continuation detail's four eager term/phase associations, for new
N+1s. Fix any with targeted `@EntityGraph`s rather than batch fetching.

**Existing environments.** The service is not live, so there is no production data at risk. The
migration still rewrites both families in every development and test database. Run it against a
copy of the busiest one first and compare row counts per type before and after, for the main and
audit tables.

## Decisions

1. Table names are `applications` and `application_details`.
2. Columns used by only one family stay in that family's own table, not in the root tables
   (joined inheritance for details; single-table plus a secondary table for applications).
3. `withdrawal_reason` goes on `applications`, because withdrawal is being implemented for every
   application type.
4. `licence_continuation_applications` is dropped, since it has no columns of its own once
   `withdrawal_reason` moves. Its one child foreign key is re-pointed to `applications`.
5. `steward_wua_id` stays extension-only, in `schedule_work_programme_applications`.
6. The type-specific tables keep their existing names.
7. The child foreign key columns are renamed in the same migration, to `application_detail_id`
   or `application_id`.
8. Each phase is its own PR. The PRs cascade off each other and are merged up the stack before
   the whole change goes into `develop` in one merge (see Delivery). With no live data, separate
   releases aren't needed to manage risk; the split is for review.

## Open decisions

None.
