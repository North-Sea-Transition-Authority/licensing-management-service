ALTER TABLE other_schedule_events
    ADD COLUMN event_reference UUID;

ALTER TABLE other_schedule_events_aud
    ADD COLUMN event_reference UUID;