CREATE TABLE event_comments(
    id UUID PRIMARY KEY,
    event_reference_id UUID NOT NULL,
    comment TEXT,
    status TEXT,
    timestamp TIMESTAMPTZ,
    author_wua_id INTEGER,
    CONSTRAINT event_comments_event_reference_fk FOREIGN KEY (event_reference_id) REFERENCES event_references (id)
);

CREATE INDEX event_comments_event_reference_idx ON event_comments(event_reference_id);

CREATE TABLE event_comments_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    event_reference_id UUID,
    comment TEXT,
    status TEXT,
    timestamp TIMESTAMPTZ,
    author_wua_id INTEGER,
    CONSTRAINT event_comments_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT event_comments_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX event_comments_aud_rev_idx ON event_comments_aud (rev);
