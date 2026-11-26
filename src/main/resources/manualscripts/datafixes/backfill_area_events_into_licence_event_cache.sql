-- Adds the retention, development and pre-producing area events of each licence's ACTIVE schedule version to the cross
-- licence event tracker cache, for licences whose cache was last refreshed before area events were cached. The service
-- adds them itself the next time a licence's schedule is applied or migrated.
-- Writes the rows the service writes: one per area event with a date, with the same summary text in activity_type.
-- Safe to re-run: a licence that already has area event rows in the cache is skipped.
WITH area_events AS (
  SELECT
    ls.licence_id,
    l.licence_reference,
    ae.event_mnem,
    ae.event_date,
    ae.area_name,
    CASE
      WHEN trim(coalesce(ae.area_version, '')) IN ('', '1') THEN ae.area_short_name
      ELSE ae.area_short_name || 'v' || trim(ae.area_version)
    END AS short_name,
    CASE upper(trim(coalesce(ae.area_type, '')))
      WHEN 'RETENTION' THEN 'Retention area'
      WHEN 'DEVELOPMENT' THEN 'Development area'
      WHEN 'PROSPECTIVE' THEN 'Pre-producing area'
      ELSE CASE WHEN ae.event_mnem = 'PA' THEN 'Pre-producing area' ELSE 'Retention or development area' END
    END AS area_type,
    lower(replace(trim(coalesce(ae.subtype, '')), '_', ' ')) AS subtype
  FROM lms.licence_schedule_area_events ae
  JOIN lms.licence_schedule_details lsd ON lsd.id = ae.licence_schedule_detail_id
  JOIN lms.licence_schedules ls ON ls.id = lsd.licence_schedule_id
  JOIN lms.licences l ON l.id = ls.licence_id
  WHERE lsd.status = 'ACTIVE'
    AND ae.event_date IS NOT NULL
    AND NOT EXISTS (
      SELECT 1
      FROM lms.licence_event_cache lec
      WHERE lec.licence_id = ls.licence_id
        AND lec.event_type = 'AREA'
    )
),
headed_area_events AS (
  SELECT
    area_events.*,
    CASE
      WHEN trim(coalesce(area_name, '')) = ''
        THEN CASE WHEN event_mnem = 'PA' THEN 'Pre-producing area' ELSE 'Retention or development area' END
      WHEN trim(coalesce(short_name, '')) = '' THEN area_name
      ELSE area_name || ' (' || short_name || ')'
    END AS heading
  FROM area_events
)
INSERT INTO lms.licence_event_cache (id, licence_id, licence_reference, event_type, activity_type, event_date)
SELECT
  gen_random_uuid(),
  licence_id,
  licence_reference,
  'AREA',
  concat_ws(
    ' - ',
    heading,
    nullif(area_type, heading),
    nullif(upper(left(subtype, 1)) || substr(subtype, 2), '')
  ),
  event_date
FROM headed_area_events;
