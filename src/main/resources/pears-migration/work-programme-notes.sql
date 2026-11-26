-- Every note of one licence's current work programmes (as work-programmes.sql), in
-- the order each programme holds them: GENERAL notes and status-change notes, which
-- carry the activity and its new status.
--
-- All notes are returned, including the system-authored "Migration Process" ones
-- (plan M6), so the mapper can leave those out and count them for the report.
--
-- DATETIME is an xs:dateTime. Its first 19 characters are read as a timestamp, and
-- one that can't be read comes back null rather than failing the query. Every other
-- value is returned as PEARS holds it, and DESCRIPTION is a CLOB.
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
SELECT
  c.ped_wp_id
, n.position
, n.note_type
, n.activity_id
, n.activity_title
, n.new_activity_status
, TO_CHAR(
    TO_TIMESTAMP(SUBSTR(n.note_datetime, 1, 19) DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD"T"HH24:MI:SS'),
    'YYYY-MM-DD"T"HH24:MI:SS'
  ) note_datetime
, n.created_by
, n.description
FROM cur_wp c
CROSS APPLY XMLTABLE('/WORK_PROGRAMME/NOTES_LIST/NOTE'
    PASSING c.xml_data
    COLUMNS
      position FOR ORDINALITY
    , note_type VARCHAR2(4000) PATH 'TYPE'
    , activity_id VARCHAR2(4000) PATH 'ACTIVITY_ID'
    , activity_title VARCHAR2(4000) PATH 'ACTIVITY_TITLE'
    , new_activity_status VARCHAR2(4000) PATH 'NEW_ACTIVITY_STATUS'
    , note_datetime VARCHAR2(4000) PATH 'DATETIME'
    , created_by VARCHAR2(4000) PATH 'CREATED_BY'
    , description CLOB PATH 'DESCRIPTION'
  ) n
ORDER BY c.ped_wp_id, n.position
