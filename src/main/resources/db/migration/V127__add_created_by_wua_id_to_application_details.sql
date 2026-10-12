ALTER TABLE schedule_work_programme_application_details ADD COLUMN created_by_wua_id BIGINT;
ALTER TABLE schedule_work_programme_application_details_aud ADD COLUMN created_by_wua_id BIGINT;

ALTER TABLE licence_continuation_application_details ADD COLUMN created_by_wua_id BIGINT;
ALTER TABLE licence_continuation_application_details_aud ADD COLUMN created_by_wua_id BIGINT;

UPDATE schedule_work_programme_application_details t
SET created_by_wua_id = r.user_wua_id
FROM schedule_work_programme_application_details_aud a
JOIN audit_revisions r ON r.rev = a.rev
WHERE a.id = t.id
AND a.revtype = 0;

UPDATE licence_continuation_application_details t
SET created_by_wua_id = r.user_wua_id
FROM licence_continuation_application_details_aud a
JOIN audit_revisions r ON r.rev = a.rev
WHERE a.id = t.id
AND a.revtype = 0;
