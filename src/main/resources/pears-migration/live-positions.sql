-- The live positions PEARS holds for one licence, one row per operation, so a
-- position appears once per operation that made it and is grouped back up by
-- position date and sequence.
SELECT
  xpo.licence_type
, xpo.licence_no
, xpt.regulator_reference_full
, TO_CHAR(xpt.position_datetime, 'YYYY-MM-DD') position_date
, pst.position_sequence -- the order of transactions on the same date
FROM pedmgr.ped_simulation_transactions pst
JOIN pedmgr.ped_transactions pt ON pt.id = pst.ped_tran_id
JOIN pedmgr.xview_ped_transactions xpt ON xpt.ped_tran_id = pt.id
JOIN pedmgr.ped_operations po ON po.ped_tran_id = pt.id
JOIN pedmgr.xview_ped_operations xpo ON xpo.ped_operation_id = po.id
WHERE xpo.licence_type = ? AND xpo.licence_no = ?
AND pst.ped_sim_id = 0 -- only sim 0 (live sim) transactions
AND pt.status = 'EXECUTED' -- that are executed (should always be the case except for bad dev data)
AND po.status IN ('LIVE','LEGACY','CORRECTED') -- only operations in status relevant to the execution
ORDER BY xpo.licence_type, xpo.licence_no, xpt.position_datetime, pst.position_sequence, po.operation_sequence
