# Investigation: EventReference Data Model

## Problem

`EventReference` is an intermediate entity that acts as a stable identity anchor for all
schedule entities (Term, Phase, Rate, WorkProgrammeActivity, OtherScheduleEvent, Expiry).
Each child entity holds a FK to `event_references`, and `EventComment` /
`WorkProgrammeActivityStatus` attach to that same anchor so their history survives when
`LicenceScheduleDetail` is duplicated for a new draft.

The model has two structural weaknesses.

**1. `eventType` is a redundant, fragile field.**
The type of event is already implied by which entity holds the FK to `event_references`.
`eventType` exists only because there is no back-reference from `EventReference` to its
owner. If the two diverge (e.g. a migration populates the wrong type), the inconsistency
is silent and only surfaces at runtime in `EventReferenceService.getEventReferenceEventCaption()`.

**2. No relational back-reference forces code-level polymorphic dispatch.**
Given an `EventReference`, you cannot navigate to the owning entity through a JOIN — you
must inspect `eventType` and branch to the correct table in service code. This is an
anti-pattern in a relational model and makes `EventReferenceService` a growing maintenance
surface as new event types are added.

Current table relationships (simplified):

```
event_references (id, licence_schedule_id, event_type)
    ↑ event_reference_id FK from: licence_schedule_terms
    ↑ event_reference_id FK from: licence_schedule_phases
    ↑ event_reference_id FK from: work_programme_activities
    ↑ event_reference_id FK from: licence_schedule_rates
    ↑ event_reference_id FK from: other_schedule_events
    ↑ event_reference_id FK from: licence_schedule_expiry_dates
    ↑ event_reference_id FK from: event_comments
    ↑ event_reference_id FK from: work_programme_activity_statuses
```

## Recommendation: JPA Joined-Table Inheritance

Rename `event_references` to `schedule_events` and make it the base entity in a JPA
joined-table inheritance hierarchy. Each child entity extends `ScheduleEvent` directly —
its table's `id` column is both its PK and a FK to `schedule_events`. The `eventType`
field is replaced by a JPA `@DiscriminatorColumn`.

Target relationships:

```
schedule_events (id, licence_schedule_id, dtype)   ← renamed from event_references
    ↑ id FK from: licence_schedule_terms
    ↑ id FK from: licence_schedule_phases
    ↑ id FK from: work_programme_activities
    ↑ id FK from: licence_schedule_rates
    ↑ id FK from: other_schedule_events
    ↑ id FK from: licence_schedule_expiry_dates

event_comments.event_reference_id → schedule_events.id   (column name unchanged)
work_programme_activity_statuses.event_reference_id → schedule_events.id
```

Benefits:
- `eventType` is derived from the real type automatically — can never diverge.
- `EventReferenceService.getEventReferenceEventCaption()` dispatch is replaced by a
  polymorphic method on `ScheduleEvent`.
- Creating a Term writes both `schedule_events` and `licence_schedule_terms` in a single
  `termRepository.save(term)` call — the separate `createEventReference()` step is gone.
- `instanceof` or the OOP hierarchy replaces runtime type-switching.

## Migration Plan

### The ID remapping problem

JPA joined-table inheritance requires child and parent to share the same PK. Currently
they do not:

```
event_references:        id = UUID_A
licence_schedule_terms:  id = UUID_B,  event_reference_id = UUID_A
```

The least disruptive resolution is to keep the child entity IDs unchanged and insert new
`schedule_events` rows keyed by child IDs, then re-point `event_comments` and
`work_programme_activity_statuses` to the child IDs, and delete the old
`event_references` rows.

### Phase 1 — Flyway schema migration

