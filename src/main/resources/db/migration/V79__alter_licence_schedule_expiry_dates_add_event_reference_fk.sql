ALTER TABLE licence_schedule_expiry_dates RENAME event_reference TO event_reference_id;
ALTER TABLE licence_schedule_expiry_dates
    ADD CONSTRAINT licence_schedule_expiry_dates_event_reference_fk FOREIGN KEY (event_reference_id) REFERENCES event_references (id);

ALTER TABLE licence_schedule_expiry_dates_aud RENAME event_reference TO event_reference_id;
