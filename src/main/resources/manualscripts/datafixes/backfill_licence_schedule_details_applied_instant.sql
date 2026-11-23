-- Backfill licence_schedule_details.applied_instant for schedule versions that were applied
-- before the column was added (V137). Run once against each environment after V137 has run.
-- Safe to re-run: only rows with no applied_instant are touched.

-- Step 1: a version applied in LMS moved DRAFT -> ACTIVE in an update revision, so take the
-- earliest such revision. Versions written by the PEARS and carbon storage migrations were
-- inserted already ACTIVE or REPLACED, carry their historical date in created_instant, and have
-- no update revision to ACTIVE, so they fall back to created_instant.
UPDATE lms.licence_schedule_details lsd
SET applied_instant = COALESCE(
    (
      SELECT MIN(ar.created_date_time)
      FROM lms.licence_schedule_details_aud lsda
      JOIN lms.audit_revisions ar ON ar.rev = lsda.rev
      WHERE lsda.id = lsd.id
      AND lsda.status = 'ACTIVE'
      AND lsda.revtype = 1
    ),
    lsd.created_instant
  )
WHERE lsd.status IN ('ACTIVE', 'REPLACED')
AND lsd.applied_instant IS NULL;

-- Step 2: copy the value onto the existing audit rows for those versions
UPDATE lms.licence_schedule_details_aud lsda
SET applied_instant = lsd.applied_instant
FROM lms.licence_schedule_details lsd
WHERE lsd.id = lsda.id
AND lsda.status IN ('ACTIVE', 'REPLACED')
AND lsda.applied_instant IS NULL;
