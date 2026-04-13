-- Drop document template, template sections, instances, and instance sections for CONTINUATION_LETTER-CONTINUATION_APPLICATION.
-- This script also removes associated audit records.

-- 1. Instance Sections
DELETE FROM lms.document_library_document_instance_sections_aud
WHERE document_instance_id IN (
  SELECT di.id
  FROM lms.document_library_document_instances di
  JOIN lms.document_library_document_templates t ON t.id = di.document_template_id
  WHERE t.mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION'
);

DELETE FROM lms.document_library_document_instance_sections
WHERE document_instance_id IN (
  SELECT di.id
  FROM lms.document_library_document_instances di
  JOIN lms.document_library_document_templates t ON t.id = di.document_template_id
  WHERE t.mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION'
);

-- 2. Instances
DELETE FROM lms.document_library_document_instances_aud
WHERE document_template_id IN (
  SELECT t.id
  FROM lms.document_library_document_templates t
  WHERE t.mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION'
);

DELETE FROM lms.document_library_document_instances
WHERE document_template_id IN (
  SELECT t.id
  FROM lms.document_library_document_templates t
  WHERE t.mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION'
);

-- 3. Template Sections
DELETE FROM lms.document_library_document_template_sections_aud
WHERE document_template_id IN (
  SELECT t.id
  FROM lms.document_library_document_templates t
  WHERE t.mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION'
);

DELETE FROM lms.document_library_document_template_sections
WHERE document_template_id IN (
  SELECT t.id
  FROM lms.document_library_document_templates t
  WHERE t.mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION'
);

-- 4. Metadata
DELETE FROM lms.document_templates_metadata_aud
WHERE document_template_id IN (
  SELECT t.id
  FROM lms.document_library_document_templates t
  WHERE t.mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION'
);

DELETE FROM lms.document_templates_metadata
WHERE document_template_id IN (
  SELECT t.id
  FROM lms.document_library_document_templates t
  WHERE t.mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION'
);

-- 5. Templates
DELETE FROM lms.document_library_document_templates_aud
WHERE mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION';

DELETE FROM lms.document_library_document_templates
WHERE mnemonic = 'CONTINUATION_LETTER-CONTINUATION_APPLICATION';
