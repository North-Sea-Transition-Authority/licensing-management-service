# Application case event history (CaseEvent)

* Status: proposed
* Deciders: Danny Betts
* Date: 2026-08-06

Technical Story: Application event history — case notes, allocations, case status changes, submissions, etc. shown to regulator users as a chronological list of summary cards, under a "Case events" tab on the application summary screen.

## Context and problem statement

Application screens need an event history: a chronological log of the meaningful things that have happened to
a case (submitted, withdrawn, edited, allocated to a caseworker, status changed, a case note added, a document attached,
a decision recorded). Some events carry extra context (a note body, the allocated user, links to uploaded documents); others
carry nothing beyond who did it and when.

The history must be scoped to the parent application, not to a single versioned `ApplicationDetail`, so that it
survives across versions of the same case.

The question is how to source and store this data. The codebase already has Hibernate Envers auditing on effectively every
domain entity, so we must decide whether to reuse that or introduce a purpose-built event log.

## Decision drivers

* It is a user-facing, semantic history ("allocated to X", a free-text note), not a technical column-diff history.
* Events must be able to carry heterogeneous extra context (typed per event), including links to uploaded documents.
* Events must scope to the parent application and be orderable by event time.
* The event write must be consistent with the state change that caused it — we never want an event without its change, or a change without its event.
* Prefer patterns that already exist in the codebase over new frameworks (Spring Data JPA, `jsonb` + native Hibernate JSON, thin services returning immutable view objects).
* Avoid vocabulary collisions — "audit" already means Envers throughout this codebase.

## Considered options

* Option 1: Reconstruct the history from Hibernate Envers `_aud` tables.
* Option 2: Dedicated append-only `CaseEvent` table with a typed `jsonb` payload, written synchronously in the same transaction as the change.
* Option 3: Dedicated `CaseEvent` table written asynchronously via a Spring `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` event bus.

## Pros and cons of the options

### Option 1: Reconstruct from Hibernate Envers

Derive the history by reading each entity's `_aud` revisions and inferring what happened from column diffs.

* Good, because the audit data already exists — no new table, no new write path.
* Good, because it is automatically kept in sync with every entity change.
* Bad, because Envers is row/version shaped; it answers "what did this row look like at revision N?", not "what business event occurred?".
* Bad, because non-column events (a case note, "allocated to X") have no natural representation as a column diff.
* Bad, because rendering the history means reverse-engineering intent from diffs across 39 audited entities — fragile and expensive.
* Bad, because it couples the history's correctness to the physical schema; every entity refactor risks breaking it.

### Option 2: Dedicated `CaseEvent` table, synchronous same-transaction write

An append-only `case_events` table. Each event stores the user, item type + id (parent application), event type, event
time, and an optional typed `jsonb` payload. A thin `CaseEventService.record(...)` is called from the relevant service
inside the existing `@Transactional` unit of work. This mirrors the established `LicencePositionCorrection` /
`LicencePositionPayload` pattern (`@JdbcTypeCode(SqlTypes.JSON)` + a sealed interface with `@JsonTypeInfo`/`@JsonSubTypes`).

* Good, because it models the domain directly — one row per meaningful event, queryable and orderable.
* Good, because the typed `jsonb` payload cleanly carries per-event context and mirrors an existing, proven pattern.
* Good, because writing in the same transaction guarantees the event and its change commit or roll back together.
* Good, because it decouples the history from the physical schema of the audited entities.
* Neutral, each producer must remember to call `record(...)` — a discipline, not a framework guarantee.
* Bad, because it is a new write path (though a very thin one).

### Option 3: Dedicated `CaseEvent` table, async event bus

Same table as Option 2, but producers publish a Spring application event and a `@TransactionalEventListener(phase = AFTER_COMMIT)`
persists the `CaseEvent` after the business transaction commits.

