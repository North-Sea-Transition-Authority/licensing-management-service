-- External contributor teams for continuation applications were scoped on the application
-- DETAIL id, while the access check compares against the APPLICATION id, so external
-- contributors could never be granted access. Extension and amendment applications already
-- scope on the application id. Align continuation with that, which also keeps the team and its
-- membership attached across application versions.

-- Repoint the request table from the application detail to the application.
ALTER TABLE licence_continuation_external_contributor_request
  ADD COLUMN licence_continuation_application_id UUID;

UPDATE licence_continuation_external_contributor_request req
  SET licence_continuation_application_id = det.licence_continuation_application_id
  FROM licence_continuation_application_details det
  WHERE det.id = req.licence_continuation_application_detail_id;

ALTER TABLE licence_continuation_external_contributor_request
  DROP CONSTRAINT licence_continuation_external_contributor_request_fk,
  DROP COLUMN licence_continuation_application_detail_id,
  ADD CONSTRAINT licence_continuation_external_contributor_request_fk
    FOREIGN KEY (licence_continuation_application_id)
    REFERENCES licence_continuation_applications (id);

-- Dropping the old column takes its index with it.
CREATE INDEX licence_continuation_external_contributor_request_idx
  ON licence_continuation_external_contributor_request (licence_continuation_application_id);

ALTER TABLE licence_continuation_external_contributor_request_aud
  ADD COLUMN licence_continuation_application_id UUID,
  DROP COLUMN licence_continuation_application_detail_id;

-- Repoint the existing external contributor teams, so current members keep their access.
-- teams is unique on (type, scope_type, scope_id). Every continuation application has exactly
-- one detail today, so this mapping is one to one; if that ever stopped being true this would
-- fail on the unique index rather than silently merging two teams, which is the intended
-- failure mode.
UPDATE teams t
  SET scope_id = det.licence_continuation_application_id::TEXT
  FROM licence_continuation_application_details det
  WHERE t.type = 'EXTERNAL_CONTRIBUTORS'
    AND t.scope_type = 'CONTINUATION_APPLICATION'
    AND t.scope_id = det.id::TEXT;
