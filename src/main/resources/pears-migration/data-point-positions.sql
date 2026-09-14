-- The positions PEARS itself holds for one licence, taken from its own data points
-- rather than re-derived from the operations, so it is an oracle the migration's
-- reading of PEARS can be checked against rather than a restatement of it.
--
-- PED_DATA_POINTS is PEARS' record of "this licence holds a position here": it
-- names the licence directly, and carries the position's date and its sequence
-- within that date. One row per position, so nothing needs grouping back up.
--
-- FIELD_GHOST data points are excluded. They are transactions on other licences
-- that changed a field on this one -- positions in PEARS, but with no operation on
-- this licence to build one from, and out of scope until fields are settled in the
-- new model.
--
-- A (licence, transaction) pair can hold two live-series data points, from a
-- transaction master executed twice on one date, so this can return two rows for
-- one transaction. That is what PEARS holds and is left as two positions.
SELECT
  dp.licence_type
, dp.licence_no
, xpt.regulator_reference_full
, TO_CHAR(dp.position_datetime, 'YYYY-MM-DD') position_date
, dp.position_sequence -- the order of positions on the same date
FROM pedmgr.ped_data_points dp
JOIN pedmgr.xview_ped_transactions xpt ON xpt.ped_tran_id = dp.ped_tran_id
WHERE dp.licence_type = ? AND dp.licence_no = ?
AND dp.ped_sim_id = 0 -- only sim 0 (live sim) data points
AND (dp.flag_list IS NULL OR dp.flag_list NOT LIKE '%FIELD_GHOST%')
ORDER BY dp.position_datetime, dp.position_sequence
