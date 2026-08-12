CREATE TABLE swp_record_of_decision_extension(
    id UUID PRIMARY KEY,
    schedule_work_programme_application_detail_id UUID NOT NULL,
    term_id UUID,
    phase_id UUID,
    extension_duration_days INTEGER NOT NULL,
    extension_duration_months INTEGER NOT NULL,
    extension_duration_years INTEGER NOT NULL,
    CONSTRAINT swp_record_of_decision_extension_fk FOREIGN KEY (schedule_work_programme_application_detail_id)
        REFERENCES schedule_work_programme_application_details (id)
);

CREATE INDEX swp_record_of_decision_extension_idx
    ON swp_record_of_decision_extension(schedule_work_programme_application_detail_id);

CREATE UNIQUE INDEX swp_record_of_decision_extension_term_unique
    ON swp_record_of_decision_extension(schedule_work_programme_application_detail_id, term_id)
    WHERE term_id IS NOT NULL;

CREATE UNIQUE INDEX swp_record_of_decision_extension_phase_unique
    ON swp_record_of_decision_extension(schedule_work_programme_application_detail_id, phase_id)
    WHERE phase_id IS NOT NULL;

CREATE TABLE swp_record_of_decision_extension_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    schedule_work_programme_application_detail_id UUID,
    term_id UUID,
    phase_id UUID,
    extension_duration_days INTEGER,
    extension_duration_months INTEGER,
    extension_duration_years INTEGER,
    CONSTRAINT swp_record_of_decision_extension_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT swp_record_of_decision_extension_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX swp_record_of_decision_extension_aud_rev_idx ON swp_record_of_decision_extension_aud (rev);