* Good, because it decouples producers from the event write.
* Bad, because the codebase has essentially no event infrastructure to build on — `ApplicationEventPublisher` is used in exactly one place (`UserCancelledEvent`) and there is zero `@TransactionalEventListener` usage; this would be a brand-new pattern to introduce and maintain.
* Bad, because an `AFTER_COMMIT` listener runs outside the original transaction: if it throws, the business change is already committed and the event is silently lost — the opposite of the consistency we want from an audit log.
* Bad, because ordering and testing become harder for no benefit at the current scale.

## Decision outcome

Chosen option: **Option 2 — a dedicated append-only `CaseEvent` table with a typed `jsonb` payload, written synchronously
in the same transaction as the change.**

Envers (Option 1) stays as the technical audit trail; it is the wrong shape for a semantic history. The async event bus
(Option 3) buys decoupling we do not need and sacrifices the commit-together guarantee we do. Option 2 models the domain
directly and reuses patterns already established in the codebase.

### Positive consequences

* The history is a first-class, queryable read model, independent of the audited entities' physical schema.
* Event and state change are transactionally consistent.
* The typed payload reuses a proven `jsonb` + sealed-interface Jackson pattern.

### Negative consequences

* Every producer of a state change must call `CaseEventService.record(...)`; missing a call means a missing history entry (mitigated by keeping producers few and well-located, and by integration tests asserting events are recorded).
* A second, deliberate write to the DB per recorded event (negligible cost).

### Naming and Envers boundary (important)

* The entity is `CaseEvent` / table `case_events` — **not** `AuditEvent`. This codebase already uses "audit" exclusively for Envers (`AuditRevision`, `AuditRevisionListener`, `audit_revisions`, `_aud` tables, the `audit` package). Introducing `AuditEvent` would collide with that vocabulary.
* `CaseEvent` is **not** `@Audited`. It is append-only and immutable by design, so there is nothing to version. There is no ArchUnit rule requiring all entities to be `@Audited` (verified), so no exemption is needed and no `case_events_aud` table is created.
* Distinct from the existing `EventComment` (`licence.schedule.eventcomments`, table `event_comments`): that is a mutable, `@Audited`, regulator-only annotation on *schedule* reference data (`ScheduleEvent`), not an application. `CaseEvent` does **not** absorb it — no merge, no backfill. The application-level case note is a *new* `CASE_NOTE` payload. Note "event" in the `eventcomments`/`eventreference` packages means *schedule event*, so keep the `caseevent` package and vocabulary distinct.

## Links

* Refines the existing "reconstruct from Envers" approach noted in [investigations/event-reference-data-model.md](../investigations/event-reference-data-model.md).
* Related read-model discussion: [ADR-0007](0007-event-tracker-data-retrieval-and-materialisation.md).

---

## Appendix: Implementation plan

### Package layout

New package `uk.co.nstauthority.licensingmanagementservice.caseevent`, with a `payload` sub-package mirroring
`licence/correction/position/payloads`.

```
caseevent/
  CaseEvent.java                 // @Entity, NOT @Audited
  CaseEventType.java             // enum: source of truth for the event_type column
  CaseEventRepository.java       // Spring Data JPA
  CaseEventService.java          // record(...) + query methods
  CaseEventUser.java             // resolves recording user (user / proxy / system)
  CaseEventView.java             // immutable view object for templates
  payload/
    CaseEventPayload.java        // sealed interface, @JsonTypeInfo/@JsonSubTypes
    CaseNotePayload.java
    CaseAllocationPayload.java
    ApplicationSubmittedPayload.java
    ...                          // one per event type that needs extra context
```

### Phase 1 — Data model

Migration `V107__case_event_data_model.sql` (next free number; latest is V106). No `_aud` table — `CaseEvent` is not
audited. DDL style mirrors `V94` (no schema prefix, UUID PK).

