CREATE TABLE licence_position_changes (
  id                  UUID PRIMARY KEY,
  licence_position_id UUID   NOT NULL,
  operations          JSONB  NOT NULL,
  change_order        BIGINT NOT NULL,
  status              TEXT   NOT NULL,
  CONSTRAINT licence_position_changes_licence_position_fk FOREIGN KEY (licence_position_id) REFERENCES licence_positions(id)
);

CREATE INDEX licence_position_changes_licence_position_idx ON licence_position_changes (licence_position_id);

CREATE TABLE licence_position_changes_aud (
  rev SERIAL,
  revtype NUMERIC,
  id                  UUID,
  licence_position_id UUID,
  operations          JSONB,
  change_order        BIGINT,
  status              TEXT,
  CONSTRAINT licence_position_changes_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT licence_position_changes_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_position_changes_aud_rev_idx ON licence_position_changes_aud (rev);
