CREATE TABLE swp_record_of_decision(
    id UUID PRIMARY KEY,
    schedule_work_programme_application_detail_id UUID NOT NULL,
    extension_decision TEXT,
    work_programme_decision TEXT,
    CONSTRAINT swp_record_of_decision_fk FOREIGN KEY (schedule_work_programme_application_detail_id) REFERENCES schedule_work_programme_application_details (id)
);

CREATE INDEX swp_record_of_decision_idx
    ON swp_record_of_decision(schedule_work_programme_application_detail_id);

CREATE TABLE swp_record_of_decision_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    schedule_work_programme_application_detail_id UUID,
    extension_decision TEXT,
    work_programme_decision TEXT,
    CONSTRAINT swp_record_of_decision_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT swp_record_of_decision_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX swp_record_of_decision_aud_rev_idx ON swp_record_of_decision_aud (rev);
