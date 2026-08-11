CREATE TABLE licence_statuses(
    id UUID PRIMARY KEY,
    licence_id INTEGER NOT NULL,
    status TEXT NOT NULL,
    status_date DATE NOT NULL,
    CONSTRAINT licence_statuses_licence_fk FOREIGN KEY (licence_id) REFERENCES licences (id)
);

CREATE INDEX licence_statuses_idx ON licence_statuses(licence_id);

CREATE TABLE licence_statuses_aud(
    rev SERIAL,
    revtype NUMERIC,
    id UUID,
    licence_id INTEGER,
    status TEXT,
    status_date DATE,
    CONSTRAINT licence_statuses_aud_pk PRIMARY KEY (rev, id),
    CONSTRAINT licence_statuses_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_statuses_aud_rev_idx ON licence_statuses_aud (rev);
