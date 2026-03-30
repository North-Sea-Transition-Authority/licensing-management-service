CREATE TABLE work_programme_activity_statuses(
    id UUID PRIMARY KEY,
    work_programme_activity_event_reference UUID NOT NULL,
    status TEXT,
    applied_datetime TIMESTAMPTZ,
    licence_transferred_to INTEGER,
    CONSTRAINT work_programme_activity_statuses_licence_fk FOREIGN KEY (licence_transferred_to) references licences (id)
);

CREATE INDEX work_programme_activity_statuses_idx ON work_programme_activity_statuses(work_programme_activity_event_reference);

CREATE TABLE work_programme_activity_statuses_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    work_programme_activity_event_reference UUID ,
    status TEXT,
    applied_datetime TIMESTAMPTZ,
    licence_transferred_to INTEGER,
    CONSTRAINT work_programme_activity_statuses_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT work_programme_activity_statuses_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX work_programme_activity_statuses_aud_rev_idx ON work_programme_activity_statuses_aud (rev);