```sql
CREATE TABLE case_events (
    id             UUID PRIMARY KEY,
    item_type      TEXT        NOT NULL,   -- ApplicationType enum name (e.g. SCHEDULE_AMENDMENT_APPLICATION)
    item_id        UUID        NOT NULL,   -- parent *Application id (NOT the *Detail id)
    event_type     TEXT        NOT NULL,   -- CaseEventType enum name (queryable discriminator)
    event_instant  TIMESTAMPTZ NOT NULL,   -- when the event occurred
    user_wua_id    BIGINT,                 -- nullable: system/job events have no user
    proxy_wua_id   BIGINT,                 -- proxy user, mirroring audit_revisions
    is_system      BOOLEAN     NOT NULL DEFAULT FALSE,
    payload        JSONB                   -- nullable: many events carry no extra context
);

CREATE INDEX case_events_item_idx ON case_events (item_type, item_id, event_instant);
```

Notes:
* `item_id` is UUID — every application id is UUID (`LicenceApplication.getId()` mandates it).
* `event_type` is a real column (not only Jackson's embedded `type`) so the log is queryable in SQL and self-describing.
* Composite index `(item_type, item_id, event_instant)` matches the primary query: "events for this application, ordered by time".

### Phase 2 — Entity, enum, repository

* `CaseEvent` entity: `@Entity`, `@Table(name = "case_events")`, `@GeneratedValue(strategy = GenerationType.UUID)` for the id, `@JdbcTypeCode(SqlTypes.JSON) private CaseEventPayload payload;`. **No `@Audited`.**
* `CaseEventType` enum: one value per event (e.g. `APPLICATION_SUBMITTED`, `APPLICATION_WITHDRAWN`, `APPLICATION_UPDATED`, `CASE_ALLOCATED`, `CASE_STATUS_CHANGED`, `CASE_NOTE_ADDED`, `DECISION_RECORDED`). Consider having each value name (or map to) its expected payload subtype.
* `CaseEventRepository extends CrudRepository<CaseEvent, UUID>` with `List<CaseEvent> findByItemTypeAndItemIdOrderByEventInstantDesc(String itemType, UUID itemId)`.

### Phase 3 — Payload model (mirror `LicencePositionPayload`)

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CaseNotePayload.class,       name = CaseEventPayload.CASE_NOTE),
    @JsonSubTypes.Type(value = CaseAllocationPayload.class,  name = CaseEventPayload.CASE_ALLOCATION),
    // ...
})
public sealed interface CaseEventPayload
    permits CaseNotePayload, CaseAllocationPayload, ApplicationSubmittedPayload /*...*/ {

  String CASE_NOTE = "case-note";
  String CASE_ALLOCATION = "case-allocation";
  // ...

  String type();
}
```

Payload conventions:
* **Document links are `List<UUID>`** referencing `UploadedFile` ids (files are UUID-keyed via the Fivium file-upload library, soft-referenced by `(usageId, usageType, documentType)`). Store ids only — never denormalise filenames, which can change or be deleted. Resolve to current metadata at read time via `FileService.findAll(Collection<UUID>)` (see `ApplicationFileService` for the existing usage), handling "file no longer exists" gracefully.
* Store only what the card needs to *display*. Anything you need to filter/join/aggregate on at the DB level should be a column, not JSON.
* Each subtype carries the additional information for its event, which is what the card body renders (see Phase 7). Because the interface is `sealed`, the view layer can `switch` over it and the compiler enforces a branch per subtype.

**JSON forward-compatibility (must-verify):** old rows must survive payload field additions, so deserialization must be
lenient (`FAIL_ON_UNKNOWN_PROPERTIES = false`). There is no custom `ObjectMapper` bean; Hibernate's `SqlTypes.JSON` mapping
uses its *own* internal mapper, independent of the Spring bean. **Add an integration test that adds a field to a payload
and confirms an older-shaped row still deserializes.** If the Hibernate mapper is not lenient by default, configure a
`FormatMapper` accordingly rather than assuming the Spring `ObjectMapper` config applies.

### Phase 4 — User resolution

`CaseEventUser` resolves the recording user at record time, reusing the concept already in `AuditRevisionListener`:

* Normal case: current `ServiceUserDetail` → `user_wua_id` (+ `proxy_wua_id` if a proxy user is acting).
* System/job case (ShedLock-driven transitions, EPMQ-driven events like the existing `UserCancelledEvent`, bootstrap): no user in the `SecurityContext` → `is_system = true`, `user_wua_id = null`. Reuse `AuditRevisionUtil.getFallbackAuditUser()` semantics so behaviour matches Envers.

**Display rule (decided):** when a proxy user created the event (`proxy_wua_id` is set), the card shows the **proxy user**, not the underlying real user. When no proxy is present it shows `user_wua_id`. Both ids are still stored on every row for a complete audit trail (matching Envers), but the on-screen "who" is the proxy whenever one exists. `getCaseEventViews(...)` / `CaseEventView` therefore resolve the display name from `proxy_wua_id` when set, falling back to `user_wua_id`.

### Phase 5 — Service (synchronous, same transaction)

```java
@Service
public class CaseEventService {
  // record() is called from within the caller's existing @Transactional unit — no new transaction.
  public CaseEvent record(CaseEventType type, LicenceApplication application, @Nullable CaseEventPayload payload) { ... }

