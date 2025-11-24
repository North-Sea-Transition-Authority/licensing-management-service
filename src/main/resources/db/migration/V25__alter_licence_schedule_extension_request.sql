ALTER TABLE licence_schedule_extension_request
    ADD COLUMN term_id UUID,
    ADD COLUMN phase_id UUID,
    DROP COLUMN explanation;

ALTER TABLE licence_schedule_extension_request_aud
    ADD COLUMN term_id UUID,
    ADD COLUMN phase_id UUID,
    DROP COLUMN explanation,
    ALTER COLUMN schedule_work_programme_application_details_id DROP NOT NULL,
    ALTER COLUMN extension_duration_days DROP NOT NULL,
    ALTER COLUMN extension_duration_months DROP NOT NULL,
    ALTER COLUMN extension_duration_years DROP NOT NULL;