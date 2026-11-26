-- One licence's current work programmes, apart from DRAFTs, oldest programme first:
-- one row per programme (INITIAL, FALLOW, EXTENSION) with its programme-level values.
--
-- "Current" and the DRAFT filter are as in work-programme-licence-references.sql.
-- The well-count matrix (plan D12) and LEGAL_TEXT (D14) are not migrated, so not
-- read. Values are returned as PEARS holds them, untrimmed and unparsed: the
-- breakpoint can be empty or partial, and judging it is the mapper's job.
SELECT
  wp.id ped_wp_id
, wpd.id ped_wpd_id
, TO_CHAR(wpd.start_datetime, 'YYYY-MM-DD"T"HH24:MI:SS') start_datetime
, x.work_programme_type
, x.title
, x.work_programme_status
, x.breakpoint_years
, x.breakpoint_months
, x.breakpoint_days
FROM pedmgr.ped_work_programme wp
JOIN pedmgr.ped_licence_master plm ON plm.id = wp.ped_lm_id
JOIN pedmgr.ped_work_programme_details wpd ON wpd.ped_wp_id = wp.id
CROSS APPLY XMLTABLE('/WORK_PROGRAMME'
    PASSING wpd.xml_data
    COLUMNS
      work_programme_type VARCHAR2(4000) PATH 'WORK_PROGRAMME_TYPE'
    , title VARCHAR2(4000) PATH 'TITLE'
    , work_programme_status VARCHAR2(4000) PATH 'WORK_PROGRAMME_STATUS'
    , breakpoint_years VARCHAR2(4000) PATH 'BREAKPOINT/YEARS'
    , breakpoint_months VARCHAR2(4000) PATH 'BREAKPOINT/MONTHS'
    , breakpoint_days VARCHAR2(4000) PATH 'BREAKPOINT/DAYS'
  ) x
WHERE plm.licence_type = ? AND plm.licence_no = ?
AND wpd.status_control = 'C'
AND wpd.record_status = 'ACTIVE'
AND NVL(x.work_programme_status, '-') != 'DRAFT'
ORDER BY wp.id
