-- The tables from manualscripts/pearsworkprogrammemigration, for the integration tests

-- The schedules the work programme migration has written to, which is what makes it safe to run again
CREATE TABLE IF NOT EXISTS lms.pears_work_programme_migrated_licences (
    licence_schedule_id UUID PRIMARY KEY,
    licence_id INTEGER NOT NULL,
    run_id UUID NOT NULL,
    migrated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pears_work_programme_migrated_licences_schedule_fk FOREIGN KEY (licence_schedule_id)
        REFERENCES lms.licence_schedules (id)
);

-- The PEARS activity each migrated work programme activity came from, for the comparison report and the cleardown
CREATE TABLE IF NOT EXISTS lms.pears_work_programme_activity_migration_links (
    schedule_event_id UUID PRIMARY KEY,
    pears_wp_id BIGINT NOT NULL,
    pears_wpd_id BIGINT NOT NULL,
    pears_activity_id TEXT NOT NULL,
    pears_type TEXT NOT NULL,
    CONSTRAINT pears_work_programme_activity_migration_links_event_fk FOREIGN KEY (schedule_event_id)
        REFERENCES lms.schedule_events (id)
);

-- The schedule comments the migration wrote, so the cleardown removes those and leaves any added in LMS. As 0001 and
-- then 0003, which lets a migrated comment be removed in LMS
CREATE TABLE IF NOT EXISTS lms.pears_work_programme_schedule_comment_links (
    schedule_comment_id UUID PRIMARY KEY,
    CONSTRAINT pears_work_programme_schedule_comment_links_comment_fk FOREIGN KEY (schedule_comment_id)
        REFERENCES lms.schedule_comments (id) ON DELETE CASCADE
);