  public List<CaseEventView> getCaseEventViews(LicenceApplication application) { ... }
}
```

* `record(...)` derives `item_type` from `application.getApplicationType()`, `item_id` from `application.getId()`, user from `CaseEventUser`, and `event_instant` from the injected `Clock` (consistent with the rest of the codebase's `Clock` usage for testability).
* `getCaseEventViews(...)` maps entities to immutable `CaseEventView` objects — one per event, newest-first — each carrying the data to render one summary card (Phase 7): heading, display user (proxy-preferred per Phase 4), formatted datetime, the payload's detail rows, and resolved file views. Resolve user names (via the person/user lookup already used for `authorWuaId` display elsewhere) and document metadata in bulk. Keep controllers thin — the view assembly lives here (consistent with the summary-page pattern in the codebase).
* **Watch for N+1** when resolving user names and file metadata across many events — resolve in bulk. (See `investigations/timeline-n-plus-one-plan.md` for prior art on this exact concern.)

Bulk resolution — gather every wua id and every file id across all events first, resolve each with a **single** call, then
map events against the pre-built lookups (no per-event queries):

```java
public List<CaseEventView> getCaseEventViews(LicenceApplication application) {
  var events = caseEventRepository.findByItemTypeAndItemIdOrderByEventInstantDesc(
      application.getApplicationType().name(), application.getId());

  // 1. Every display user (user + proxy), resolved in ONE Energy Portal call.
  var wuaIds = events.stream()
      .flatMap(e -> Stream.of(e.getUserWuaId(), e.getProxyWuaId()))
      .filter(Objects::nonNull)
      .distinct()
      .map(WebUserAccountId::from)
      .toList();
  Map<Long, String> namesByWuaId = energyPortalUserService.findByWuaIds(wuaIds, RESOLVE_CASE_EVENT_USERS_PURPOSE)
      .stream()
      .collect(StreamUtil.toLinkedHashMap(EnergyPortalUserJson::webUserAccountId, EnergyPortalUserJson::displayName));

  // 2. Every referenced file across all payloads, resolved in ONE file-service call.
  var fileIds = events.stream()
      .flatMap(e -> fileIdsOf(e.getPayload()).stream())   // sealed switch over the payload subtype
      .distinct()
      .toList();
  Map<UUID, UploadedFile> filesById = fileService.findAll(fileIds).stream()
      .collect(Collectors.toMap(UploadedFile::getId, Function.identity()));

  // 3. Map each event against the pre-resolved lookups — no queries in this loop.
  return events.stream()
      .map(event -> toView(event, namesByWuaId, filesById))
      .toList();
}
```

* Display user per Phase 4: `toView` picks `proxy_wua_id` from `namesByWuaId` when set, else `user_wua_id`; `is_system` events show a system label.
* `fileIdsOf(CaseEventPayload)` is the sealed `switch` that collects document ids from whichever subtype is present. A file id absent from `filesById` (deleted/unavailable) renders as an unavailable-file row rather than failing.

### Phase 6 — Producers

Call `caseEventService.record(...)` from the services that already perform the state changes, inside their existing
transactions. There is no fixed v1 producer set — each producer is added alongside the feature that emits it. Candidate
producers:

* Application submitted / withdrawn / updated (the `*ApplicationDetail` status transitions).
* Case allocation (steward assignment — `schedule_work_programme_applications.steward_wua_id`).
* Case status change.
* Decision recorded (`RecordOfDecision`).
* Case note added.

### Phase 7 — Presentation (summary cards)

Each case event renders as **its own summary card**, listed newest-first — one card per event. Use the existing
summary-card pattern (`lms/summary/_summaryDetails.ftl` driven by `SummaryItem` → `SummaryCard`, backed by the FDS
`fdsCard` component), **not** the `fdsTimeline` component.

**Placement — a "Case events" tab on the application summary screen.** Add a `CASE_EVENTS` value to the existing
`OverviewTab` enum (`licence.application.caseprocessing`) and render it through the existing `caseProcessingTabsWithContent`
macro (FDS `fdsBackendTabs`) already used by the overview screens (`ScheduleWorkProgrammeApplicationOverviewController`,
`LicenceContinuationApplicationOverviewController`). These are **backend tabs** — each tab is a separate server request
via `?tab=<anchor>` — so the overview controller builds the `CaseEventView` list only when the case-events tab is
selected (lazy; no cost when other tabs are shown).

**Regulator-only (decided).** The tab is visible to **regulator users only**, enforced in two places:
* Presentation: add the `CASE_EVENTS` tab to the tab list only when `teamQueryService.userIsInRegulatorTeam(user.wuaId())` — the same regulator gate `EventComment` uses.
* Security: the controller must also refuse to serve case-events content when `tab=case-events` is requested by a non-regulator (return not-found/forbidden). Do not rely on hiding the tab alone.

* **Card header:** the event caption (from `CaseEventType`) plus the "who / when" — the display user (proxy-preferred per Phase 4) and the formatted `event_instant`.
* **Card body — additional info from the JSON subtype:** each `CaseEventPayload` subtype supplies the rows shown on its card (e.g. `CaseAllocationPayload` → "Allocated to"; a status-change payload → "From" / "To"; `CaseNotePayload` → the note text). Build these via a sealed `switch` over the payload so the compiler forces a branch per subtype. Detail rows map naturally onto the existing `SIMPLE_SUMMARY` card type (key/value rows).
* **Documents:** document ids in the payload render as attached-file links via `fdsCard.cardFilesList` / `cardFilesListItem` (resolved to current file metadata in Phase 5). When a card has both detail rows and files, this mirrors the existing `FILES_AND_DETAILS_SUMMARY` card type — reuse it rather than inventing new markup.
* Routing: the tab content is served by the existing overview controller at its current URL with `?tab=case-events` (the macro builds `controllerUrl?tab=<anchor>`), so no separate controller is introduced. The overview controllers already carry the application access/status annotations (`@InvokingUserCanAccessScheduleApplication`, `@ScheduleAmendmentApplicationHasStatus`); the only new rule is the regulator gate above.

### Phase 8 — Tests

* Unit tests for `CaseEventService.record(...)` (user resolution incl. proxy + system) and `getCaseEventViews(...)` mapping, including a sealed `switch` branch per payload subtype producing the expected card rows.
* Integration test: record events in a transaction, assert rollback removes them (proves same-transaction semantics).
* Integration test: `jsonb` round-trip per payload subtype **and** the forward-compat test from Phase 3.
* Integration test: producers actually record the expected event on each state transition.
* Controller test: the `Case events` tab is present for a regulator user and absent for a non-regulator; requesting `?tab=case-events` as a non-regulator is refused (not-found/forbidden), not silently served.
* Naming per convention: `methodName_whenCondition_assertExpectedBehaviour`.
