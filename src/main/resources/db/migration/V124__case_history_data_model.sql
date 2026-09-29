CREATE TABLE case_events (
    id               UUID PRIMARY KEY,
    application_type TEXT        NOT NULL,
    application_id   UUID        NOT NULL,
    event_type       TEXT        NOT NULL,
    event_instant    TIMESTAMPTZ NOT NULL,
    user_wua_id      BIGINT,
    proxy_wua_id     BIGINT,
    is_system        BOOLEAN     NOT NULL DEFAULT FALSE,
    payload          JSONB
);

CREATE INDEX case_events_application_idx ON case_events (application_type, application_id, event_instant);

CREATE TABLE case_notes (
    id               UUID PRIMARY KEY,
    application_type TEXT        NOT NULL,
    application_id   UUID        NOT NULL,
    note_text        TEXT        NOT NULL,
    created_instant  TIMESTAMPTZ NOT NULL,
    author_wua_id    BIGINT      NOT NULL,
    proxy_wua_id     BIGINT
);

CREATE INDEX case_notes_application_idx ON case_notes (application_type, application_id);

CREATE TABLE case_events_aud (
    rev              SERIAL,
    revtype          NUMERIC,
    id               UUID,
    application_type TEXT,
    application_id   UUID,
    event_type       TEXT,
    event_instant    TIMESTAMPTZ,
    user_wua_id      BIGINT,
    proxy_wua_id     BIGINT,
    is_system        BOOLEAN,
    payload          JSONB,
    CONSTRAINT case_events_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT case_events_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX case_events_aud_rev_idx ON case_events_aud (rev);

CREATE TABLE case_notes_aud (
    rev              SERIAL,
    revtype          NUMERIC,
    id               UUID,
    application_type TEXT,
    application_id   UUID,
    note_text        TEXT,
    created_instant  TIMESTAMPTZ,
    author_wua_id    BIGINT,
    proxy_wua_id     BIGINT,
    CONSTRAINT case_notes_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT case_notes_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX case_notes_aud_rev_idx ON case_notes_aud (rev);
