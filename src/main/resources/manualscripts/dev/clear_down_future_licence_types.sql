-- Delete every licence of a type held back behind the FUTURE release phase (gas storage, landward exploration,
-- methane drainage, seaward exploration), along with its schedules, applications, corrections, positions,
-- reminders and the rows in shared tables that point at them without a foreign key.
-- Run in a single transaction, e.g. psql --single-transaction. Safe to re-run.
-- Audit (_aud) rows are left in place, and uploaded file content stays in the file store - only the file rows go.

CREATE TEMP TABLE future_licences AS
SELECT id FROM lms.licences
WHERE type IN ('GAS_STORAGE', 'LANDWARD_EXPLORATION', 'METHANE_DRAINAGE', 'SEAWARD_EXPLORATION');

CREATE TEMP TABLE future_schedules AS
SELECT id FROM lms.licence_schedules WHERE licence_id IN (SELECT id FROM future_licences);

CREATE TEMP TABLE future_schedule_details AS
SELECT id FROM lms.licence_schedule_details WHERE licence_schedule_id IN (SELECT id FROM future_schedules);

CREATE TEMP TABLE future_schedule_events AS
SELECT id FROM lms.schedule_events WHERE licence_schedule_id IN (SELECT id FROM future_schedules);

CREATE TEMP TABLE future_swp_applications AS
SELECT id FROM lms.schedule_work_programme_applications
WHERE licence_schedule_id IN (SELECT id FROM future_schedules)
   OR submitted_licence_schedule_detail_id IN (SELECT id FROM future_schedule_details);

CREATE TEMP TABLE future_swp_application_details AS
SELECT id FROM lms.schedule_work_programme_application_details
WHERE schedule_work_programme_application_id IN (SELECT id FROM future_swp_applications);

CREATE TEMP TABLE future_continuation_applications AS
SELECT id FROM lms.licence_continuation_applications
WHERE licence_schedule_id IN (SELECT id FROM future_schedules)
   OR submitted_licence_schedule_detail_id IN (SELECT id FROM future_schedule_details);

CREATE TEMP TABLE future_continuation_application_details AS
SELECT id FROM lms.licence_continuation_application_details
WHERE licence_continuation_application_id IN (SELECT id FROM future_continuation_applications);

CREATE TEMP TABLE future_corrections AS
SELECT id FROM lms.licence_corrections WHERE licence_id IN (SELECT id FROM future_licences);

CREATE TEMP TABLE future_positions AS
SELECT id, licence_transaction_id FROM lms.licence_positions WHERE licence_id IN (SELECT id FROM future_licences);

-- Rows in shared tables that reference the above by id, with no foreign key

DELETE FROM lms.work_area_item_views
WHERE (item_type = 'SCHEDULE_WORK_PROGRAMME_APPLICATION'
       AND item_id IN (SELECT id FROM future_swp_application_details))
   OR (item_type = 'LICENCE_CONTINUATION_APPLICATION'
       AND (item_id IN (SELECT id FROM future_continuation_applications)
            OR item_id IN (SELECT id FROM future_continuation_application_details)))
   OR (item_type = 'DRAFT_LICENCE_SCHEDULE' AND item_id IN (SELECT id FROM future_schedule_details))
   OR (item_type = 'LICENCE_CORRECTION' AND item_id IN (SELECT id FROM future_corrections));

DELETE FROM lms.case_notes
WHERE (application_type = 'SCHEDULE_AMENDMENT_APPLICATION' AND application_id IN (SELECT id FROM future_swp_applications))
   OR (application_type = 'CONTINUATION_APPLICATION' AND application_id IN (SELECT id FROM future_continuation_applications));

DELETE FROM lms.case_events
WHERE (application_type = 'SCHEDULE_AMENDMENT_APPLICATION' AND application_id IN (SELECT id FROM future_swp_applications))
   OR (application_type = 'CONTINUATION_APPLICATION' AND application_id IN (SELECT id FROM future_continuation_applications));