```sql
-- 1. Rename table and column
ALTER TABLE event_references RENAME TO schedule_events;
ALTER TABLE event_references_aud RENAME TO schedule_events_aud;
ALTER TABLE schedule_events RENAME COLUMN event_type TO dtype;
ALTER TABLE schedule_events_aud RENAME COLUMN event_type TO dtype;

-- 2. Insert a schedule_events row for each child entity using the child's own id.
--    Pull licence_schedule_id from the EventReference row it currently points to.

INSERT INTO schedule_events (id, licence_schedule_id, dtype)
SELECT t.id, er.licence_schedule_id, 'TERM'
FROM licence_schedule_terms t
JOIN schedule_events er ON er.id = t.event_reference_id;

INSERT INTO schedule_events (id, licence_schedule_id, dtype)
SELECT p.id, er.licence_schedule_id, 'PHASE'
FROM licence_schedule_phases p
JOIN schedule_events er ON er.id = p.event_reference_id;

INSERT INTO schedule_events (id, licence_schedule_id, dtype)
SELECT a.id, er.licence_schedule_id, 'WORK_PROGRAMME_ACTIVITY'
FROM work_programme_activities a
JOIN schedule_events er ON er.id = a.event_reference_id;

INSERT INTO schedule_events (id, licence_schedule_id, dtype)
SELECT r.id, er.licence_schedule_id, 'RATE'
FROM licence_schedule_rates r
JOIN schedule_events er ON er.id = r.event_reference_id;

INSERT INTO schedule_events (id, licence_schedule_id, dtype)
SELECT o.id, er.licence_schedule_id, 'OTHER'
FROM other_schedule_events o
JOIN schedule_events er ON er.id = o.event_reference_id;

INSERT INTO schedule_events (id, licence_schedule_id, dtype)
SELECT e.id, er.licence_schedule_id, 'EXPIRY'
FROM licence_schedule_expiry_dates e
JOIN schedule_events er ON er.id = e.event_reference_id;

-- 3. Re-point event_comments from old EventReference id → child entity id
UPDATE event_comments ec SET event_reference_id = t.id
FROM licence_schedule_terms t WHERE ec.event_reference_id = t.event_reference_id;

UPDATE event_comments ec SET event_reference_id = p.id
FROM licence_schedule_phases p WHERE ec.event_reference_id = p.event_reference_id;

UPDATE event_comments ec SET event_reference_id = a.id
FROM work_programme_activities a WHERE ec.event_reference_id = a.event_reference_id;

UPDATE event_comments ec SET event_reference_id = r.id
FROM licence_schedule_rates r WHERE ec.event_reference_id = r.event_reference_id;

UPDATE event_comments ec SET event_reference_id = o.id
FROM other_schedule_events o WHERE ec.event_reference_id = o.event_reference_id;

UPDATE event_comments ec SET event_reference_id = e.id
FROM licence_schedule_expiry_dates e WHERE ec.event_reference_id = e.event_reference_id;

-- Repeat the same UPDATE block for work_programme_activity_statuses

-- 4. Delete the original EventReference rows (now orphaned)
DELETE FROM schedule_events
WHERE id NOT IN (
    SELECT id FROM licence_schedule_terms
    UNION ALL SELECT id FROM licence_schedule_phases
    UNION ALL SELECT id FROM work_programme_activities
    UNION ALL SELECT id FROM licence_schedule_rates
    UNION ALL SELECT id FROM other_schedule_events
    UNION ALL SELECT id FROM licence_schedule_expiry_dates
);

-- 5. Add FK from each child table to schedule_events and drop the old column
ALTER TABLE licence_schedule_terms
    ADD CONSTRAINT fk_terms_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE licence_schedule_phases
    ADD CONSTRAINT fk_phases_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE work_programme_activities
    ADD CONSTRAINT fk_activities_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE licence_schedule_rates
    ADD CONSTRAINT fk_rates_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE other_schedule_events
    ADD CONSTRAINT fk_other_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

ALTER TABLE licence_schedule_expiry_dates
    ADD CONSTRAINT fk_expiry_schedule_event FOREIGN KEY (id) REFERENCES schedule_events(id),
    DROP COLUMN event_reference_id;

-- 6. Drop event_reference_id from audit tables
ALTER TABLE licence_schedule_terms_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE licence_schedule_phases_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE work_programme_activities_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE licence_schedule_rates_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE other_schedule_events_aud DROP COLUMN IF EXISTS event_reference_id;
ALTER TABLE licence_schedule_expiry_dates_aud DROP COLUMN IF EXISTS event_reference_id;
```

