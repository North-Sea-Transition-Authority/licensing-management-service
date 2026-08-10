CREATE TABLE IF NOT EXISTS gis_framework_command_journeys (
  id UUID NOT NULL
, CONSTRAINT gis_framework_command_journeys_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS gis_framework_command_journeys_aud (
  rev SERIAL
, revtype NUMERIC
, id UUID
, CONSTRAINT gis_framework_command_journeys_aud_pk PRIMARY KEY (rev, id)
, CONSTRAINT gis_framework_command_journeys_aud_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE TABLE IF NOT EXISTS gis_framework_operator_commands (
  id UUID NOT NULL
, command_journey_id UUID NOT NULL
, input_feature_ids JSONB NOT NULL
, status TEXT NOT NULL
, transformation_type TEXT NOT NULL
, command_order INTEGER NOT NULL
, CONSTRAINT gis_framework_operator_commands_pk PRIMARY KEY (id)
, CONSTRAINT gis_framework_operator_commands_journey_id_fk FOREIGN KEY (command_journey_id) REFERENCES gis_framework_command_journeys (id)
);

CREATE INDEX gis_framework_operator_commands_journey_id_idx ON gis_framework_operator_commands (command_journey_id);

CREATE TABLE IF NOT EXISTS gis_framework_operator_commands_aud (
  rev SERIAL
, revtype NUMERIC
, id UUID
, command_journey_id UUID
, input_feature_ids JSONB
, status TEXT
, transformation_type TEXT
, command_order INTEGER
, CONSTRAINT gis_framework_operator_commands_aud_pk PRIMARY KEY (rev, id)
, CONSTRAINT gis_framework_operator_commands_aud_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

ALTER TABLE gis_framework_features ADD COLUMN command_journey_id UUID;
ALTER TABLE gis_framework_features ADD COLUMN created_by_command_id UUID;
ALTER TABLE gis_framework_features ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE gis_framework_features_aud ADD COLUMN command_journey_id UUID;
ALTER TABLE gis_framework_features_aud ADD COLUMN created_by_command_id UUID;
ALTER TABLE gis_framework_features_aud ADD COLUMN active BOOLEAN;

ALTER TABLE gis_framework_features ADD CONSTRAINT gis_framework_features_command_journey_id_fk FOREIGN KEY (command_journey_id) REFERENCES gis_framework_command_journeys (id);
ALTER TABLE gis_framework_features ADD CONSTRAINT gis_framework_features_created_by_command_id_fk FOREIGN KEY (created_by_command_id) REFERENCES gis_framework_operator_commands (id);

CREATE INDEX gis_framework_features_command_journey_id_idx ON gis_framework_features (command_journey_id);
