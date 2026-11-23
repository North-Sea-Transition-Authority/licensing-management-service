ALTER TABLE licence_schedule_details ADD COLUMN applied_instant TIMESTAMPTZ;
ALTER TABLE licence_schedule_details ADD COLUMN created_by_wua_id BIGINT;
ALTER TABLE licence_schedule_details ADD COLUMN applied_by_wua_id BIGINT;

ALTER TABLE licence_schedule_details_aud ADD COLUMN applied_instant TIMESTAMPTZ;
ALTER TABLE licence_schedule_details_aud ADD COLUMN created_by_wua_id BIGINT;
ALTER TABLE licence_schedule_details_aud ADD COLUMN applied_by_wua_id BIGINT;
