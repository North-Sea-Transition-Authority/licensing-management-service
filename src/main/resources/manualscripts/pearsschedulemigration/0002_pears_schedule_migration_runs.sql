-- The PEARS schedule migration's runs, and the licences each run works through. Run by hand before the migration, in
-- each environment it runs in.

CREATE TABLE lms.pears_schedule_migration_runs (
    id UUID PRIMARY KEY,
    kind TEXT NOT NULL,
    status TEXT NOT NULL,
    requested_by BIGINT NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

-- Only one run is worked at a time
CREATE UNIQUE INDEX pears_schedule_migration_runs_one_active_idx
    ON lms.pears_schedule_migration_runs ((true))
    WHERE status IN ('QUEUED', 'RUNNING');

CREATE TABLE lms.pears_schedule_migration_queue (
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

CREATE INDEX pears_schedule_migration_queue_status_idx ON lms.pears_schedule_migration_queue (run_id, status);
