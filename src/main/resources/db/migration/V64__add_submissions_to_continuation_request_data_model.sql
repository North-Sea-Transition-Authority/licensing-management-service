ALTER TABLE licence_continuation_application_details
  ADD COLUMN submitted_datetime TIMESTAMPTZ,
  ADD COLUMN submitted_by_wua_id INTEGER;

ALTER TABLE licence_continuation_application_details_aud
  ADD COLUMN submitted_datetime TIMESTAMPTZ,
  ADD COLUMN submitted_by_wua_id INTEGER;

ALTER TABLE licence_continuation_applications
  ADD COLUMN application_reference TEXT;

ALTER TABLE licence_continuation_applications_aud
  ADD COLUMN application_reference TEXT;