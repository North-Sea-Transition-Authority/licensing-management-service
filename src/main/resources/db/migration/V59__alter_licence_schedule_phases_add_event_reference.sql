ALTER TABLE licence_schedule_phases
    ADD COLUMN event_reference UUID;

ALTER TABLE licence_schedule_phases_aud
    ADD COLUMN event_reference UUID;