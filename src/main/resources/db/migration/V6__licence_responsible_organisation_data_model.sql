CREATE TABLE licence_responsible_organisations(
    licence_id INTEGER,
    responsible_organisation_id INTEGER,
    managed_by_lms BOOLEAN,
    PRIMARY KEY (licence_id, responsible_organisation_id),
    CONSTRAINT licence_responsible_organisations_fk FOREIGN KEY (licence_id) REFERENCES licences (id)
);

CREATE INDEX licence_responsible_organisations_idx1 ON licence_responsible_organisations(licence_id);

CREATE TABLE licence_responsible_organisations_aud(
    rev SERIAL,
    revtype NUMERIC,
    licence_id INTEGER,
    responsible_organisation_id INTEGER,
    managed_by_lms BOOLEAN,
    CONSTRAINT licence_responsible_organisations_aud_pk PRIMARY KEY (rev, licence_id, responsible_organisation_id),
    CONSTRAINT licence_responsible_organisations_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_responsible_organisations_aud_rev_idx ON licence_responsible_organisations_aud (rev);