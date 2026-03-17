ALTER TABLE work_programme_activities
    ADD COLUMN event_reference UUID;

ALTER TABLE work_programme_activities_aud
    ADD COLUMN event_reference UUID;