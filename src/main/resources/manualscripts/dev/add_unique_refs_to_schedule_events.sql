UPDATE lms.licence_schedule_terms SET event_reference = gen_random_uuid();
UPDATE lms.licence_schedule_phases SET event_reference = gen_random_uuid();
UPDATE lms.work_programme_activities SET event_reference = gen_random_uuid();
UPDATE lms.licence_schedule_rates SET event_reference = gen_random_uuid();
UPDATE lms.licence_schedule_expiry_dates SET event_reference = gen_random_uuid();
UPDATE lms.other_schedule_events SET event_reference = gen_random_uuid();