### Phase 2 — Java / JPA changes

**Base entity** (rename `EventReference` → `ScheduleEvent`):

```java
@Entity
@Table(name = "schedule_events")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Audited
public abstract class ScheduleEvent {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "licence_schedule_id", nullable = false)
    private LicenceSchedule licenceSchedule;

    // eventType removed — dtype discriminator replaces it
}
```

**Each child entity** extends `ScheduleEvent` and drops the `eventReference` field:

```java
@Entity
@Table(name = "licence_schedule_terms")
@DiscriminatorValue("TERM")
@Audited
public class LicenceScheduleTerm extends ScheduleEvent {
    // id and licenceSchedule inherited — no @Id annotation here
    // eventReference field deleted

    @ManyToOne
    @JoinColumn(name = "licence_schedule_detail_id")
    private LicenceScheduleDetail licenceScheduleDetail;

    // all other fields unchanged
}
```

**`EventComment` and `WorkProgrammeActivityStatus`** — field type changes only:

```java
@ManyToOne
@JoinColumn(name = "event_reference_id")
private ScheduleEvent scheduleEvent;  // was: EventReference eventReference
```

### Phase 3 — Service simplifications

`EventReferenceService` is eliminated. Its two responsibilities split cleanly:

| Old | New |
|---|---|
| `createEventReference(schedule, TERM)` | Implicit — `termRepository.save(term)` writes both tables |
| `getEventReferenceEventCaption(ref)` | Polymorphic method on `ScheduleEvent` (or a visitor) |

Creation sites go from two steps to one:

```java
// Before
EventReference ref = eventReferenceService.createEventReference(schedule, ScheduleEventType.TERM);
term.setEventReference(ref);
termRepository.save(term);

// After — one save writes schedule_events and licence_schedule_terms
term.setLicenceSchedule(schedule);
termRepository.save(term);
```

## What Does Not Change

- `ScheduleEventType` enum — its values become the `@DiscriminatorValue` strings.
- `EventComment` and `WorkProgrammeActivityStatus` table structure (Java field rename only).
- The duplication pattern — `ScheduleEvent` continues to implement `NotDuplicationSource`.
- All application business logic, validators, controllers, and templates.

## Risk Areas

**Audit history.** Removing `event_reference_id` from child audit tables means historical
Envers revisions no longer record which `EventReference` a row pointed to. Assess whether
this gap is acceptable before proceeding.

**Null `event_reference_id` rows.** If any child entity has a null `event_reference_id`,
the `INSERT INTO schedule_events ... JOIN` steps in Phase 1 will silently omit those rows.
The FK constraint added in step 5 will then fail at migration time. Run the following
query across all six child tables before executing the migration:

```sql
SELECT 'terms' AS src, COUNT(*) FROM licence_schedule_terms WHERE event_reference_id IS NULL
UNION ALL
SELECT 'phases', COUNT(*) FROM licence_schedule_phases WHERE event_reference_id IS NULL
UNION ALL
SELECT 'activities', COUNT(*) FROM work_programme_activities WHERE event_reference_id IS NULL
UNION ALL
SELECT 'rates', COUNT(*) FROM licence_schedule_rates WHERE event_reference_id IS NULL
UNION ALL
SELECT 'other', COUNT(*) FROM other_schedule_events WHERE event_reference_id IS NULL
UNION ALL
SELECT 'expiry', COUNT(*) FROM licence_schedule_expiry_dates WHERE event_reference_id IS NULL;
```

**`RELATIVE_DATE` anchor references.** If `WorkProgrammeActivity` or `OtherScheduleEvent`
hold a second `event_reference_id`-style column pointing to another entity's
`EventReference` as a relative-date anchor, those FKs will also need re-pointing in step 3.
Verify whether such a column exists before running the migration.
