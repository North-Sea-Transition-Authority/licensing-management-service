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
-- Block operations carry their entries instead, shredded out of the payload and resolved against
-- the rows on the transaction's live data point: the primary side is the block ended (or created,
-- on a SET) and the secondary the block that replaced it.
--
-- The IN list is interpolated rather than bound: Oracle has no list bind, and the values are the
-- migrators' declared operation types, checked against a strict pattern before they get here. Every
-- bind is a licence type and number pair, in that order.
WITH block_entries AS (
    SELECT e.*
         , in_plb.si_id AS input_si_id
         -- composed the way PED_LICENCE_BLOCK_REFS does: seaward (P) refs separate quadrant and block with a slash
         , CASE WHEN e.entry_type <> 'SET'
                THEN e.prim_quad_no || CASE WHEN e.licence_type = 'P' THEN '/' END || e.prim_block_no || e.prim_block_suffix
           END AS input_block_ref
         , in_plb.calculated_size_km2 AS input_area_km2
         , out_plb.si_id AS output_si_id
         , CASE e.entry_type
             WHEN 'SET' THEN e.prim_quad_no || CASE WHEN e.licence_type = 'P' THEN '/' END || e.prim_block_no || e.prim_block_suffix
             WHEN 'TRANSFER' THEN e.sec_quad_no || CASE WHEN e.licence_type = 'P' THEN '/' END || e.sec_block_no || e.sec_block_suffix
           END AS output_block_ref
         , out_plb.calculated_size_km2 AS output_area_km2
    FROM (
             SELECT po.id AS op_id
                  , xpo.operation_type
                  , pdp.id AS pdp_id
                  , pdp.licence_type
                  , pdp.position_datetime
                  , be.entry_seq
                  , be.entry_type
                  , be.prim_quad_no
                  , be.prim_block_no
                  , be.prim_block_suffix
                  , be.sec_quad_no
                  , be.sec_block_no
                  , be.sec_block_suffix
                  , CASE WHEN be.entry_type IN ('REMOVE','TRANSFER') THEN (
                 SELECT MAX(plb.id)
                 FROM pedmgr.ped_licence_blocks plb
                      JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = plb.id
                 WHERE plb.ped_dp_id = pdp.id
                         AND plbr.quadrant_no = be.prim_quad_no
                         AND plbr.block_no = be.prim_block_no
                         AND COALESCE(plbr.suffix, 'x') = COALESCE(be.prim_block_suffix, 'x')
                       -- the same suffix may be used for a new one, we want the one ended on this change
                         AND plb.end_datetime = pdp.position_datetime
             ) END AS input_plb_id
                  , CASE be.entry_type
                 WHEN 'SET' THEN (
                     SELECT MAX(plb.id) -- should only be one
                     FROM pedmgr.ped_licence_blocks plb
                          JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = plb.id
                     WHERE plb.ped_dp_id = pdp.id
                             AND plbr.quadrant_no = be.prim_quad_no
                             AND plbr.block_no = be.prim_block_no
                             AND COALESCE(plbr.suffix, 'x') = COALESCE(be.prim_block_suffix, 'x')
                 )
                 WHEN 'TRANSFER' THEN (
                     SELECT MAX(plb.id)
                     FROM pedmgr.ped_licence_blocks plb
                          JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = plb.id
                     WHERE plb.ped_dp_id = pdp.id
                             AND plbr.quadrant_no = be.sec_quad_no
                             AND plbr.block_no = be.sec_block_no
                             AND COALESCE(plbr.suffix, 'x') = COALESCE(be.sec_block_suffix, 'x')
                           -- only one block change per data point, so the secondary can't later be ended
                             AND plb.end_datetime IS NULL
                 )
                 END AS output_plb_id
             FROM pedmgr.ped_data_points pdp
                  JOIN pedmgr.ped_operations po ON po.ped_tran_id = pdp.ped_tran_id
                  JOIN pedmgr.xview_ped_operations xpo ON xpo.ped_operation_id = po.id
                  CROSS JOIN XMLTABLE(
                     '/OPERATION/BLOCK_ENTRY_LIST/BLOCK_ENTRY'
                         PASSING po.xml_data
      COLUMNS
        entry_seq FOR ORDINALITY
                 , entry_type        VARCHAR2(30) PATH 'ENTRY_TYPE/text()'
                 , prim_quad_no      VARCHAR2(60) PATH 'PRIMARY_BLOCK/QUADRANT_NO/text()'
                 , prim_block_no     VARCHAR2(60) PATH 'PRIMARY_BLOCK/BLOCK_NO/text()'
                 , prim_block_suffix VARCHAR2(60) PATH 'PRIMARY_BLOCK/BLOCK_SUFFIX/text()'
                 , sec_quad_no       VARCHAR2(60) PATH 'SECONDARY_BLOCK/QUADRANT_NO/text()'
                 , sec_block_no      VARCHAR2(60) PATH 'SECONDARY_BLOCK/BLOCK_NO/text()'
                 , sec_block_suffix  VARCHAR2(60) PATH 'SECONDARY_BLOCK/BLOCK_SUFFIX/text()'
                             ) be
             WHERE pdp.licence_type = ? AND pdp.licence_no = ?
                     AND pdp.ped_sim_id = 0
                     AND xpo.licence_type = ? AND xpo.licence_no = ?
                     AND xpo.operation_type IN ('PED_BLOCK_CREATE','PED_BLOCK_CHANGE','PED_BLOCK_END')
                     AND xpo.status IN ('LIVE','LEGACY','CORRECTED')
         ) e
         LEFT JOIN pedmgr.ped_licence_blocks in_plb ON in_plb.id = e.input_plb_id
         LEFT JOIN pedmgr.ped_licence_blocks out_plb ON out_plb.id = e.output_plb_id
)

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
                                     CASE WHEN xpo.operation_type IN (%s) THEN po.xml_data END,
                                     CASE WHEN xpo.operation_type IN ('PED_BLOCK_CREATE','PED_BLOCK_CHANGE','PED_BLOCK_END')
                                              THEN XMLELEMENT("BLOCK_ENTRY_LIST", bel.entries) END)
                              ORDER BY xpt.position_datetime, pst.position_sequence, po.operation_sequence, po.id)
       ).getclobval()
