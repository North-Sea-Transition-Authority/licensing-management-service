-- Backfill document instances for EAA applications already in ISSUE_DECISION status.
-- Run this against any environment where applications were transitioned to ISSUE_DECISION
-- before RecordFinalDecisionService started creating document instances automatically.
-- Safe to re-run: both statements are guarded by NOT EXISTS.

-- Step 1: create document instances for EAA applications in ISSUE_DECISION with no existing instance
INSERT INTO lms.document_library_document_instances
  (id, item_reference, item_type, title, description, document_template_id)
SELECT
  gen_random_uuid(),
  swpa.id::text,
  'SCHEDULE_AMENDMENT_APPLICATION',
  t.title,
  t.description,
  t.id
FROM lms.schedule_work_programme_applications swpa
JOIN lms.schedule_work_programme_application_details swpad
  ON swpad.schedule_work_programme_application_id = swpa.id
JOIN lms.document_library_document_templates t
  ON t.mnemonic = 'EXTENSION_APPROVAL_LETTER-SCHEDULE_AMENDMENT_APPLICATION'
WHERE swpad.status = 'ISSUE_DECISION'
  AND NOT EXISTS (
    SELECT 1
    FROM lms.document_library_document_instances di
    WHERE di.item_reference = swpa.id::text
      AND di.item_type = 'SCHEDULE_AMENDMENT_APPLICATION'
  );

-- Step 2: create sections for any instances that have none yet
INSERT INTO lms.document_library_document_instance_sections
  (id, document_instance_id, created_from_document_template_section_id,
   parent_id, title, content, numbered, has_page_break_before, display_order)
SELECT
  gen_random_uuid(),
  di.id,
  ts.id,
  NULL,
  ts.title,
  ts.content,
  ts.numbered,
  ts.has_page_break_before,
  ts.display_order
FROM lms.document_library_document_instances di
JOIN lms.document_library_document_templates t ON t.id = di.document_template_id
JOIN lms.document_library_document_template_sections ts ON ts.document_template_id = t.id
WHERE di.item_type = 'SCHEDULE_AMENDMENT_APPLICATION'
  AND t.mnemonic = 'EXTENSION_APPROVAL_LETTER-SCHEDULE_AMENDMENT_APPLICATION'
  AND ts.parent_id IS NULL
  AND NOT EXISTS (
    SELECT 1
    FROM lms.document_library_document_instance_sections dis
    WHERE dis.document_instance_id = di.id
  );
