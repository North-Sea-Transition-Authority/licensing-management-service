CREATE TABLE application_decision_details (
    id                UUID PRIMARY KEY,
    application_type  TEXT        NOT NULL,
    application_id    UUID        NOT NULL,
    version_number    INTEGER     NOT NULL,
    status            TEXT        NOT NULL,
    created_datetime  TIMESTAMPTZ NOT NULL,
    created_by_wua_id BIGINT      NOT NULL,
    CONSTRAINT application_decision_details_version_uk
        UNIQUE (application_type, application_id, version_number)
);

CREATE TABLE application_decision_details_aud (
    rev               SERIAL,
    revtype           NUMERIC,
    id                UUID,
    application_type  TEXT,
    application_id    UUID,
    version_number    INTEGER,
    status            TEXT,
    created_datetime  TIMESTAMPTZ,
    created_by_wua_id BIGINT,
    CONSTRAINT application_decision_details_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT application_decision_details_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX application_decision_details_aud_rev_idx ON application_decision_details_aud (rev);
