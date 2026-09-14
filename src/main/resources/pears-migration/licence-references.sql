-- Every licence the sweep should compare: the licences PEARS holds data points for,
-- together with the licences its operations name. Neither side is a superset of the
-- other -- PEARS holds data points for transactions that have no operation naming a
-- licence, and there are operation-derived positions with no data point in any
-- simulation -- and a licence missing from the worklist is a licence silently not
-- compared, so both are included.
--
-- Deliberately does not read PED_SIMULATION_TRANSACTIONS. That table decides live
-- series membership but is not always granted to a migration user, and the worklist
-- is only deciding which licences to look at: a licence that turns out to hold no
-- live positions on either side compares equal and costs one query.
SELECT licence_type, licence_no FROM (
  SELECT DISTINCT
    dp.licence_type
  , dp.licence_no
  FROM pedmgr.ped_data_points dp
  WHERE dp.ped_sim_id = 0
  AND (dp.flag_list IS NULL OR dp.flag_list NOT LIKE '%FIELD_GHOST%')
  UNION
  SELECT DISTINCT
    xpo.licence_type
  , xpo.licence_no
  FROM pedmgr.xview_ped_operations xpo
  JOIN pedmgr.ped_transactions pt ON pt.id = xpo.ped_tran_id
  WHERE pt.status = 'EXECUTED'
  AND xpo.status IN ('LIVE','LEGACY','CORRECTED')
  AND xpo.licence_type IS NOT NULL
  AND xpo.licence_no IS NOT NULL
)
ORDER BY licence_type, licence_no
