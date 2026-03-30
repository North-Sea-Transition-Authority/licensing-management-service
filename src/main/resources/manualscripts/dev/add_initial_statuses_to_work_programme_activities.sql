INSERT INTO lms.work_programme_activity_statuses (id, work_programme_activity_event_reference, status, applied_datetime)
SELECT gen_random_uuid(), wpa.event_reference, 'OPEN', current_timestamp
    FROM lms.work_programme_activities wpa;