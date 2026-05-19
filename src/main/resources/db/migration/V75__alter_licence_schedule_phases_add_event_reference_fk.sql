ALTER TABLE licence_schedule_phases RENAME event_reference TO event_reference_id;
ALTER TABLE licence_schedule_phases
    ADD CONSTRAINT licence_schedule_phases_event_reference_fk FOREIGN KEY (event_reference_id) REFERENCES event_references (id);

ALTER TABLE licence_schedule_phases_aud RENAME event_reference TO event_reference_id;
