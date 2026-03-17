ALTER TABLE licence_schedule_terms
    ADD COLUMN event_reference UUID;

ALTER TABLE licence_schedule_terms_aud
    ADD COLUMN event_reference UUID;