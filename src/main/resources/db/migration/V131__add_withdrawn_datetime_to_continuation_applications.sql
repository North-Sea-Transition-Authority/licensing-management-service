ALTER TABLE licence_continuation_applications
  ADD COLUMN withdrawn_datetime TIMESTAMPTZ;

ALTER TABLE licence_continuation_applications_aud
  ADD COLUMN withdrawn_datetime TIMESTAMPTZ;
