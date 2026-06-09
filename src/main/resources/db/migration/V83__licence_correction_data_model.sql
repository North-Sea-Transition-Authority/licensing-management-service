CREATE TABLE licence_corrections(
  id UUID PRIMARY KEY,
  licence_id INTEGER NOT NULL,
  correction_reference TEXT NOT NULL,
  reason TEXT NOT NULL,
  status TEXT NOT NULL,
  allocated_to_wua_id BIGINT NOT NULL,
  created_instant TIMESTAMPTZ NOT NULL,
  CONSTRAINT licence_corrections_licence_fk FOREIGN KEY (licence_id) REFERENCES licences (id)
);

CREATE INDEX licence_corrections_licence_idx ON licence_corrections(licence_id);

CREATE UNIQUE INDEX licence_corrections_single_open_idx
  ON licence_corrections (licence_id)
  WHERE status = 'IN_PROGRESS';

CREATE TABLE licence_corrections_aud(
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  licence_id INTEGER,
  correction_reference TEXT,
  reason TEXT,
  status TEXT,
  allocated_to_wua_id BIGINT,
  created_instant TIMESTAMPTZ,
  CONSTRAINT licence_corrections_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT licence_corrections_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_corrections_aud_rev_idx ON licence_corrections_aud (rev);

