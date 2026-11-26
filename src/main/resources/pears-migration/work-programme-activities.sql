-- Every activity of one licence's current work programmes (as work-programmes.sql),
-- in the order each programme holds them: the flat ACTIVITY_LIST ("general
-- organisation" rounds) and every PHASE_LIST/PHASE/ACTIVITY_LIST ("phase
-- organisation" rounds). list_kind says which, and phase_type names the phase.
--
-- status_since is when the activity first had its current status: the start of the
-- earliest ACTIVE version of its programme holding it with that status. It dates the
-- status of an activity with no status note (plan M5), which is better than the
-- current version's start, often just a later review save.
--
-- DESCRIPTION is a CLOB, keeping its line breaks. Every value is returned as PEARS
-- holds it, untrimmed and unparsed, for the mapper to judge.
WITH cur_wp AS (
  SELECT wp.id ped_wp_id, wpd.xml_data
  FROM pedmgr.ped_work_programme wp
  JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
  JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
  WHERE plm.licence_type = ? AND plm.licence_no = ?
  AND wpd.status_control = 'C'
  AND wpd.record_status = 'ACTIVE'
  AND NVL(
    XMLCAST(XMLQUERY('/WORK_PROGRAMME/WORK_PROGRAMME_STATUS/text()' PASSING wpd.xml_data RETURNING CONTENT) AS VARCHAR2(20)),
    '-'
  ) != 'DRAFT'
)
, acts AS (
  SELECT c.ped_wp_id, a.*
  FROM cur_wp c
  CROSS APPLY XMLTABLE(
      'for $a at $i in /WORK_PROGRAMME//ACTIVITY
       return <A>
         {$a/ACTIVITY_ID, $a/ACTIVITY_TYPE, $a/TITLE, $a/ACTIVITY_STATUS, $a/COMPETED_BLOCKS, $a/DESCRIPTION}
         <POSITION>{$i}</POSITION>
         <LIST_KIND>{if ($a/parent::ACTIVITY_LIST/parent::PHASE) then "PHASE" else "FLAT"}</LIST_KIND>
         <PHASE_TYPE>{$a/parent::ACTIVITY_LIST/parent::PHASE/TYPE/text()}</PHASE_TYPE>
       </A>'
      PASSING c.xml_data
      COLUMNS
        position NUMBER PATH 'POSITION'
      , list_kind VARCHAR2(5) PATH 'LIST_KIND'
      , phase_type VARCHAR2(4000) PATH 'PHASE_TYPE'
      , activity_id VARCHAR2(4000) PATH 'ACTIVITY_ID'
      , activity_type VARCHAR2(4000) PATH 'ACTIVITY_TYPE'
      , title VARCHAR2(4000) PATH 'TITLE'
      , activity_status VARCHAR2(4000) PATH 'ACTIVITY_STATUS'
      , competed_blocks VARCHAR2(4000) PATH 'COMPETED_BLOCKS'
      , description CLOB PATH 'DESCRIPTION'
    ) a
)
, status_since AS (
  SELECT wpd.ped_wp_id, h.activity_id, h.activity_status, MIN(wpd.start_datetime) first_with_status
  FROM pedmgr.ped_work_programme_details wpd
  CROSS APPLY XMLTABLE('/WORK_PROGRAMME//ACTIVITY'
      PASSING wpd.xml_data
      COLUMNS
        activity_id VARCHAR2(4000) PATH 'ACTIVITY_ID'
      , activity_status VARCHAR2(4000) PATH 'ACTIVITY_STATUS'
    ) h
  WHERE wpd.record_status = 'ACTIVE'
  AND wpd.ped_wp_id IN (SELECT ped_wp_id FROM cur_wp)
  GROUP BY wpd.ped_wp_id, h.activity_id, h.activity_status
)
SELECT
  a.ped_wp_id
, a.position
, a.list_kind
, a.phase_type
, a.activity_id
, a.activity_type
, a.title
, a.activity_status
, a.competed_blocks
, a.description
, TO_CHAR(s.first_with_status, 'YYYY-MM-DD"T"HH24:MI:SS') status_since
FROM acts a
LEFT JOIN status_since s
  ON s.ped_wp_id = a.ped_wp_id
  AND s.activity_id = a.activity_id
  AND s.activity_status = a.activity_status
ORDER BY a.ped_wp_id, a.position
