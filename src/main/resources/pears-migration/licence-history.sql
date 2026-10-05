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
-- Block and subarea operations carry their entries instead, shredded out of the payload and
-- resolved against the rows on the transaction's live data point: the primary side is the version
-- ended (or created, on a SET) and the secondary the version that replaced it. A block operation
-- names no subareas, so its subarea entries are the versions it made on the new block and ended on
-- the old one.
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
),

subarea_entries AS (
  SELECT e.*
       , in_ps.si_id AS input_subarea_si_id
       , in_ps.title AS input_subarea_name
       , in_plb.si_id AS input_block_si_id
       -- correlated so plb_id pushes into the view as an index lookup, joined it scans every block
       , (SELECT r.block_ref FROM pedmgr.ped_licence_block_refs r WHERE r.plb_id = in_plb.id) AS input_block_ref
       , out_ps.si_id AS output_subarea_si_id
       , out_ps.title AS output_subarea_name
       , out_plb.si_id AS output_block_si_id
       , (SELECT r.block_ref FROM pedmgr.ped_licence_block_refs r WHERE r.plb_id = out_plb.id) AS output_block_ref
  FROM (
    SELECT po.id AS op_id
         , se.entry_seq
         , CAST(NULL AS NUMBER) AS block_entry_seq
         , se.entry_type
         , COALESCE(se.sec_short_name, se.prim_short_name) AS short_name
         , CASE se.entry_type
             WHEN 'REMOVE' THEN (
               SELECT MAX(ps.id)
               FROM pedmgr.ped_subareas ps
               JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = ps.ped_lb_id
               WHERE ps.ped_dp_id = pdp.id
               AND plbr.block_ref = op_block.block_ref
               AND ps.short_name = se.prim_short_name
               -- just in case they recreated on this position with the same name, it wouldn't be
               -- ended again until a later position
               -- created_by_operation_type is whatever last created it, subarea or block operation,
               -- so no filter on it; the subarea is ended so no block change can act on it later
               AND ps.end_datetime = pdp.position_datetime
             )
             WHEN 'TRANSFER' THEN (
               SELECT MAX(ps.id)
               FROM pedmgr.ped_subareas ps
               JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = ps.ped_lb_id
               WHERE ps.ped_dp_id = pdp.id
               AND plbr.block_ref = op_block.block_ref
               AND ps.short_name = se.prim_short_name
               -- in case the same name is used for a new one, we want the one ended on this change
               AND ps.end_datetime = pdp.position_datetime
               -- belt and braces, the primary subarea should have a lower id than the secondary
               -- subarea which is just being created as part of this change
               AND ps.id < (
                 SELECT MAX(ps2.id)
                 FROM pedmgr.ped_subareas ps2
                 JOIN pedmgr.ped_licence_block_refs plbr2 ON plbr2.plb_id = ps2.ped_lb_id
                 WHERE ps2.ped_dp_id = pdp.id
                 AND plbr2.block_ref = op_block.block_ref
                 AND ps2.short_name = se.sec_short_name
                 AND (ps2.created_by_operation_type = 'PED_SUBAREA_CHANGE' OR ps2.created_by_operation_type IS NULL)
                 AND ps2.start_datetime = pdp.position_datetime
               )
             )
           END AS input_ps_id
         , CASE se.entry_type
             WHEN 'SET' THEN (
               SELECT MAX(ps.id)
               FROM pedmgr.ped_subareas ps
               JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = ps.ped_lb_id
               WHERE ps.ped_dp_id = pdp.id
               AND plbr.block_ref = op_block.block_ref
               AND ps.short_name = se.prim_short_name
               -- subareas created by set are always from a ped_subarea_create or ped_subarea_change,
               -- which keeps out block versions from a block operation later on the same data point
               AND ps.created_by_operation_type IN ('PED_SUBAREA_CREATE','PED_SUBAREA_CHANGE')
               -- could be ended later on same data point so no end_datetime NULL filter
             )
             WHEN 'TRANSFER' THEN (
               SELECT MAX(ps.id)
               FROM pedmgr.ped_subareas ps
               JOIN pedmgr.ped_licence_block_refs plbr ON plbr.plb_id = ps.ped_lb_id
               WHERE ps.ped_dp_id = pdp.id
               AND plbr.block_ref = op_block.block_ref
               AND ps.short_name = se.sec_short_name
               -- subareas created by transfer are always from a ped_subarea_change, or null on
               -- legacy data, which keeps out block versions from a later block operation
               AND (ps.created_by_operation_type = 'PED_SUBAREA_CHANGE' OR ps.created_by_operation_type IS NULL)
               -- created on this position, so an older version carried onto the data point can't match
               AND ps.start_datetime = pdp.position_datetime
               -- could be ended later on same data point so no end_datetime NULL filter
             )
           END AS output_ps_id
    FROM pedmgr.ped_data_points pdp
    JOIN pedmgr.ped_operations po ON po.ped_tran_id = pdp.ped_tran_id
    JOIN pedmgr.xview_ped_operations xpo ON xpo.ped_operation_id = po.id
    CROSS JOIN XMLTABLE(
      '/OPERATION'
      PASSING po.xml_data
      COLUMNS block_ref VARCHAR2(30) PATH 'ATTRIBUTE_LIST/ATTRIBUTE_SET/ATTRIBUTE[NAME="BLOCK_REF"]/VALUE/text()'
    ) op_block
    CROSS JOIN XMLTABLE(
      '/OPERATION/SUBAREA_ENTRY_LIST/SUBAREA_ENTRY'
      PASSING po.xml_data
      COLUMNS
        entry_seq FOR ORDINALITY
      , entry_type      VARCHAR2(30) PATH 'ENTRY_TYPE/text()'
      , prim_short_name VARCHAR2(60) PATH 'PRIMARY_SUBAREA/SHORT_NAME/text()'
      , sec_short_name  VARCHAR2(60) PATH 'SECONDARY_SUBAREA/SHORT_NAME/text()'
    ) se
    WHERE pdp.licence_type = ? AND pdp.licence_no = ?
    AND pdp.ped_sim_id = 0
    AND xpo.licence_type = ? AND xpo.licence_no = ?
    AND xpo.operation_type IN ('PED_SUBAREA_CREATE','PED_SUBAREA_CHANGE','PED_SUBAREA_END')
    AND xpo.status IN ('LIVE','LEGACY','CORRECTED')

    UNION ALL

    -- A block operation's subareas, paired by short name within each block entry: the version it
    -- created on the new block and the version ended on the old one.
    SELECT op_id
         , block_entry_seq AS entry_seq
         , block_entry_seq
         , CASE WHEN input_ps_id IS NULL THEN 'SET'
                WHEN output_ps_id IS NULL THEN 'REMOVE'
                ELSE 'TRANSFER' END AS entry_type
         , short_name
         , input_ps_id
         , output_ps_id
    FROM (
      SELECT sides.*
           -- a split block repeats the block it ended on every successor entry, so a subarea is
           -- only dropped if it reached none of them, and then reported once
           , MAX(output_ps_id) OVER (PARTITION BY op_id, input_plb_id, short_name) AS carried_ps_id
           , ROW_NUMBER() OVER (PARTITION BY op_id, input_plb_id, short_name ORDER BY block_entry_seq) AS drop_seq
      FROM (
        SELECT be.op_id
             , be.entry_seq AS block_entry_seq
             , be.input_plb_id
             , ps.short_name
             , MAX(CASE WHEN ps.ped_lb_id = be.input_plb_id THEN ps.id END)
                 KEEP (DENSE_RANK LAST ORDER BY CASE WHEN ps.ped_lb_id = be.input_plb_id THEN ps.start_datetime END NULLS FIRST, ps.id)
                 AS input_ps_id
             , MAX(CASE WHEN ps.ped_lb_id = be.output_plb_id THEN ps.id END)
                 KEEP (DENSE_RANK LAST ORDER BY CASE WHEN ps.ped_lb_id = be.output_plb_id THEN ps.start_datetime END NULLS FIRST, ps.id)
                 AS output_ps_id
        FROM block_entries be
        JOIN pedmgr.ped_subareas ps
          ON ps.ped_dp_id = be.pdp_id
         AND ps.ped_lb_id IN (be.input_plb_id, be.output_plb_id)
        -- the versions current at this change: ended on it, or on the new block still open
        -- a later subarea operation on the same data point can close a new block version again
        WHERE (ps.ped_lb_id = be.input_plb_id AND ps.end_datetime = be.position_datetime)
        OR (ps.ped_lb_id = be.output_plb_id
            AND (ps.end_datetime IS NULL OR ps.end_datetime = be.position_datetime)
            -- a block change copies each subarea onto the new block as it stood, keeping its start
            -- and created_by, and makes a new version only where it re-cut one; a version started
            -- here by anything else is a subarea operation later on the same data point
            AND (ps.start_datetime < be.position_datetime
                 OR ps.created_by_operation_type = be.operation_type
                 OR ps.created_by_operation_type IS NULL))
        GROUP BY be.op_id, be.entry_seq, be.input_plb_id, ps.short_name
      ) sides
      WHERE input_ps_id IS NOT NULL OR output_ps_id IS NOT NULL
    )
    WHERE output_ps_id IS NOT NULL
    OR (carried_ps_id IS NULL AND drop_seq = 1)
  ) e
  LEFT JOIN pedmgr.ped_subareas in_ps ON in_ps.id = e.input_ps_id
  LEFT JOIN pedmgr.ped_licence_blocks in_plb ON in_plb.id = in_ps.ped_lb_id
  LEFT JOIN pedmgr.ped_subareas out_ps ON out_ps.id = e.output_ps_id
  LEFT JOIN pedmgr.ped_licence_blocks out_plb ON out_plb.id = out_ps.ped_lb_id
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
                                          THEN XMLELEMENT("BLOCK_ENTRY_LIST", bel.entries) END,
                                     CASE WHEN xpo.operation_type IN ('PED_BLOCK_CREATE','PED_BLOCK_CHANGE','PED_BLOCK_END',
                                                                      'PED_SUBAREA_CREATE','PED_SUBAREA_CHANGE','PED_SUBAREA_END')
                                          THEN XMLELEMENT("SUBAREA_ENTRY_LIST", sel.entries) END)
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
     LEFT JOIN (
       SELECT op_id
            , XMLAGG(
                XMLELEMENT("SUBAREA_ENTRY",
                  XMLATTRIBUTES(
                      entry_type AS "entry_type"
                    , block_entry_seq AS "block_entry_seq"
                    , short_name AS "subarea_short_name"
                    , output_subarea_name AS "output_subarea_name"
                    , output_subarea_si_id AS "output_subarea_si_id"
                    , output_block_ref AS "output_block_ref"
                    , output_block_si_id AS "output_block_si_id"
                    , input_subarea_name AS "input_subarea_name"
                    , input_subarea_si_id AS "input_subarea_si_id"
                    , input_block_ref AS "input_block_ref"
                    , input_block_si_id AS "input_block_si_id"))
                ORDER BY entry_seq, short_name) AS entries
       FROM subarea_entries
       GROUP BY op_id
     ) sel ON sel.op_id = po.id
WHERE xpo.licence_type = ? AND xpo.licence_no = ?
        AND pst.ped_sim_id = 0 -- only sim 0 (live sim) transactions
        AND pt.status = 'EXECUTED' -- that are executed (should always be the case except for bad dev data)
        AND po.status IN ('LIVE','LEGACY','CORRECTED') -- only operations in status relevant to the execution
