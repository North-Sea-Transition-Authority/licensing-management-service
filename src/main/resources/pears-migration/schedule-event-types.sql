-- The schedule event types PEARS knows. Users can add types through the event type
-- screen, so live can hold types other environments do not, and the migration reads
-- this at run time rather than relying on a fixed list.
SELECT
  mnem
, short_title
, display_seq
FROM pedmgr.ped_template_event_types
ORDER BY display_seq, mnem
