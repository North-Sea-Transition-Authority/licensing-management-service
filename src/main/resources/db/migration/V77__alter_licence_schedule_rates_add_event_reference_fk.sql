ALTER TABLE licence_schedule_rates RENAME event_reference TO event_reference_id;
ALTER TABLE licence_schedule_rates
    ADD CONSTRAINT licence_schedule_rates_event_reference_fk FOREIGN KEY (event_reference_id) REFERENCES event_references (id);

ALTER TABLE licence_schedule_rates_aud RENAME event_reference TO event_reference_id;
