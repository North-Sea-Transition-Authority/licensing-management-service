ALTER TABLE work_programme_activities RENAME event_reference TO event_reference_id;
ALTER TABLE work_programme_activities
    ADD CONSTRAINT work_programme_activities_event_reference_fk FOREIGN KEY (event_reference_id) REFERENCES event_references (id);

ALTER TABLE work_programme_activities_aud RENAME event_reference TO event_reference_id;
