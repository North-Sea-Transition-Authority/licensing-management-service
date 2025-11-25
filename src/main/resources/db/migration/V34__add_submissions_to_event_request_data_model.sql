ALTER TABLE schedule_work_programme_application_details
  ADD COLUMN status TEXT,
  ADD COLUMN submitted_datetime TIMESTAMPTZ,
  ADD COLUMN submitted_by_wua_id INTEGER;

ALTER TABLE schedule_work_programme_application_details_aud
  ADD COLUMN status TEXT,
  ADD COLUMN submitted_datetime TIMESTAMPTZ,
  ADD COLUMN submitted_by_wua_id INTEGER;

ALTER TABLE schedule_work_programme_applications
  ADD COLUMN application_reference TEXT;

ALTER TABLE schedule_work_programme_applications_aud
  ADD COLUMN application_reference TEXT;