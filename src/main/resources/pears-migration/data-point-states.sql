-- What the licence holds at each of its positions, as PEARS itself records it.
--
-- PEARS keeps a whole copy of the licence at every data point: its administrator
-- and licensee lists (PED_CONSORTIUMS), its blocks (PED_LICENCE_BLOCKS) and its
-- subareas (PED_SUBAREAS), each keyed by PED_DP_ID. Each copy also carries the
-- history up to that data point, so only rows with no end date are held there.
--
-- One row per thing held, told apart by held_kind. Positions are identified by
-- date and sequence, as in data-point-positions.sql, rather than by data point, so
-- the rows line up with the positions that query returns.
--
-- Beneficial interests and subarea operators are held per subarea in PEARS too,
-- but are left out until there is a migration to compare them with.
SELECT
  TO_CHAR(dp.position_datetime, 'YYYY-MM-DD') position_date
, dp.position_sequence
, pc.context_domain held_kind -- LICENCE_ADMINISTRATOR or LICENSEE
, pcd.ou_id
, CAST(NULL AS NUMBER) block_si_id
, CAST(NULL AS NUMBER) subarea_si_id
, CAST(NULL AS VARCHAR2(4000)) subarea_short_name
FROM pedmgr.ped_data_points dp
JOIN pedmgr.ped_consortiums pc ON pc.ped_dp_id = dp.id
JOIN pedmgr.ped_consortium_details pcd ON pcd.ped_con_id = pc.id
WHERE dp.licence_type = ? AND dp.licence_no = ?
AND dp.ped_sim_id = 0
AND (dp.flag_list IS NULL OR dp.flag_list NOT LIKE '%FIELD_GHOST%')
AND pc.context_domain IN ('LICENCE_ADMINISTRATOR', 'LICENSEE')
AND pcd.end_datetime IS NULL
UNION ALL
SELECT
  TO_CHAR(dp.position_datetime, 'YYYY-MM-DD')
, dp.position_sequence
, 'BLOCK'
, NULL
, plb.si_id
, NULL
, NULL
FROM pedmgr.ped_data_points dp
JOIN pedmgr.ped_licence_blocks plb ON plb.ped_dp_id = dp.id
WHERE dp.licence_type = ? AND dp.licence_no = ?
AND dp.ped_sim_id = 0
AND (dp.flag_list IS NULL OR dp.flag_list NOT LIKE '%FIELD_GHOST%')
AND plb.end_datetime IS NULL
UNION ALL
SELECT
  TO_CHAR(dp.position_datetime, 'YYYY-MM-DD')
, dp.position_sequence
, 'SUBAREA'
, NULL
, plb.si_id
, ps.si_id
, ps.short_name
FROM pedmgr.ped_data_points dp
JOIN pedmgr.ped_subareas ps ON ps.ped_dp_id = dp.id
JOIN pedmgr.ped_licence_blocks plb ON plb.id = ps.ped_lb_id
WHERE dp.licence_type = ? AND dp.licence_no = ?
AND dp.ped_sim_id = 0
AND (dp.flag_list IS NULL OR dp.flag_list NOT LIKE '%FIELD_GHOST%')
AND ps.end_datetime IS NULL