FROM pedmgr.ped_simulation_transactions pst
     JOIN pedmgr.ped_transactions pt ON pt.id = pst.ped_tran_id
     JOIN pedmgr.xview_ped_transactions xpt ON xpt.ped_tran_id = pt.id
     JOIN pedmgr.ped_operations po ON po.ped_tran_id = pt.id
     JOIN pedmgr.xview_ped_operations xpo ON xpo.ped_operation_id = po.id
    -- aggregated per operation before joining, so the outer rows cannot multiply
     LEFT JOIN (
    SELECT op_id
         , XMLAGG(
            XMLELEMENT("BLOCK_ENTRY",
                       XMLATTRIBUTES(
                               entry_type AS "entry_type"
                           , entry_seq AS "entry_seq"
                           , CASE WHEN entry_type <> 'REMOVE' THEN COALESCE(sec_quad_no, prim_quad_no) END AS "output_quadrant_no"
                           , CASE WHEN entry_type <> 'REMOVE' THEN COALESCE(sec_block_no, prim_block_no) END AS "output_block_no"
                           , CASE WHEN entry_type <> 'REMOVE' THEN COALESCE(sec_block_suffix, prim_block_suffix) END AS "output_block_suffix"
                           , output_block_ref AS "output_block_ref"
                           , output_si_id AS "output_si_id"
                           , output_area_km2 AS "output_area_km2"
                           , CASE WHEN entry_type <> 'SET' THEN prim_quad_no END AS "input_quadrant_no"
                           , CASE WHEN entry_type <> 'SET' THEN prim_block_no END AS "input_block_no"
                           , CASE WHEN entry_type <> 'SET' THEN prim_block_suffix END AS "input_block_suffix"
                           , input_block_ref AS "input_block_ref"
                           , input_si_id AS "input_si_id"
                           , input_area_km2 AS "input_area_km2"))
                ORDER BY entry_seq) AS entries
    FROM block_entries
    GROUP BY op_id
) bel ON bel.op_id = po.id
WHERE xpo.licence_type = ? AND xpo.licence_no = ?
        AND pst.ped_sim_id = 0 -- only sim 0 (live sim) transactions
        AND pt.status = 'EXECUTED' -- that are executed (should always be the case except for bad dev data)
        AND po.status IN ('LIVE','LEGACY','CORRECTED') -- only operations in status relevant to the execution
