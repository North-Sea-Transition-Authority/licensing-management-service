CREATE TABLE IF NOT EXISTS gis_framework_feature_journey_states (
  id UUID NOT NULL
, feature_id UUID NOT NULL
, command_journey_id UUID NOT NULL
, created_by_command_id UUID
, active BOOLEAN NOT NULL
, CONSTRAINT gis_framework_feature_journey_states_pk PRIMARY KEY (id)
, CONSTRAINT gis_framework_feature_journey_states_feature_id_uk UNIQUE (feature_id)
, CONSTRAINT gis_framework_feature_journey_states_feature_id_fk FOREIGN KEY (feature_id) REFERENCES gis_framework_features (id)
, CONSTRAINT gis_framework_feature_journey_states_command_journey_id_fk FOREIGN KEY (command_journey_id) REFERENCES gis_framework_command_journeys (id)
, CONSTRAINT gis_framework_feature_journey_states_created_by_command_id_fk FOREIGN KEY (created_by_command_id) REFERENCES gis_framework_operator_commands (id)
);

CREATE INDEX gis_framework_feature_journey_states_command_journey_id_idx ON gis_framework_feature_journey_states (command_journey_id);
CREATE INDEX gis_framework_feature_journey_states_created_by_command_id_idx ON gis_framework_feature_journey_states (created_by_command_id);

CREATE TABLE IF NOT EXISTS gis_framework_feature_journey_states_aud (
  rev SERIAL
, revtype NUMERIC
, id UUID
, feature_id UUID
, command_journey_id UUID
, created_by_command_id UUID
, active BOOLEAN
, CONSTRAINT gis_framework_feature_journey_states_aud_pk PRIMARY KEY (rev, id)
, CONSTRAINT gis_framework_feature_journey_states_aud_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

ALTER TABLE gis_framework_features DROP CONSTRAINT gis_framework_features_command_journey_id_fk;
ALTER TABLE gis_framework_features DROP CONSTRAINT gis_framework_features_created_by_command_id_fk;
DROP INDEX gis_framework_features_command_journey_id_idx;

ALTER TABLE gis_framework_features DROP COLUMN command_journey_id;
ALTER TABLE gis_framework_features DROP COLUMN created_by_command_id;
ALTER TABLE gis_framework_features DROP COLUMN active;

ALTER TABLE gis_framework_features_aud DROP COLUMN command_journey_id;
ALTER TABLE gis_framework_features_aud DROP COLUMN created_by_command_id;
ALTER TABLE gis_framework_features_aud DROP COLUMN active;
