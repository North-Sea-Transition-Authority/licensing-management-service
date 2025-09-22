CREATE TABLE licence_schedule_extension_request(
    id UUID PRIMARY KEY,
    schedule_work_programme_application_details_id UUID NOT NULL,
    extension_duration_days INTEGER NOT NULL,
    extension_duration_months INTEGER NOT NULL,
    extension_duration_years INTEGER NOT NULL,
    explanation TEXT,
    CONSTRAINT licence_schedule_extension_request_fk FOREIGN KEY (schedule_work_programme_application_details_id) references schedule_work_programme_application_details (id)
);

CREATE INDEX licence_schedule_extension_request_idx ON licence_schedule_extension_request(schedule_work_programme_application_details_id);

CREATE TABLE licence_schedule_extension_request_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    schedule_work_programme_application_details_id UUID NOT NULL,
    extension_duration_days INTEGER NOT NULL,
    extension_duration_months INTEGER NOT NULL,
    extension_duration_years INTEGER NOT NULL,
    explanation TEXT,
    CONSTRAINT licence_schedule_extension_request_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_schedule_extension_request_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_schedule_extension_request_aud_rev_idx ON licence_schedule_extension_request_aud (rev);