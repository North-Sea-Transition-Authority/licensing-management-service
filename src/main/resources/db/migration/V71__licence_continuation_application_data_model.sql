ALTER TABLE licence_continuation_applications
  ADD COLUMN withdrawal_reason TEXT;

ALTER TABLE licence_continuation_applications_aud
  ADD COLUMN withdrawal_reason TEXT;