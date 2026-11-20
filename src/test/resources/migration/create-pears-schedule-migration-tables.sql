-- The tables from manualscripts/pearsschedulemigration, for the integration tests

-- The PEARS event each migrated schedule event came from, for the comparison report and the write-back
CREATE TABLE IF NOT EXISTS lms.pears_schedule_event_migration_links (
    schedule_event_id UUID PRIMARY KEY,
    pears_plsd_id BIGINT NOT NULL,
    pears_event_mnem TEXT NOT NULL,
    pears_event_index INTEGER NOT NULL,
    pears_event_id TEXT,
    CONSTRAINT pears_schedule_event_migration_links_event_fk FOREIGN KEY (schedule_event_id)
        REFERENCES lms.schedule_events (id)
);

-- The LMS schedules the migration created, which are what the cleardown may delete
CREATE TABLE IF NOT EXISTS lms.pears_schedule_migrated_licences (
    licence_schedule_id UUID PRIMARY KEY,
    licence_id INTEGER NOT NULL,
    run_id UUID NOT NULL,
    migrated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pears_schedule_migrated_licences_schedule_fk FOREIGN KEY (licence_schedule_id)
        REFERENCES lms.licence_schedules (id)
);

-- The migration's runs, and the licences each run works through
CREATE TABLE IF NOT EXISTS lms.pears_schedule_migration_runs (
    id UUID PRIMARY KEY,
    kind TEXT NOT NULL,
    status TEXT NOT NULL,
    requested_by BIGINT NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS pears_schedule_migration_runs_one_active_idx
    ON lms.pears_schedule_migration_runs ((true))
    WHERE status IN ('QUEUED', 'RUNNING');

CREATE TABLE IF NOT EXISTS lms.pears_schedule_migration_queue (
    run_id UUID NOT NULL,
    licence_reference TEXT NOT NULL,
    status TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    outcome TEXT,
    notes JSONB,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    CONSTRAINT pears_schedule_migration_queue_pk PRIMARY KEY (run_id, licence_reference),
    CONSTRAINT pears_schedule_migration_queue_run_fk FOREIGN KEY (run_id)
        REFERENCES lms.pears_schedule_migration_runs (id)
);

CREATE INDEX IF NOT EXISTS pears_schedule_migration_queue_status_idx
    ON lms.pears_schedule_migration_queue (run_id, status);
