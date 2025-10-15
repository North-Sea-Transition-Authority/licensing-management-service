CREATE TABLE licence_work_programme_amendment_summary(
    id UUID PRIMARY KEY,
    schedule_work_programme_application_details_id UUID NOT NULL,
    licence_Work_Programme_Amendment_Summary_Options TEXT,
    CONSTRAINT licence_work_programme_amendment_summary_fk FOREIGN KEY (schedule_work_programme_application_details_id) references schedule_work_programme_application_details (id)
);

CREATE INDEX licence_work_programme_amendment_summary_idx ON licence_work_programme_amendment_summary(schedule_work_programme_application_details_id);

CREATE TABLE licence_work_programme_amendment_summary_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    schedule_work_programme_application_details_id UUID NOT NULL,
    licence_Work_Programme_Amendment_Summary_Options TEXT,
    CONSTRAINT licence_work_programme_amendment_summary_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_work_programme_amendment_summary_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_work_programme_amendment_summary_aud_rev_idx ON licence_work_programme_amendment_summary_aud (rev);