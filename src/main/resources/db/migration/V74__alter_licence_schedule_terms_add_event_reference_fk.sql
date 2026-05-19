ALTER TABLE licence_schedule_terms RENAME event_reference TO event_reference_id;
ALTER TABLE licence_schedule_terms
    ADD CONSTRAINT licence_schedule_terms_event_reference_fk FOREIGN KEY (event_reference_id) REFERENCES event_references (id);

ALTER TABLE licence_schedule_terms_aud RENAME event_reference TO event_reference_id;
