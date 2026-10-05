ALTER TABLE licence_corrections ADD COLUMN completed_instant TIMESTAMPTZ;
ALTER TABLE licence_corrections_aud ADD COLUMN completed_instant TIMESTAMPTZ;

UPDATE licence_corrections lc
SET completed_instant = COALESCE(
    (
      SELECT MIN(ar.created_date_time)
      FROM licence_corrections_aud lca
      JOIN audit_revisions ar ON ar.rev = lca.rev
      WHERE lca.id = lc.id
      AND lca.status = 'COMPLETE'
    ),
    lc.created_instant
  )
WHERE lc.status = 'COMPLETE';

UPDATE licence_corrections_aud lca
SET completed_instant = lc.completed_instant
FROM licence_corrections lc
WHERE lc.id = lca.id
AND lca.status = 'COMPLETE';

ALTER TABLE licence_corrections
  ADD CONSTRAINT licence_corrections_completed_instant_chk
  CHECK (status != 'COMPLETE' OR completed_instant IS NOT NULL);