CREATE TEMP TABLE future_contributor_teams AS
SELECT id FROM lms.teams
WHERE type = 'EXTERNAL_CONTRIBUTORS'
  AND ((scope_type = 'SCHEDULE_AMENDMENT_APPLICATION'
        AND scope_id IN (SELECT id::text FROM future_swp_applications))
    OR (scope_type = 'CONTINUATION_APPLICATION'
        AND scope_id IN (SELECT id::text FROM future_continuation_applications)));

DELETE FROM lms.team_roles WHERE team_id IN (SELECT id FROM future_contributor_teams);
DELETE FROM lms.teams WHERE id IN (SELECT id FROM future_contributor_teams);

CREATE TEMP TABLE future_document_instances AS
SELECT id FROM lms.document_library_document_instances
WHERE (item_type = 'SCHEDULE_AMENDMENT_APPLICATION'
       AND item_reference IN (SELECT id::text FROM future_swp_applications))
   OR (item_type = 'CONTINUATION_APPLICATION'
       AND item_reference IN (SELECT id::text FROM future_continuation_applications));

DELETE FROM lms.document_library_document_instance_sections
WHERE document_instance_id IN (SELECT id FROM future_document_instances);
DELETE FROM lms.document_library_document_instances WHERE id IN (SELECT id FROM future_document_instances);

DELETE FROM lms.file_upload_library_uploaded_files
WHERE (usage_type IN ('SCHEDULE-AMENDMENT-APP-SUPPORTING-DOCUMENT', 'FINAL-DECISION-SUPPORT-PAPER')
       AND usage_id IN (SELECT id::text FROM future_swp_application_details))
   OR (usage_type IN ('CONTINUATION-OTHER-REQUIREMENT-DOCUMENT', 'CONTINUATION-ADDITIONAL-SUPPORTING-DOCUMENT')
       AND usage_id IN (SELECT id::text FROM future_continuation_application_details))
   OR (usage_type = 'APPLICATION-CONTINUATION-LETTER'
       AND usage_id IN (SELECT id::text FROM future_continuation_applications));

DELETE FROM lms.licence_event_cache WHERE licence_id IN (SELECT id FROM future_licences);

-- Extension and amendment applications

DELETE FROM lms.swp_record_of_decision_work_programme_licence
WHERE licence_id IN (SELECT id FROM future_licences)
   OR record_of_decision_work_programme_id IN (
     SELECT id FROM lms.swp_record_of_decision_work_programme
     WHERE schedule_work_programme_application_detail_id IN (SELECT id FROM future_swp_application_details)
   );

DELETE FROM lms.swp_record_of_decision_work_programme
WHERE schedule_work_programme_application_detail_id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.swp_record_of_decision_extension
WHERE schedule_work_programme_application_detail_id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.swp_record_of_decision_reduction
WHERE schedule_work_programme_application_detail_id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.swp_record_of_decision
WHERE schedule_work_programme_application_detail_id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.swp_application_request_purpose
WHERE schedule_work_programme_application_detail_id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.licence_work_programme_amendment_summary
WHERE schedule_work_programme_application_details_id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.licence_work_programme_amendment_request
WHERE schedule_work_programme_application_details_id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.licence_schedule_supporting_information
WHERE schedule_work_programme_application_details_id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.licence_schedule_extension_request
WHERE schedule_work_programme_application_details_id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.schedule_work_programme_application_details WHERE id IN (SELECT id FROM future_swp_application_details);
DELETE FROM lms.swp_external_contributor_request
WHERE schedule_work_programme_application_id IN (SELECT id FROM future_swp_applications);
DELETE FROM lms.schedule_work_programme_applications WHERE id IN (SELECT id FROM future_swp_applications);

-- Continuation applications

