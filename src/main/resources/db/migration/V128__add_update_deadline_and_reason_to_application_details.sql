ALTER TABLE schedule_work_programme_application_details ADD COLUMN update_deadline_date DATE;
ALTER TABLE schedule_work_programme_application_details ADD COLUMN update_reason TEXT;
ALTER TABLE schedule_work_programme_application_details_aud ADD COLUMN update_deadline_date DATE;
ALTER TABLE schedule_work_programme_application_details_aud ADD COLUMN update_reason TEXT;

ALTER TABLE licence_continuation_application_details ADD COLUMN update_deadline_date DATE;
ALTER TABLE licence_continuation_application_details ADD COLUMN update_reason TEXT;
ALTER TABLE licence_continuation_application_details_aud ADD COLUMN update_deadline_date DATE;
ALTER TABLE licence_continuation_application_details_aud ADD COLUMN update_reason TEXT;

ALTER TABLE schedule_work_programme_application_details
    ADD CONSTRAINT schedule_work_programme_application_details_version_uk
        UNIQUE (schedule_work_programme_application_id, version_number);

ALTER TABLE licence_continuation_application_details
    ADD CONSTRAINT licence_continuation_application_details_version_uk
        UNIQUE (licence_continuation_application_id, version_number);
