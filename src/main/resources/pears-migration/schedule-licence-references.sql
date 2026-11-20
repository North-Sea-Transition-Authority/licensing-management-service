-- Every licence PEARS holds a licence schedule for: one with at least one saved
-- schedule version, current or not. A licence whose versions are all end-dated is
-- still listed, since the migration takes its latest version as the active one.
--
-- Licences with no licence type are left out: they are leftovers PEARS cannot
-- show, and no LMS licence matches them.
SELECT DISTINCT
  plm.licence_type
, plm.licence_no
FROM pedmgr.ped_licence_master plm
JOIN pedmgr.ped_licence_schedule_details plsd ON plsd.ped_lm_id = plm.id
WHERE plm.licence_type IS NOT NULL
AND plm.licence_no IS NOT NULL
ORDER BY plm.licence_type, plm.licence_no
