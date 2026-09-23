ALTER TABLE licence_positions ADD COLUMN status TEXT;

UPDATE licence_positions
SET status = CASE WHEN is_executed THEN 'EXECUTED' ELSE 'SUBMITTED' END;

ALTER TABLE licence_positions ALTER COLUMN status SET NOT NULL;

ALTER TABLE licence_positions DROP COLUMN is_executed;

ALTER TABLE licence_positions_aud ADD COLUMN status TEXT;

UPDATE licence_positions_aud
SET status = CASE
    WHEN is_executed IS TRUE THEN 'EXECUTED'
    WHEN is_executed IS FALSE THEN 'SUBMITTED'
    ELSE NULL
    END;

ALTER TABLE licence_positions_aud DROP COLUMN is_executed;
