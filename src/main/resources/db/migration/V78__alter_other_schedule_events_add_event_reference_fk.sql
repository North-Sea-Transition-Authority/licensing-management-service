ALTER TABLE other_schedule_events RENAME event_reference TO event_reference_id;
ALTER TABLE other_schedule_events
    ADD CONSTRAINT other_schedule_events_event_reference_fk FOREIGN KEY (event_reference_id) REFERENCES event_references (id);

ALTER TABLE other_schedule_events_aud RENAME event_reference TO event_reference_id;
