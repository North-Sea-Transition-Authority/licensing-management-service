ALTER TABLE licence_positions ADD COLUMN is_executed BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE licence_positions_aud ADD COLUMN is_executed BOOLEAN;