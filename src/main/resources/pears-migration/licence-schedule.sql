-- Every saved version of one licence's schedule, oldest first, each with its
-- events in the order the version holds them. One query, so the versions and their
-- events are read as of the same moment even if the schedule is saved meanwhile.
--
-- PEARS end-dates the current version and inserts a new one on every save, so the
-- version with no end date is the current one. The events are outer-applied, so a
-- version holding no events still comes back, as one row with no event.
--
-- Read from the schedule XML rather than XVIEW_PED_LIC_SCHEDULE_EVENTS: the xview
-- has no event position, which the migration records against each event it
-- creates, and reading the base table needs no grant on the xview.
--
-- event_index is the event's position in EVENT_LIST, counting from 1. The padding
-- rows the schedule screen adds (DUMMY_EVENT = 'true') are returned flagged rather
-- than filtered here, so a version holding only padding still comes back, and an
-- index always points at the same element of the stored XML.
--
-- ORIGIN_DATE is the schedule's start date. It is missing on the first version of
-- many licences (saved before the licence was raised), so a missing or unreadable
-- one comes back null rather than failing the query, as does an unreadable
-- EVENT_DATE. Every other value is returned as PEARS holds it, untrimmed and
-- unparsed, for the mapper to judge.
SELECT
  plsd.id plsd_id
, TO_CHAR(plsd.start_datetime, 'YYYY-MM-DD"T"HH24:MI:SS') start_datetime
, TO_CHAR(plsd.end_datetime, 'YYYY-MM-DD"T"HH24:MI:SS') end_datetime
, TO_CHAR(
    TO_DATE(
      XMLCAST(XMLQUERY('/SCHEDULE/ORIGIN_DATE/text()' PASSING plsd.xml_data RETURNING CONTENT) AS VARCHAR2(30))
      DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD'
    ),
    'YYYY-MM-DD'
  ) origin_date
, e.event_index
, e.dummy_event
, e.event_mnem
, e.term_no
, e.phase
, e.years
, e.months
, e.days
, TO_CHAR(TO_DATE(e.event_date DEFAULT NULL ON CONVERSION ERROR, 'YYYY-MM-DD'), 'YYYY-MM-DD') event_date
, e.recurring
, e.recurrances
, e.recur_end_date
, e.rate
, e.percentage
, e.comments
, e.event_subtype
, e.event_id
, e.area_name
, e.area_short_name
, e.area_type
, e.area_version
FROM pedmgr.ped_licence_schedule_details plsd
JOIN pedmgr.ped_licence_master plm ON plm.id = plsd.ped_lm_id
OUTER APPLY XMLTABLE('/SCHEDULE/EVENT_LIST/EVENT'
    PASSING plsd.xml_data
    COLUMNS
      event_index FOR ORDINALITY
    , dummy_event VARCHAR2(4000) PATH 'DUMMY_EVENT'
    , event_mnem VARCHAR2(4000) PATH 'EVENT_MNEM'
    , term_no VARCHAR2(4000) PATH 'TERM_NO'
    , phase VARCHAR2(4000) PATH 'PHASE'
    , years VARCHAR2(4000) PATH 'YEARS'
    , months VARCHAR2(4000) PATH 'MONTHS'
    , days VARCHAR2(4000) PATH 'DAYS'
    , event_date VARCHAR2(4000) PATH 'EVENT_DATE'
    , recurring VARCHAR2(4000) PATH 'RECURRING'
    , recurrances VARCHAR2(4000) PATH 'RECURRANCES'
    , recur_end_date VARCHAR2(4000) PATH 'RECUR_END_DATE'
    , rate VARCHAR2(4000) PATH 'RATE'
    , percentage VARCHAR2(4000) PATH 'PERCENTAGE'
    , comments VARCHAR2(4000) PATH 'COMMENTS'
    , event_subtype VARCHAR2(4000) PATH 'EVENT_SUBTYPE'
    , event_id VARCHAR2(4000) PATH 'EVENT_ID'
    , area_name VARCHAR2(4000) PATH 'RETENTION_AREA/NAME'
    , area_short_name VARCHAR2(4000) PATH 'RETENTION_AREA/SHORT_NAME'
    , area_type VARCHAR2(4000) PATH 'RETENTION_AREA/TYPE'
    , area_version VARCHAR2(4000) PATH 'RETENTION_AREA/VERSION'
  ) e
WHERE plm.licence_type = ? AND plm.licence_no = ?
ORDER BY plsd.start_datetime, plsd.id, e.event_index
