-- Moves the columns shared by extension and amendment applications and continuation applications
-- into central applications and application_details tables, keyed by an application_type
-- discriminator. See documentation/investigations/central-application-tables.md.

-- 1. Root tables

CREATE TABLE applications (
  id                    UUID PRIMARY KEY,
  application_type      TEXT NOT NULL,
  licence_id            INTEGER NOT NULL,
  application_reference TEXT,
  withdrawal_reason     TEXT,
  withdrawn_datetime    TIMESTAMPTZ,
  CONSTRAINT applications_licence_fk FOREIGN KEY (licence_id) REFERENCES licences (id),
  CONSTRAINT applications_application_reference_unique UNIQUE (application_reference),
  CONSTRAINT applications_id_type_uk UNIQUE (id, application_type)
);

CREATE INDEX applications_licence_idx ON applications (licence_id);

CREATE TABLE schedule_applications (
  id                                   UUID PRIMARY KEY,
  licence_schedule_id                  UUID NOT NULL,
  submitted_licence_schedule_detail_id UUID,
  CONSTRAINT schedule_applications_application_fk FOREIGN KEY (id) REFERENCES applications (id),
  CONSTRAINT schedule_applications_licence_schedule_fk FOREIGN KEY (licence_schedule_id) REFERENCES licence_schedules (id),
  CONSTRAINT schedule_applications_submitted_licence_schedule_detail_fk
    FOREIGN KEY (submitted_licence_schedule_detail_id) REFERENCES licence_schedule_details (id)
);

CREATE INDEX schedule_applications_licence_schedule_idx ON schedule_applications (licence_schedule_id);
CREATE INDEX schedule_applications_submitted_licence_schedule_detail_idx
  ON schedule_applications (submitted_licence_schedule_detail_id);

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

-- 2. Copy the shared columns up, keeping every id. The licence comes from each application's
--    schedule.

INSERT INTO applications (id, application_type, licence_id, application_reference)
SELECT a.id, 'SCHEDULE_AMENDMENT_APPLICATION', s.licence_id, a.application_reference
FROM schedule_work_programme_applications a
JOIN licence_schedules s ON s.id = a.licence_schedule_id;

INSERT INTO applications (id, application_type, licence_id, application_reference, withdrawal_reason, withdrawn_datetime)
SELECT a.id, 'CONTINUATION_APPLICATION', s.licence_id, a.application_reference, a.withdrawal_reason, a.withdrawn_datetime
FROM licence_continuation_applications a
JOIN licence_schedules s ON s.id = a.licence_schedule_id;

INSERT INTO schedule_applications (id, licence_schedule_id, submitted_licence_schedule_detail_id)
SELECT id, licence_schedule_id, submitted_licence_schedule_detail_id FROM schedule_work_programme_applications
UNION ALL
SELECT id, licence_schedule_id, submitted_licence_schedule_detail_id FROM licence_continuation_applications;

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

-- 3. Link the type-specific tables to the roots and drop the columns that moved. Dropping a
--    column also drops the constraints and indexes on it.

-- Re-point both external contributor request tables to applications first:
-- licence_continuation_applications is about to be dropped, and extension applications without
-- a steward are about to lose their schedule_work_programme_applications row.
ALTER TABLE licence_continuation_external_contributor_request
  DROP CONSTRAINT licence_continuation_external_contributor_request_fk,
  ADD CONSTRAINT licence_continuation_external_contributor_request_fk
    FOREIGN KEY (licence_continuation_application_id) REFERENCES applications (id);

ALTER TABLE swp_external_contributor_request
  DROP CONSTRAINT swp_external_contributor_request_fk,
  ADD CONSTRAINT swp_external_contributor_request_fk
    FOREIGN KEY (schedule_work_programme_application_id) REFERENCES applications (id);

-- The detail tables go before schedule_work_programme_applications: dropping their application
-- columns removes the references that would otherwise block deleting steward-less rows below.
ALTER TABLE schedule_work_programme_application_details
  ADD CONSTRAINT swpad_application_detail_fk
    FOREIGN KEY (id) REFERENCES application_details (id),
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
  ADD CONSTRAINT lcad_application_detail_fk
    FOREIGN KEY (id) REFERENCES application_details (id),
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

-- schedule_work_programme_applications holds extension-only application data, currently just the
-- steward: one row per extension application that has one.
DELETE FROM schedule_work_programme_applications WHERE steward_wua_id IS NULL;

