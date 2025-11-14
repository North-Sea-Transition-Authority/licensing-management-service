ALTER TABLE licence_schedule_details
    ADD COLUMN status TEXT;

ALTER TABLE licence_schedule_details
    ADD COLUMN created_instant TIMESTAMPTZ;

ALTER TABLE licence_schedule_details_aud
    ADD COLUMN status TEXT;

ALTER TABLE licence_schedule_details_aud
    ADD COLUMN created_instant TIMESTAMPTZ;