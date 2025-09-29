CREATE TABLE licence_work_programme_amendment_request(
    id UUID PRIMARY KEY,
    work_programme_activity_id UUID,
    schedule_work_programme_application_details_id UUID NOT NULL,
    CONSTRAINT licence_work_programme_amendment_request_fk FOREIGN KEY (schedule_work_programme_application_details_id) references schedule_work_programme_application_details (id)
);

CREATE INDEX licence_work_programme_amendment_request_idx ON licence_work_programme_amendment_request(schedule_work_programme_application_details_id);

CREATE TABLE licence_work_programme_amendment_request_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    work_programme_activity_id UUID,
    schedule_work_programme_application_details_id UUID NOT NULL,
    CONSTRAINT licence_work_programme_amendment_request_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_work_programme_amendment_request_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_work_programme_amendment_request_aud_rev_idx ON licence_work_programme_amendment_request_aud (rev);