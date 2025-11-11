ALTER TABLE lms.licence_work_programme_amendment_request
ALTER COLUMN work_programme_completion_date_change_requested
    TYPE BOOLEAN
    USING work_programme_completion_date_change_requested::BOOLEAN,
ALTER COLUMN work_programme_change_requested
    TYPE BOOLEAN
    USING work_programme_change_requested::BOOLEAN;

ALTER TABLE lms.licence_work_programme_amendment_request_aud
ALTER COLUMN work_programme_completion_date_change_requested
    TYPE BOOLEAN
    USING work_programme_completion_date_change_requested::BOOLEAN,
ALTER COLUMN work_programme_change_requested
    TYPE BOOLEAN
    USING work_programme_change_requested::BOOLEAN;