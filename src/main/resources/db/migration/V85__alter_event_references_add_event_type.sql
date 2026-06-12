ALTER TABLE event_references ADD COLUMN event_type TEXT NOT NULL;
ALTER TABLE event_references ALTER COLUMN licence_schedule_id SET NOT NULL;

ALTER TABLE event_references_aud ADD COLUMN event_type TEXT;
