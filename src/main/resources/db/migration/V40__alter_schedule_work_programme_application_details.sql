ALTER TABLE schedule_work_programme_application_details
  ADD COLUMN created_datetime TIMESTAMPTZ NOT NULL;

ALTER TABLE schedule_work_programme_application_details_aud
  ADD COLUMN created_datetime TIMESTAMPTZ NOT NULL;