CREATE TABLE swp_application_request_purpose (
  id UUID PRIMARY KEY,
  schedule_work_programme_application_detail_id UUID,
  extend_phase_or_term BOOLEAN NOT NULL,
  extend_term BOOLEAN NOT NULL,
  amend_work_programme BOOLEAN NOT NULL,
  CONSTRAINT swp_application_request_purpose_fk FOREIGN KEY (schedule_work_programme_application_detail_id) REFERENCES schedule_work_programme_application_details(id)
);

CREATE INDEX swp_application_request_purpose_idx ON swp_application_request_purpose(schedule_work_programme_application_detail_id);

CREATE TABLE swp_application_request_purpose_aud (
  rev SERIAL,
  revtype NUMERIC,
  id UUID,
  schedule_work_programme_application_detail_id UUID,
  extend_phase_or_term BOOLEAN,
  extend_term BOOLEAN,
  amend_work_programme BOOLEAN,
  CONSTRAINT swp_application_request_purpose_aud_pk PRIMARY KEY (rev, id),
  CONSTRAINT swp_application_request_purpose_aud_rev_fk FOREIGN KEY (rev) REFERENCES audit_revisions(rev)
);

CREATE INDEX swp_application_request_purpose_aud_rev_idx ON swp_application_request_purpose_aud(rev);