DELETE FROM lms.licence_continuation_wpa_requirement_request
WHERE licence_continuation_application_detail_id IN (SELECT id FROM future_continuation_application_details);
DELETE FROM lms.licence_continuation_other_requirement_request
WHERE licence_continuation_application_detail_id IN (SELECT id FROM future_continuation_application_details);
DELETE FROM lms.licence_continuation_licence_operators_request
WHERE licence_continuation_application_detail_id IN (SELECT id FROM future_continuation_application_details);
DELETE FROM lms.licence_continuation_supporting_information
WHERE licence_continuation_application_detail_id IN (SELECT id FROM future_continuation_application_details);
DELETE FROM lms.licence_continuation_application_details
WHERE id IN (SELECT id FROM future_continuation_application_details);
DELETE FROM lms.licence_continuation_external_contributor_request
WHERE licence_continuation_application_id IN (SELECT id FROM future_continuation_applications);
DELETE FROM lms.licence_continuation_applications WHERE id IN (SELECT id FROM future_continuation_applications);

-- Reminders

DELETE FROM lms.licence_reminders
WHERE licence_id IN (SELECT id FROM future_licences)
   OR schedule_event_id IN (SELECT id FROM future_schedule_events);
DELETE FROM lms.licence_reminder_missing_contacts WHERE licence_id IN (SELECT id FROM future_licences);

-- Schedules

DELETE FROM lms.event_comments WHERE event_reference_id IN (SELECT id FROM future_schedule_events);
DELETE FROM lms.work_programme_activity_statuses WHERE event_reference_id IN (SELECT id FROM future_schedule_events);

-- A work programme activity on another licence can record a transfer to one of these licences
UPDATE lms.work_programme_activity_statuses
SET licence_transferred_to = NULL
WHERE licence_transferred_to IN (SELECT id FROM future_licences);

DELETE FROM lms.licence_schedule_rates WHERE licence_schedule_detail_id IN (SELECT id FROM future_schedule_details);
DELETE FROM lms.other_schedule_events WHERE licence_schedule_detail_id IN (SELECT id FROM future_schedule_details);
DELETE FROM lms.work_programme_activities WHERE licence_schedule_detail_id IN (SELECT id FROM future_schedule_details);
DELETE FROM lms.licence_schedule_expiry_dates WHERE licence_schedule_detail_id IN (SELECT id FROM future_schedule_details);
DELETE FROM lms.licence_start_dates WHERE licence_schedule_detail_id IN (SELECT id FROM future_schedule_details);
DELETE FROM lms.licence_schedule_phases WHERE licence_schedule_detail_id IN (SELECT id FROM future_schedule_details);
DELETE FROM lms.licence_schedule_terms WHERE licence_schedule_detail_id IN (SELECT id FROM future_schedule_details);
DELETE FROM lms.schedule_events WHERE id IN (SELECT id FROM future_schedule_events);
DELETE FROM lms.licence_schedule_details WHERE id IN (SELECT id FROM future_schedule_details);
DELETE FROM lms.licence_schedules WHERE id IN (SELECT id FROM future_schedules);

-- Corrections and positions

DELETE FROM lms.licence_position_corrections
WHERE licence_correction_id IN (SELECT id FROM future_corrections)
   OR target_licence_position_id IN (SELECT id FROM future_positions);
DELETE FROM lms.licence_corrections WHERE id IN (SELECT id FROM future_corrections);
DELETE FROM lms.licence_position_changes WHERE licence_position_id IN (SELECT id FROM future_positions);
DELETE FROM lms.licence_positions WHERE id IN (SELECT id FROM future_positions);

DELETE FROM lms.licence_transactions
WHERE id IN (SELECT licence_transaction_id FROM future_positions)
  AND NOT EXISTS (SELECT 1 FROM lms.licence_positions lp WHERE lp.licence_transaction_id = licence_transactions.id);

-- The licences

DELETE FROM lms.licence_contact WHERE licence_id IN (SELECT id FROM future_licences);
DELETE FROM lms.licence_responsible_organisations WHERE licence_id IN (SELECT id FROM future_licences);
DELETE FROM lms.licence_statuses WHERE licence_id IN (SELECT id FROM future_licences);
DELETE FROM lms.licences WHERE id IN (SELECT id FROM future_licences);

DROP TABLE future_licences, future_schedules, future_schedule_details, future_schedule_events, future_swp_applications,
  future_swp_application_details, future_continuation_applications, future_continuation_application_details,
  future_corrections, future_positions, future_contributor_teams, future_document_instances;
