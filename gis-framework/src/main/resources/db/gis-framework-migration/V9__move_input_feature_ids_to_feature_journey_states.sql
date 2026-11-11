ALTER TABLE gis_framework_feature_journey_states ADD COLUMN input_feature_ids JSONB;
ALTER TABLE gis_framework_feature_journey_states_aud ADD COLUMN input_feature_ids JSONB;

UPDATE gis_framework_feature_journey_states s
SET input_feature_ids = c.input_feature_ids
FROM gis_framework_operator_commands c
WHERE s.created_by_command_id = c.id;

ALTER TABLE gis_framework_operator_commands DROP COLUMN input_feature_ids;
ALTER TABLE gis_framework_operator_commands_aud DROP COLUMN input_feature_ids;
