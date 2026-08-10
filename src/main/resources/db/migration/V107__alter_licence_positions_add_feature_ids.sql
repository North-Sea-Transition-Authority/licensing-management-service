ALTER TABLE licence_positions ADD COLUMN feature_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE licence_positions_aud ADD COLUMN feature_ids JSONB;
