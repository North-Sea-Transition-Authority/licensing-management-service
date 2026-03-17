ALTER TABLE licence_schedule_expiry_dates
    ADD COLUMN event_reference UUID;

ALTER TABLE licence_schedule_expiry_dates_aud
    ADD COLUMN event_reference UUID;