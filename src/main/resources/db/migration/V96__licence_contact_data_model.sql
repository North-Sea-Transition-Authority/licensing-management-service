CREATE TABLE licence_contact(
  id UUID PRIMARY KEY,
  licence_id INTEGER NOT NULL,
  responsible_organisation_id INTEGER NOT NULL,
  contact_email TEXT NOT NULL,
  CONSTRAINT licence_contact_licensee_fk FOREIGN KEY (licence_id, responsible_organisation_id)  REFERENCES licence_responsible_organisations (licence_id, responsible_organisation_id) ON DELETE CASCADE,
  CONSTRAINT licence_contact_licensee_unique UNIQUE (licence_id, responsible_organisation_id)
);

CREATE INDEX licence_contact_responsible_organisation_idx ON licence_contact(responsible_organisation_id);

CREATE TABLE licence_contact_aud(
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  licence_id INTEGER,
  responsible_organisation_id INTEGER,
  contact_email TEXT,
  CONSTRAINT licence_contact_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT licence_contact_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_contact_aud_rev_idx ON licence_contact_aud (rev);
