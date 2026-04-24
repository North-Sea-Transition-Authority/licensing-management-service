ALTER TABLE gis_framework_features ADD COLUMN legacy_id INTEGER NOT NULL;
ALTER TABLE gis_framework_features ADD COLUMN test_case TEXT NOT NULL;
ALTER TABLE gis_framework_polygons ADD COLUMN legacy_id INTEGER NOT NULL;
ALTER TABLE gis_framework_lines ADD COLUMN legacy_id INTEGER NOT NULL;

ALTER TABLE gis_framework_features_aud ADD COLUMN legacy_id INTEGER;
ALTER TABLE gis_framework_features_aud ADD COLUMN test_case TEXT;
ALTER TABLE gis_framework_polygons_aud ADD COLUMN legacy_id INTEGER;
ALTER TABLE gis_framework_lines_aud ADD COLUMN legacy_id INTEGER;

ALTER TABLE gis_framework_features DROP COLUMN feature_type;
ALTER TABLE gis_framework_features_aud DROP COLUMN feature_type;

ALTER TABLE gis_framework_features ADD COLUMN attributes JSONB NOT NULL;
ALTER TABLE gis_framework_features_aud ADD COLUMN attributes JSONB;
