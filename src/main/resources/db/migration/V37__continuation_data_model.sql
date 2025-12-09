CREATE TABLE licence_continuation_applications(
  id UUID PRIMARY KEY,
  licence_schedule_detail_id UUID,
  CONSTRAINT licence_continuation_applications_fk FOREIGN KEY (licence_schedule_detail_id) references licence_schedule_details (id)
);

CREATE INDEX licence_continuation_applications_idx ON licence_continuation_applications(licence_schedule_detail_id);

CREATE TABLE licence_continuation_applications_aud(
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  licence_schedule_detail_id UUID,
  CONSTRAINT licence_continuation_applications_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT licence_continuation_applications_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_continuation_applications_aud_rev_idx ON licence_continuation_applications_aud (rev);

CREATE TABLE licence_continuation_application_details(
  id UUID PRIMARY KEY,
  licence_continuation_application_id UUID,
  version_number INTEGER,
  status TEXT,
  created_date_time TIMESTAMPTZ,
  CONSTRAINT licence_continuation_application_details_fk FOREIGN KEY (licence_continuation_application_id) references licence_continuation_applications (id)
);

CREATE INDEX licence_continuation_application_details_idx ON licence_continuation_application_details(licence_continuation_application_id);

CREATE TABLE licence_continuation_application_details_aud(
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  licence_continuation_application_id UUID,
  version_number INTEGER,
  status TEXT,
  created_date_time TIMESTAMPTZ,
  CONSTRAINT licence_continuation_application_details_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT licence_continuation_application_details_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX licence_continuation_application_details_aud_rev_idx ON licence_continuation_application_details_aud (rev);