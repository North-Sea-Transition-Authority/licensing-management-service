CREATE TABLE IF NOT EXISTS gis_framework_features (
  id UUID NOT NULL
, feature_name TEXT NOT NULL
, feature_type TEXT NOT NULL
, coordinate_system TEXT NOT NULL
, feature_area NUMERIC NOT NULL
, parent_feature_id UUID
, CONSTRAINT gis_framework_features_pk PRIMARY KEY (id)
, CONSTRAINT gis_framework_features_parent_feature_id_fk FOREIGN KEY (parent_feature_id) REFERENCES gis_framework_features (id)
);

CREATE INDEX gis_framework_features_parent_feature_id_idx ON gis_framework_features (parent_feature_id);

CREATE TABLE IF NOT EXISTS gis_framework_features_aud (
  rev SERIAL
, revtype NUMERIC
, id UUID
, feature_name TEXT
, feature_type TEXT
, coordinate_system TEXT
, feature_area NUMERIC
, parent_feature_id UUID
, CONSTRAINT gis_framework_features_aud_pk PRIMARY KEY (rev, id)
, CONSTRAINT gis_framework_features_aud_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX gis_framework_features_aud_rev_idx ON gis_framework_features_aud (rev);

CREATE TABLE IF NOT EXISTS gis_framework_polygons (
  id UUID NOT NULL
, feature_id UUID NOT NULL
, attributes JSONB NOT NULL
, start_depth NUMERIC
, end_depth NUMERIC
, CONSTRAINT gis_framework_polygons_pk PRIMARY KEY (id)
, CONSTRAINT gis_framework_polygons_feature_id_fk FOREIGN KEY (feature_id) REFERENCES gis_framework_features (id)
);

CREATE INDEX gis_framework_polygons_feature_id_idx ON gis_framework_polygons (feature_id);

CREATE TABLE IF NOT EXISTS gis_framework_polygons_aud (
  rev SERIAL
, revtype NUMERIC
, id UUID
, feature_id UUID
, attributes JSONB
, start_depth NUMERIC
, end_depth NUMERIC
, CONSTRAINT gis_framework_polygons_aud_pk PRIMARY KEY (rev, id)
, CONSTRAINT gis_framework_polygons_aud_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX gis_framework_polygons_aud_rev_idx ON gis_framework_polygons_aud (rev);

CREATE TABLE IF NOT EXISTS gis_framework_lines (
  id UUID NOT NULL
, polygon_id UUID NOT NULL
, navigation_type TEXT NOT NULL
, ring_number NUMERIC NOT NULL
, ring_connection_order NUMERIC NOT NULL
, esri_json TEXT NOT NULL
, attributes JSONB NOT NULL
, CONSTRAINT gis_framework_lines_pk PRIMARY KEY (id)
, CONSTRAINT gis_framework_lines_polygon_id_fk FOREIGN KEY (polygon_id) REFERENCES gis_framework_polygons (id)
);

CREATE INDEX gis_framework_lines_polygon_id_idx ON gis_framework_lines (polygon_id);

CREATE TABLE IF NOT EXISTS gis_framework_lines_aud (
  rev SERIAL
, revtype NUMERIC
, id UUID
, polygon_id UUID
, navigation_type TEXT
, ring_number NUMERIC
, ring_connection_order NUMERIC
, esri_json TEXT
, attributes JSONB
, CONSTRAINT gis_framework_lines_aud_pk PRIMARY KEY (rev, id)
, CONSTRAINT gis_framework_lines_aud_fk FOREIGN KEY (rev) REFERENCES audit_revisions (rev)
);

CREATE INDEX gis_framework_lines_aud_rev_idx ON gis_framework_lines_aud (rev);