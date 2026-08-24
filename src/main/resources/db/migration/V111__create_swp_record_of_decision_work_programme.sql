CREATE TABLE swp_record_of_decision_work_programme(
    id UUID PRIMARY KEY,
    schedule_work_programme_application_detail_id UUID NOT NULL,
    work_programme_activity_id UUID NOT NULL,
    decision TEXT NOT NULL,
    amend_duration BOOLEAN,
    amend_text BOOLEAN,
    amended_duration_days INTEGER,
    amended_duration_months INTEGER,
    amended_duration_years INTEGER,
    amended_text TEXT,
    CONSTRAINT swp_record_of_decision_work_programme_fk FOREIGN KEY (schedule_work_programme_application_detail_id)
        REFERENCES schedule_work_programme_application_details (id),
    CONSTRAINT swp_record_of_decision_work_programme_activity_fk FOREIGN KEY (work_programme_activity_id)
        REFERENCES work_programme_activities (id)
);

CREATE INDEX swp_record_of_decision_work_programme_idx
    ON swp_record_of_decision_work_programme(schedule_work_programme_application_detail_id);

CREATE UNIQUE INDEX swp_record_of_decision_work_programme_activity_unique
    ON swp_record_of_decision_work_programme(schedule_work_programme_application_detail_id, work_programme_activity_id);

CREATE TABLE swp_record_of_decision_work_programme_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    schedule_work_programme_application_detail_id UUID,
    work_programme_activity_id UUID,
    decision TEXT,
    amend_duration BOOLEAN,
    amend_text BOOLEAN,
    amended_duration_days INTEGER,
    amended_duration_months INTEGER,
    amended_duration_years INTEGER,
    amended_text TEXT,
    CONSTRAINT swp_record_of_decision_work_programme_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT swp_record_of_decision_work_programme_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX swp_record_of_decision_work_programme_aud_rev_idx
    ON swp_record_of_decision_work_programme_aud (rev);

CREATE TABLE swp_record_of_decision_work_programme_licence(
    id UUID PRIMARY KEY,
    record_of_decision_work_programme_id UUID NOT NULL,
    licence_id INTEGER NOT NULL,
    CONSTRAINT swp_record_of_decision_work_programme_licence_fk FOREIGN KEY (record_of_decision_work_programme_id)
        REFERENCES swp_record_of_decision_work_programme (id),
    CONSTRAINT swp_record_of_decision_work_programme_licence_licence_fk FOREIGN KEY (licence_id)
        REFERENCES licences (id)
);

CREATE INDEX swp_record_of_decision_work_programme_licence_idx
    ON swp_record_of_decision_work_programme_licence(record_of_decision_work_programme_id);

CREATE UNIQUE INDEX swp_record_of_decision_work_programme_licence_unique
    ON swp_record_of_decision_work_programme_licence(record_of_decision_work_programme_id, licence_id);

CREATE TABLE swp_record_of_decision_work_programme_licence_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    record_of_decision_work_programme_id UUID,
    licence_id INTEGER,
    CONSTRAINT swp_record_of_decision_work_programme_licence_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT swp_record_of_decision_work_programme_licence_aud_rev_fk FOREIGN KEY (rev)
        REFERENCES audit_revisions (rev)
);

CREATE INDEX swp_record_of_decision_work_programme_licence_aud_rev_idx
    ON swp_record_of_decision_work_programme_licence_aud (rev);
