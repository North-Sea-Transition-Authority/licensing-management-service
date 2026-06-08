# Event Tracker data retrieval and materialisation

* Status: proposed
* Date: 2026-05-11

## Context and problem statement

The Event Tracker needs to show upcoming and historical licence events on a single screen without pagination.

The table will include rows for licence schedule terms and phases, work programme activities, and other schedule events.
It also needs enough projected data to support filtering, sorting, request links, and different regulator and industry views.

Retrieving and assembling this data through many repository calls could create performance issues, especially because the
table needs dynamic sorting and a large default date range.

## Decision drivers

* Needs to be performant enough for a large non-paginated table.
* Needs to support filtering and sorting from the data source.
* Avoid jOOQ usage where Spring Data JPA can support the requirement.
* Avoid extra materialisation or caching complexity until the investigation proves it is required.
* Keep organisation lookups efficient by resolving licensee data in bulk.

## Considered options

* Option 1: SQL view mapped through Hibernate/JPA.
* Option 2: Direct SQL query called from Java using jOOQ.
* Option 3: Materialised view refreshed by a scheduled job.
* Option 4: Multiple repository calls assembled with Java streams.
* Option 5: Cache table maintained from source table changes.
* Option 5.1: Cache table with foreign keys to selected source tables.

## Pros and cons of the options

### Option 1: SQL view mapped through Hibernate/JPA

Create a database view for the Event Tracker query and map it to a Hibernate/JPA entity queried through a Spring Data
repository.

* Good, because the query returns the tracker data in one database call.
* Good, because the current query is fast on the limited UAT data used during the spike.
* Good, because filtering and sorting can operate over the projected event fields.
* Good, because it avoids new jOOQ usage.
* Neutral, responsible organisation ids need transforming from a CSV projection into a Java list.
* Bad, because it introduces a SQL view which must be maintained alongside Java based functionality.
* Bad, because it is harder to test and would require comprehensive integration test coverage.

### Option 2: Direct SQL query called from Java using jOOQ

Run the same SQL query directly from the application using jOOQ.

* Good, because it keeps the single-query approach.
* Good, because filtering and sorting can be woven into the query.
* Good, because it allows one bulk EPA call to resolve licensee data after fetching tracker rows.
* Bad, because the Digital team preference is to avoid jOOQ where possible due to readability and maintainability concerns.
* Bad, because it is harder to test and would require comprehensive integration test coverage.

### Option 3: Materialised view refreshed by a scheduled job

Store the query result in a materialised view and refresh it on a schedule.

* Good, because reads could be faster if the normal view query becomes too expensive.
* Bad, because scheduled refresh introduces stale data and operational complexity.
* Bad, unnecessary because the current query is already fast enough on limited UAT data.
* Bad, because it introduces a SQL view which must be maintained alongside Java based functionality.

### Option 4: Multiple repository calls assembled with Java streams

Query continuation applications, event request applications, terms, phases, work programme activities, scheduled events,
and related data separately, then assemble the Event Tracker DTOs in Java.

* Good, because it follows the usual repository and service pattern.
* Good, because some of the filtering and sorting could be done in the repository.
* Good, integration tests would be simple, and comprehensive unit test coverage could be achieved.
* Bad, because it needs multiple repository calls and JPQL queries.
* Bad, because it introduces significant complexity by requiring filtering, sorting, and data assembly within the Java layer.

### Option 5: Flat cache table maintained from source table changes

Maintain an Event Tracker flat cache table whenever source data is inserted or updated using transactional event listeners.
May need to bootstrap/prefill the data from the source tables. Though, if the migration of the source tables occur in Java,
then the cache table could be populated in the same transaction using the event listeners.

* Good, because reads could be simple and fast. One query to fetch all data.
* Good, because it would be easy to add test coverage.
* Bad, because it potentially adds some write-path complexity across several source tables.
* Bad, because it potentially introduces consistency risk.

### Option 5.1: Flat cache table with foreign keys to selected source tables

Use the same flat cache table approach as Option 5, but store foreign keys to selected source tables instead of copying
all related data into the cache table. For example, the cache table could store the licence id, continuation application
id, and event request application id, with licensee and steward data resolved separately when the tracker data is read.

* Good, because it reduces the risk of inconsistency for data that can be easily changed by users, such as licensees and stewards.
* Good, because the cache table can still provide a simple tracker-specific read model.
* Good, because this is the approach used by the digital team implementing the Environmental service.
* Bad, because reads need extra lookups for data that is not stored directly in the cache table.
* Bad, because it still has some write-path complexity.

## Decision outcome

Option 5.1 is chosen as the preferred approach, with Option 4 as the fallback approach if this approach proves to be too
complex to implement.

* We are already exploring this approach in another service being built by the Digital team.
* The write path complexity is mitigated by the use of simple transactional event listeners to populate the cache table.
* The consistency risk is mitigated by only storing foreign keys to the source tables.

