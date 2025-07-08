CREATE TABLE licence_schedules(
    id UUID PRIMARY KEY,
    licence_id INTEGER,
    CONSTRAINT licence_schedules_fk FOREIGN KEY (licence_id) references licences (id)
);

CREATE INDEX licence_schedules_idx ON licence_schedules(licence_id);

CREATE TABLE licence_schedules_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_id INTEGER,
    CONSTRAINT licence_schedules_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_schedules_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_schedules_aud_rev_idx ON licence_schedules_aud (rev);