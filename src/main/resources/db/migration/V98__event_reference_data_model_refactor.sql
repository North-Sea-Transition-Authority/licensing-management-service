ALTER TABLE event_references RENAME TO schedule_events;
ALTER TABLE event_references_aud RENAME TO schedule_events_aud;

ALTER TABLE licence_schedule_terms
    ADD CONSTRAINT fk_terms_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE licence_schedule_phases
    ADD CONSTRAINT fk_phases_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE work_programme_activities
    ADD CONSTRAINT fk_activities_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE licence_schedule_rates
    ADD CONSTRAINT fk_rates_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE other_schedule_events
    ADD CONSTRAINT fk_other_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE licence_schedule_expiry_dates
    ADD CONSTRAINT fk_expiry_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE licence_schedule_terms_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE licence_schedule_phases_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE work_programme_activities_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE licence_schedule_rates_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE other_schedule_events_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE licence_schedule_expiry_dates_aud DROP COLUMN IF EXISTS event_reference_id;
