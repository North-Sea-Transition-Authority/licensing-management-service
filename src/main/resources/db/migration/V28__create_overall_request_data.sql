CREATE TABLE licence_schedule_overall_request(
    id UUID PRIMARY KEY,
    schedule_work_programme_application_details_id UUID NOT NULL,
    licence_Progress TEXT,
    reason_For_Amendment TEXT,
    plan_During_Extension TEXT,
    impact_On_Deliverables TEXT,
    CONSTRAINT licence_schedule_overall_request_fk FOREIGN KEY (schedule_work_programme_application_details_id) references schedule_work_programme_application_details (id)
);

CREATE INDEX licence_schedule_overall_request_idx ON licence_schedule_overall_request(schedule_work_programme_application_details_id);

CREATE TABLE licence_schedule_overall_request_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    schedule_work_programme_application_details_id UUID NOT NULL,
    licence_Progress TEXT,
    reason_For_Amendment TEXT,
    plan_During_Extension TEXT,
    impact_On_Deliverables TEXT,
    CONSTRAINT licence_schedule_overall_request_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_schedule_overall_request_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_schedule_overall_request_aud_rev_idx ON licence_schedule_overall_request_aud (rev);