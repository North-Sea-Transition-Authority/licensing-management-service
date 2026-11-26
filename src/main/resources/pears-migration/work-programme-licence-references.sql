-- Every numbered licence with a current work programme that isn't a DRAFT.
--
-- "Current" is the details row with STATUS_CONTROL = 'C' and RECORD_STATUS =
-- 'ACTIVE': DELETE_WORK_PROGRAMME is a soft delete that leaves STATUS_CONTROL =
-- 'C', so both are needed. DRAFT programmes are not migrated (plan, "Scope:
-- programme statuses"); on live they are only on unnumbered awards anyway.
SELECT DISTINCT
  plm.licence_type
, plm.licence_no
FROM pedmgr.ped_work_programme wp
JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
WHERE wpd.status_control = 'C'
AND wpd.record_status = 'ACTIVE'
AND NVL(
  XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)),
  '-'
) != 'DRAFT'
AND plm.licence_type IS NOT NULL
AND plm.licence_no IS NOT NULL
ORDER BY plm.licence_type, plm.licence_no
