-- Lets the PEARS schedule migration's runs work the work programme migration too. Run by hand before the work
-- programme migration, in each environment it runs in, after the schedule migration's scripts.

ALTER TABLE lms.pears_schedule_migration_runs ADD COLUMN migration TEXT NOT NULL DEFAULT 'SCHEDULE';
