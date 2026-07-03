ALTER TABLE schedule_events ADD COLUMN original_event_id UUID;
ALTER TABLE schedule_events ALTER COLUMN original_event_id SET NOT NULL;

ALTER TABLE schedule_events_aud ADD COLUMN original_event_id UUID;
