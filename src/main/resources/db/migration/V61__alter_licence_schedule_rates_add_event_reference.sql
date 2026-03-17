ALTER TABLE licence_schedule_rates
    ADD COLUMN event_reference UUID;

ALTER TABLE licence_schedule_rates_aud
    ADD COLUMN event_reference UUID;