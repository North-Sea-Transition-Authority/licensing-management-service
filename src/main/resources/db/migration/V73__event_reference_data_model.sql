CREATE TABLE event_references(
    id UUID PRIMARY KEY,
    licence_schedule_id UUID,
    CONSTRAINT event_references_licence_schedule_fk FOREIGN KEY (licence_schedule_id) references licence_schedules (id)
);

CREATE INDEX event_references_idx ON event_references(licence_schedule_id);

CREATE TABLE event_references_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_schedule_id UUID,
    CONSTRAINT event_references_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT event_references_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX event_references_aud_rev_idx ON event_references_aud (rev);