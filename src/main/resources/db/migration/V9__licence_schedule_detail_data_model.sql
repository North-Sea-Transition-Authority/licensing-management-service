CREATE TABLE licence_schedule_details(
    id UUID PRIMARY KEY,
    licence_schedule_id UUID,
    CONSTRAINT licence_schedule_details_fk FOREIGN KEY (licence_schedule_id) references licence_schedules (id)
);

CREATE INDEX licence_schedule_details_idx ON licence_schedule_details(licence_schedule_id);

CREATE TABLE licence_schedule_details_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_schedule_id UUID,
    CONSTRAINT licence_schedule_details_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_schedule_details_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_schedule_details_aud_rev_idx ON licence_schedule_details_aud (rev);