## Appendix: Events tracker query

```sql
WITH latest_lcad AS (
  SELECT
    id
  , licence_continuation_application_id
  , status
  FROM (
    SELECT
      id
    , licence_continuation_application_id
    , status
    , submitted_datetime
    , ROW_NUMBER() OVER (PARTITION BY licence_continuation_application_id ORDER BY version_number DESC) AS rn
    FROM lms.licence_continuation_application_details
  ) t
  WHERE rn = 1
    AND submitted_datetime IS NOT NULL
),
latest_swpad AS (
  SELECT
    id
  , schedule_work_programme_application_id
  , status
  FROM (
    SELECT
      id
    , schedule_work_programme_application_id
    , status
    , submitted_datetime
    , ROW_NUMBER() OVER (PARTITION BY schedule_work_programme_application_id ORDER BY version_number DESC) AS rn
    FROM lms.schedule_work_programme_application_details
  ) t
  WHERE rn = 1
    AND submitted_datetime IS NOT NULL
),
base_licence AS (
  SELECT 
    l.id licence_id
  , l.licence_reference
  , lsd.id schedule_detail_id
  , ls.id schedule_id
  , STRING_AGG(lro.responsible_organisation_id::text, ',') licence_responsible_organisations
  , l.round_issued_on
  FROM lms.licences l
  JOIN lms.licence_schedules ls ON ls.licence_id = l.id
  JOIN lms.licence_schedule_details lsd ON ls.id = lsd.licence_schedule_id
  JOIN lms.licence_responsible_organisations lro ON l.id = lro.licence_id
  GROUP by l.id, ls.id, lsd.id
)
SELECT
  'TERM_PHASE' event_type
, bl.licence_id
, bl.licence_reference
, lst.term_type
, lsp.phase_type
, '' wp_category
, COALESCE(lst.end_date, lsp.end_date) event_date
, CASE
    WHEN lser.id IS NOT NULL
      THEN swpad.status
    WHEN lcad.id IS NOT NULL
      THEN lcad.status
    ELSE ''
  END status
, bl.licence_responsible_organisations
, bl.round_issued_on
, swpa.steward_wua_id
FROM base_licence bl
LEFT JOIN lms.licence_schedule_terms lst ON bl.schedule_detail_id = lst.licence_schedule_detail_id
LEFT JOIN lms.licence_schedule_phases lsp ON bl.schedule_detail_id = lsp.licence_schedule_detail_id
LEFT JOIN lms.licence_continuation_applications lca ON bl.schedule_id = lca.licence_schedule_id
LEFT JOIN latest_lcad lcad ON lca.id = lcad.licence_continuation_application_id
LEFT JOIN lms.schedule_work_programme_applications swpa ON bl.schedule_id = swpa.licence_schedule_id
LEFT JOIN latest_swpad swpad ON swpa.id = swpad.schedule_work_programme_application_id
LEFT JOIN lms.licence_schedule_extension_request lser
  ON lser.schedule_work_programme_application_details_id = swpad.id
    AND (lser.term_id = lst.id OR lser.phase_id = lsp.id)
WHERE lst.id IS NOT NULL OR lsp.id IS NOT NULL

UNION ALL

SELECT
  'WP' event_type
, bl.licence_id
, bl.licence_reference
, '' term_type
, '' phase_type
, wsa.category wp_category
, wsa.due_date event_date
, CASE
    WHEN lwpar.id IS NOT NULL
      THEN swpad.status
    ELSE ''
  END status
, bl.licence_responsible_organisations
, bl.round_issued_on
, NULL steward_wua_id
FROM base_licence bl
JOIN lms.work_programme_activities wsa ON bl.schedule_detail_id = wsa.licence_schedule_detail_id
LEFT JOIN lms.schedule_work_programme_applications swpa ON bl.schedule_id = swpa.licence_schedule_id
LEFT JOIN latest_swpad swpad ON swpa.id = swpad.schedule_work_programme_application_id
LEFT JOIN lms.licence_work_programme_amendment_request lwpar
  ON lwpar.schedule_work_programme_application_details_id = swpad.id AND lwpar.work_programme_activity_id = wsa.id
WHERE wsa.due_date IS NOT NULL

UNION ALL

SELECT
  'EVENT' event_type
, bl.licence_id
, bl.licence_reference
, '' term_type
, '' phase_type
, ose.category wp_category
, ose.event_date event_date
, '' status
, bl.licence_responsible_organisations
, bl.round_issued_on
, NULL steward_wua_id	
FROM base_licence bl
JOIN lms.other_schedule_events ose ON bl.schedule_detail_id = ose.licence_schedule_detail_id
WHERE ose.event_date IS NOT NULL

ORDER BY event_date, licence_id, event_type;
```
