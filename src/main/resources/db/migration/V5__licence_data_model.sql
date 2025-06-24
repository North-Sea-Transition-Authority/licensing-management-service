CREATE TABLE licences(
    id INTEGER PRIMARY KEY,
    type TEXT NOT NULL,
    subtype TEXT,
    prefix TEXT,
    licence_number TEXT
);

CREATE TABLE licences_aud(
    rev SERIAL,
    revtype NUMERIC,
    id INTEGER,
    type TEXT,
    subtype TEXT,
    prefix TEXT,
    licence_number TEXT,
    CONSTRAINT licences_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licences_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licences_aud_rev_idx ON licences_aud (rev);