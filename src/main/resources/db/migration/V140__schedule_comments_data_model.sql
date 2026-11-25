CREATE TABLE schedule_comments (
  id UUID PRIMARY KEY,
  licence_schedule_id UUID NOT NULL,
  comment TEXT NOT NULL,
  timestamp TIMESTAMPTZ NOT NULL,
  author_wua_id INTEGER,
  CONSTRAINT schedule_comments_licence_schedule_fk FOREIGN KEY (licence_schedule_id) REFERENCES licence_schedules (id)
);

CREATE INDEX schedule_comments_licence_schedule_idx ON schedule_comments (licence_schedule_id);

CREATE TABLE schedule_comments_aud (
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  licence_schedule_id UUID,
  comment TEXT,
  timestamp TIMESTAMPTZ,
  author_wua_id INTEGER,
  CONSTRAINT schedule_comments_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT schedule_comments_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX schedule_comments_aud_rev_idx ON schedule_comments_aud (rev);