ALTER TABLE schedule_work_programme_applications
  ADD CONSTRAINT swpa_application_fk FOREIGN KEY (id) REFERENCES applications (id),
  DROP COLUMN licence_schedule_id,
  DROP COLUMN submitted_licence_schedule_detail_id,
  DROP COLUMN application_reference,
  ALTER COLUMN steward_wua_id TYPE BIGINT,
  ALTER COLUMN steward_wua_id SET NOT NULL;

-- Dropping licence_continuation_application_id above removed the last reference to it.
DROP TABLE licence_continuation_applications;

-- 4. Audit tables. Envers puts revtype, the discriminator and the shared columns in the root
--    audit table, and a joined subclass's audit table holds only rev, id and its own columns.
--    The schedule_applications and schedule_work_programme_applications entities are entities in
--    their own right, so their audit tables keep revtype. (rev, id) cannot collide in the root
--    audit tables because ids are unique across families.

CREATE TABLE applications_aud (
  rev                   INTEGER NOT NULL,
  revtype               NUMERIC,
  id                    UUID NOT NULL,
  application_type      TEXT,
  licence_id            INTEGER,
  application_reference TEXT,
  withdrawal_reason     TEXT,
  withdrawn_datetime    TIMESTAMPTZ,
  CONSTRAINT applications_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT applications_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX applications_aud_rev_idx ON applications_aud (rev);

-- An application never changes schedule, so each audit row's licence comes from the schedule
-- that audit row records.
INSERT INTO applications_aud (rev, revtype, id, application_type, licence_id, application_reference)
SELECT a.rev, a.revtype, a.id, 'SCHEDULE_AMENDMENT_APPLICATION', s.licence_id, a.application_reference
FROM schedule_work_programme_applications_aud a
LEFT JOIN licence_schedules s ON s.id = a.licence_schedule_id;

INSERT INTO applications_aud (rev, revtype, id, application_type, licence_id, application_reference,
                              withdrawal_reason, withdrawn_datetime)
SELECT a.rev, a.revtype, a.id, 'CONTINUATION_APPLICATION', s.licence_id, a.application_reference,
       a.withdrawal_reason, a.withdrawn_datetime
FROM licence_continuation_applications_aud a
LEFT JOIN licence_schedules s ON s.id = a.licence_schedule_id;

CREATE TABLE schedule_applications_aud (
  rev                                  INTEGER NOT NULL,
  revtype                              NUMERIC,
  id                                   UUID NOT NULL,
  licence_schedule_id                  UUID,
  submitted_licence_schedule_detail_id UUID,
  CONSTRAINT schedule_applications_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT schedule_applications_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX schedule_applications_aud_rev_idx ON schedule_applications_aud (rev);

INSERT INTO schedule_applications_aud (rev, revtype, id, licence_schedule_id, submitted_licence_schedule_detail_id)
SELECT rev, revtype, id, licence_schedule_id, submitted_licence_schedule_detail_id
FROM schedule_work_programme_applications_aud
UNION ALL
SELECT rev, revtype, id, licence_schedule_id, submitted_licence_schedule_detail_id
FROM licence_continuation_applications_aud;

CREATE TABLE application_details_aud (
  rev                              INTEGER NOT NULL,
  revtype                          NUMERIC,
  id                               UUID NOT NULL,
  application_id                   UUID,
  application_type                 TEXT,
  version_number                   INTEGER,
  status                           TEXT,
  created_datetime                 TIMESTAMPTZ,
  created_by_wua_id                BIGINT,
  submitted_datetime               TIMESTAMPTZ,
  submitted_by_wua_id              BIGINT,
  responsible_organisation_unit_id INTEGER,
  update_deadline_date             DATE,
  update_reason                    TEXT,
  CONSTRAINT application_details_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT application_details_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX application_details_aud_rev_idx ON application_details_aud (rev);

INSERT INTO application_details_aud (rev, revtype, id, application_id, application_type, version_number,
    status, created_datetime, created_by_wua_id, submitted_datetime, submitted_by_wua_id,
    responsible_organisation_unit_id, update_deadline_date, update_reason)
SELECT rev, revtype, id, schedule_work_programme_application_id, 'SCHEDULE_AMENDMENT_APPLICATION', version_number,
    status, created_datetime, created_by_wua_id, submitted_datetime, submitted_by_wua_id,
    responsible_organisation_unit_id, update_deadline_date, update_reason
FROM schedule_work_programme_application_details_aud;

INSERT INTO application_details_aud (rev, revtype, id, application_id, application_type, version_number,
    status, created_datetime, created_by_wua_id, submitted_datetime, submitted_by_wua_id,
    responsible_organisation_unit_id, update_deadline_date, update_reason)
SELECT rev, revtype, id, licence_continuation_application_id, 'CONTINUATION_APPLICATION', version_number,
    status, created_datetime, created_by_wua_id, submitted_datetime, submitted_by_wua_id,
    responsible_organisation_unit_id, update_deadline_date, update_reason
FROM licence_continuation_application_details_aud;

DROP TABLE licence_continuation_applications_aud;

-- Audit rows recorded before any steward was assigned stay, with steward_wua_id null, as history.
ALTER TABLE schedule_work_programme_applications_aud
  DROP COLUMN licence_schedule_id,
  DROP COLUMN submitted_licence_schedule_detail_id,
  DROP COLUMN application_reference,
  ALTER COLUMN steward_wua_id TYPE BIGINT;

ALTER TABLE schedule_work_programme_application_details_aud
  DROP COLUMN revtype,
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

ALTER TABLE licence_continuation_application_details_aud
  DROP COLUMN revtype,
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

-- 5. Rename the child foreign key columns to application_detail_id or application_id, in each
--    table and its audit table. Constraints and indexes follow a column rename.

ALTER TABLE swp_application_request_purpose RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;
ALTER TABLE swp_application_request_purpose_aud RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;
ALTER TABLE swp_record_of_decision RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;
ALTER TABLE swp_record_of_decision_aud RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;
ALTER TABLE swp_record_of_decision_extension RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;
ALTER TABLE swp_record_of_decision_extension_aud RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;
ALTER TABLE swp_record_of_decision_reduction RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;
ALTER TABLE swp_record_of_decision_reduction_aud RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;
ALTER TABLE swp_record_of_decision_work_programme RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;
ALTER TABLE swp_record_of_decision_work_programme_aud RENAME COLUMN schedule_work_programme_application_detail_id TO application_detail_id;

ALTER TABLE licence_schedule_extension_request RENAME COLUMN schedule_work_programme_application_details_id TO application_detail_id;
ALTER TABLE licence_schedule_extension_request_aud RENAME COLUMN schedule_work_programme_application_details_id TO application_detail_id;
ALTER TABLE licence_work_programme_amendment_request RENAME COLUMN schedule_work_programme_application_details_id TO application_detail_id;
ALTER TABLE licence_work_programme_amendment_request_aud RENAME COLUMN schedule_work_programme_application_details_id TO application_detail_id;
ALTER TABLE licence_work_programme_amendment_summary RENAME COLUMN schedule_work_programme_application_details_id TO application_detail_id;
ALTER TABLE licence_work_programme_amendment_summary_aud RENAME COLUMN schedule_work_programme_application_details_id TO application_detail_id;
ALTER TABLE licence_schedule_supporting_information RENAME COLUMN schedule_work_programme_application_details_id TO application_detail_id;
ALTER TABLE licence_schedule_supporting_information_aud RENAME COLUMN schedule_work_programme_application_details_id TO application_detail_id;

ALTER TABLE licence_continuation_wpa_requirement_request RENAME COLUMN licence_continuation_application_detail_id TO application_detail_id;
ALTER TABLE licence_continuation_wpa_requirement_request_aud RENAME COLUMN licence_continuation_application_detail_id TO application_detail_id;
ALTER TABLE licence_continuation_other_requirement_request RENAME COLUMN licence_continuation_application_detail_id TO application_detail_id;
ALTER TABLE licence_continuation_other_requirement_request_aud RENAME COLUMN licence_continuation_application_detail_id TO application_detail_id;
ALTER TABLE licence_continuation_licence_operators_request RENAME COLUMN licence_continuation_application_detail_id TO application_detail_id;
ALTER TABLE licence_continuation_licence_operators_request_aud RENAME COLUMN licence_continuation_application_detail_id TO application_detail_id;
ALTER TABLE licence_continuation_supporting_information RENAME COLUMN licence_continuation_application_detail_id TO application_detail_id;
ALTER TABLE licence_continuation_supporting_information_aud RENAME COLUMN licence_continuation_application_detail_id TO application_detail_id;

ALTER TABLE swp_external_contributor_request RENAME COLUMN schedule_work_programme_application_id TO application_id;
ALTER TABLE swp_external_contributor_request_aud RENAME COLUMN schedule_work_programme_application_id TO application_id;
ALTER TABLE licence_continuation_external_contributor_request RENAME COLUMN licence_continuation_application_id TO application_id;
ALTER TABLE licence_continuation_external_contributor_request_aud RENAME COLUMN licence_continuation_application_id TO application_id;
