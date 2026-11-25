-- Case events, case notes and application decision details can now reference the central
-- applications table directly. An application id is unique across application types, so the
-- application_type column on each is redundant.

DROP INDEX case_events_application_idx;
DROP INDEX case_notes_application_idx;

ALTER TABLE case_events
  DROP COLUMN application_type,
  ADD CONSTRAINT case_events_application_fk FOREIGN KEY (application_id) REFERENCES applications (id);

ALTER TABLE case_notes
  DROP COLUMN application_type,
  ADD CONSTRAINT case_notes_application_fk FOREIGN KEY (application_id) REFERENCES applications (id);

CREATE INDEX case_events_application_idx ON case_events (application_id, event_instant);
CREATE INDEX case_notes_application_idx ON case_notes (application_id);

ALTER TABLE case_events_aud DROP COLUMN application_type;
ALTER TABLE case_notes_aud DROP COLUMN application_type;

ALTER TABLE application_decision_details
  DROP CONSTRAINT application_decision_details_version_uk,
  DROP COLUMN application_type,
  ADD CONSTRAINT application_decision_details_application_fk FOREIGN KEY (application_id) REFERENCES applications (id),
  ADD CONSTRAINT application_decision_details_version_uk UNIQUE (application_id, version_number);

ALTER TABLE application_decision_details_aud DROP COLUMN application_type;
