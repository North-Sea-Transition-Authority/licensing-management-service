-- Tables only the PEARS schedule migration uses. Run by hand before the migration, in each environment it runs in.

-- The PEARS event each migrated schedule event came from, for the comparison report and the write-back
CREATE TABLE lms.pears_schedule_event_migration_links (
    schedule_event_id UUID PRIMARY KEY,
    pears_plsd_id BIGINT NOT NULL,
    pears_event_mnem TEXT NOT NULL,
    pears_event_index INTEGER NOT NULL,
    pears_event_id TEXT,
    CONSTRAINT pears_schedule_event_migration_links_event_fk FOREIGN KEY (schedule_event_id)
        REFERENCES lms.schedule_events (id)
);

-- The LMS schedules the migration created, which are what the cleardown may delete
CREATE TABLE lms.pears_schedule_migrated_licences (
    licence_schedule_id UUID PRIMARY KEY,
    licence_id INTEGER NOT NULL,
    run_id UUID NOT NULL,
    migrated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pears_schedule_migrated_licences_schedule_fk FOREIGN KEY (licence_schedule_id)
        REFERENCES lms.licence_schedules (id)
);
