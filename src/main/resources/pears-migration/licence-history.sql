-- One licence's operation history, as the LICENCE_OPERATION_HISTORY document the reader binds.
--
-- PED_OPERATIONS.XML_DATA is already the <OPERATION> element, so this only has to wrap each one in
-- the transaction and operation it belongs to. The joins and the filter are the ones the retired
-- live-positions.sql used, so the positions this document yields are the positions that query
-- yielded: one per executed transaction in the live simulation.
--
-- The payload is restricted and the entries are not. Every operation produces an OPERATION_ENTRY,
-- because a licence's positions are its distinct transactions and a transaction that did nothing
-- this application migrates is still a position PEARS holds; only the operation types some migrator
-- asked for carry their XML_DATA. That is what keeps a history in the tens of kilobytes rather than
-- the tens of megabytes -- the geometry on block and subarea operations is the bulk of a document,
-- and none of it is read. An entry with no payload binds to a header-only operation.
--
-- The IN list is interpolated rather than bound: Oracle has no list bind, and the values are the
-- migrators' declared operation types, checked against a strict pattern before they get here.
SELECT XMLELEMENT("LICENCE_OPERATION_HISTORY",
         XMLATTRIBUTES(? AS "licence_type", ? AS "licence_no"),
         XMLAGG(
           XMLELEMENT("OPERATION_ENTRY",
             XMLATTRIBUTES(
               pt.id AS "tran_id"
             , xpt.regulator_reference_full AS "regulator_reference"
             , TO_CHAR(xpt.position_datetime, 'YYYY-MM-DD') AS "position_date"
             , pst.position_sequence AS "position_sequence"
             , po.id AS "op_id"
             , po.operation_sequence AS "op_seq"
             , po.status AS "op_status"
             , xpo.operation_type AS "op_type"),
             CASE WHEN xpo.operation_type IN (%s) THEN po.xml_data END)
           ORDER BY xpt.position_datetime, pst.position_sequence, po.operation_sequence, po.id)
       ).getclobval()
FROM pedmgr.ped_simulation_transactions pst
JOIN pedmgr.ped_transactions pt ON pt.id = pst.ped_tran_id
JOIN pedmgr.xview_ped_transactions xpt ON xpt.ped_tran_id = pt.id
JOIN pedmgr.ped_operations po ON po.ped_tran_id = pt.id
JOIN pedmgr.xview_ped_operations xpo ON xpo.ped_operation_id = po.id
WHERE xpo.licence_type = ? AND xpo.licence_no = ?
AND pst.ped_sim_id = 0 -- only sim 0 (live sim) transactions
AND pt.status = 'EXECUTED' -- that are executed (should always be the case except for bad dev data)
AND po.status IN ('LIVE','LEGACY','CORRECTED') -- only operations in status relevant to the execution
