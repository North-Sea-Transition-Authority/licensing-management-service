# Investigation: REST / URL Convention Inconsistencies in Controllers

* Author: Danny Betts
* Date: 2026-08-12
* Status: Open — findings recorded for triage; no mass change proposed

## Context

A full audit of every Spring MVC controller under `src/main/java` (110 classes, all
request-mapped handler methods read) was carried out to assess adherence to REST / URL
conventions. The prompt was a specific oddity — `licences/{licenceId}` addressing a single
licence — but the sweep covered verbs, HTTP-method semantics, plurality, path structure and
annotations across the whole surface.

**Calibration.** This is a server-rendered Freemarker MVC application, not a REST API. Strict
REST only meaningfully binds the handful of true JSON `@RestController` endpoints. For the
~100 HTML page controllers, "RESTful URLs" is a *style convention*: verbs in paths
(`/delete`, `/start`), POST-for-everything, and a GET-confirmation + POST-action pattern are
normal and largely unavoidable with HTML forms (which speak only GET and POST). Findings below
are therefore ranked by whether they are a **genuine problem**, a **consistency smell**, or
**expected/non-issue**.

**On the original example.** `/licences/{licenceId}` is in fact the *correct* REST convention
— a collection noun is plural and an individual member is addressed by id beneath it
(`/licences/{id}`, like `/users/{id}`). The real inconsistency is the opposite: the **singular**
`/licence/{id}/...` form used by much of the newer code. See Finding 1.

## Already addressed (not in scope here)

Two categories from the audit have been fixed separately and are **out of scope** for this
document:

1. **Mutating GETs** on the migration controllers (`PearsRefreshController`,
   `IndustryTeamMigrationController`, `CarbonStorageLicenceMigrationController`) → changed to
   `POST` (with a `/migration/**` CSRF exemption, mirroring the logout endpoint, so the
   operator/script trigger still works).

   **Subsequently revised:** these three now accept **both `GET` and `POST`**
   (`@RequestMapping(method = {GET, POST})`). Because `/migration/**` sits behind SAML
   authentication, a logged-in browser is the only practical way to trigger them by hand, and
   that means a `GET`. `POST` is retained as the correct verb for scripted triggers. The
   mutating-GET caveat therefore still applies — browsers and intermediaries may prefetch or
   retry these URLs, so treat each operation as one that must tolerate being invoked more than
   once.
2. **Misannotations** — the migration controllers `@Controller` → `@RestController` (they
   return `ResponseEntity<String>`, no views); `TeamManagementController` and
   `ScopedTeamManagementController` `@RestController` → `@Controller` (they render
   `ModelAndView`).

Everything below is the **remaining** set: consistency smells and softer REST deviations that
were deliberately *not* changed, because they are cosmetic and reshaping live URLs carries real
regression risk (see [Cross-cutting risk](#cross-cutting-risk)).

## Findings

### 1. Singular `licence/…` vs plural `licences/…` — the headline

The same domain object is addressed both ways, and the split even runs **through a single
journey**: the schedule-work-programme and continuation journeys *start* on plural `licences/…`
and flip to singular `licence/…` at the licensee-info step and every subsequent step.

| Convention | Examples |
|---|---|
| **Plural `licences/…`** (REST-correct) | `/licences`, `licences/search`, `licences/start-application`, `licences/{id}/timeline`, `licences/{id}/overview`, `licences/{id}/correction/start`, `licences/schedule-work-programme-application/…` (start), `licences/continuation-application/…` (start) |
| **Singular `licence/…`** (deviation) | entire `/licence/schedule/…` subtree, `licence/{id}/schedule-work-programme-application/…`, `licence/continuation-application/{id}/…`, `licence/{id}/schedule/start` |

**Direct in-feature contradiction:** `SelectScheduleWorkProgrammeApplicationLicenceController`
→ `licences/schedule-work-programme-application/{slug}/licence`, but the very next step
`LicenseeInformationController` →
`licence/{id}/schedule-work-programme-application/{slug}/licensee-information`. Same split
exists for continuation.

*Severity:* consistency smell (highest visibility). Plural is the target; the singular endpoints
are the deviation.

### 2. Plural/singular chaos within the corrections feature

The same underlying position id is reached via three different nouns:

- `positions` (plural) — `CorrectPositionDateController`,
  `LicencePositionCorrectionOrderChangeController`, `ReinstateLicencePositionCorrectionController`,
  `RemoveExecutedLicencePositionCorrectionController`, `UndoLicencePositionCorrectionController`
  (`/licence-corrections/{correctionId}/positions/{licencePositionId}/…`).
- `position` (singular) — all `…change.*` controllers (add-change, administrator, set-equity,
  transfer-equity, partial-surrender) (`/licence-corrections/{correctionId}/position/{…}/…`).
- `added-positions` vs `added-position` vs `position-correction` — three spellings for the
  added-position-correction id across `LicenceCorrectionController`, the change controllers, and
  `PartialSurrenderTaskListController`.

*Severity:* consistency smell, localised to `licence.correction.*` (all `@Profile("enable-lms2")`,
not yet fully live — cheapest window to standardise).

### 3. Leading-slash inconsistency — including within a single class

Roughly half the newer controllers omit the leading `/` on the class mapping (`licences/search`,
`licence/schedule`, `document-library/templates`, `unlinked-files`, `api/v1/logout`). Some classes
mix both styles across their own methods:

| Class | Inconsistency |
|---|---|
| `WorkProgrammeActivityController` | `/{id}/…/create` vs `work-programme-activity/{id}/update` |
| `OtherScheduleEventController` | `/{id}/…/create` vs `other-schedule-event/{id}/update` |
| `DocumentTemplatePdfController` | `without-conditions` vs `/with-conditions` |
| `TestHarnessController` | `/licence-position` (GET) vs `licence-position` (POST) |

Spring normalises these, so behaviour is identical — purely a "pick one" cleanup.

*Severity:* cosmetic. A good candidate for a checkstyle/ArchUnit guard rather than manual fixes.

### 4. Sibling REST endpoints disagree on plurality

`GET /api/organisation-units` (plural) vs `GET /api/organisation-group` (singular) — two adjacent
JSON search endpoints, opposite conventions (`OrganisationUnitRestController` vs
`OrganisationGroupRestController`). As these are *real* JSON APIs, this is the most defensible
place to standardise (→ `/api/organisation-groups`).

*Severity:* consistency smell; low blast radius (two JS autocompletes call these — grep the
`.ftl`/`.js` search URLs before renaming).

### 5. JSON file-delete endpoints use POST + verb-in-path instead of DELETE

Four JS-driven endpoints return JSON (`ResponseEntity<FileDeleteResponse>`) via
`POST …/files/delete/{fileId}`. Because these are XHR calls, not native HTML form posts, they
could legitimately be `DELETE …/files/{fileId}` — dropping both the wrong method and the
verb-in-path:

| File | Line |
|---|---|
| `LicenceScheduleSupportingInformationController` | 135 |
| `RecordFinalDecisionController` | 102 |
| `LicenceContinuationSupportingInformationController` | 128 |
| `LicenceContinuationOtherRequirementController` | 144 |

*Severity:* soft REST deviation. Changing the verb means updating the front-end fetch calls and
the CSRF handling for `DELETE`; only worthwhile if these areas are already being touched.

### 6. Resource identity carried in query params instead of the path

Operation/target identity passed as `@RequestParam` rather than a path segment (weaker
addressability):

- `LicencePositionTransferEquityController` — `?index=` identifies the transfer to
  withdraw/remove (lines 118, 143, 268, 293).
- `LicencePositionSetEquityController` — `?transferTo=` (lines 141, 233).
- `ApplicationLetterDocumentController` / `DocumentTemplateSectionController` — `?section=`
  action modifier.

*Severity:* soft REST deviation; also a mild readability concern.

### 7. Trailing slashes

`/licence/{id}/schedule/new-draft/` (`LicenceScheduleDetailDuplicationController`) and the team
roles endpoints `…/member/{wuaId}/` (`TeamManagementController`, lines 207/215/278/288) carry a
trailing slash their siblings do not.

*Severity:* cosmetic.

### 8. Other structural smells (noted, low priority)

- **Deeply nested paths (>5 segments)** in corrections, e.g.
  `/licence-corrections/{correctionId}/position/{licencePositionId}/change/{changeId}/correct-administrator-change`
  (7 segments).
- **Shared URI space, inconsistent variable naming** — `/application/{applicationType}/{applicationId}/document/{…}`
  is used by `ApplicationDocumentActionsController` and `ContinuationApplicationDocumentActionsController`,
  while `ApplicationLetterDocumentController` names the last variable `{documentInstanceSectionId}`
  for the same slot.
- **Generic noun, specific behaviour** — the `/application/…` letter endpoints are guarded by
  continuation-specific status/role rules despite the generic `application` noun.

## Considered and deemed non-issues

Recorded so they are not re-raised:

- **Verbs in URLs** (`/start`, `/delete`, `/create`, `/cancel`, `/reinstate`,
  `/allocate-steward`, …) — ubiquitous but standard and reasonable for a task/journey-based
  server-rendered UI. Not worth churning.
- **POST-for-delete / POST-for-update in HTML controllers** — correct within the GET/POST-only
  constraint of HTML forms.
- **GET (confirmation page) + POST (perform action)** across the `*DeletionController`s — this
  is the *right* pattern for destructive actions in a form app.
- **`?tab=` / `?page=` query params** for view state — acceptable.
- **`getOrCreateExpiry` on a GET** (`LicenceScheduleExpiryController`) — verified safe: returns a
  transient entity (`orElseGet(LicenceScheduleExpiry::new)`), never persists on the GET path.
  Misleading *name* only.

## Cross-cutting risk

Most of this is cosmetic, and **changing live URLs is not free.** Bookmarks, links from other
services (e.g. Energy Portal deep links), and a large integration-test suite all pin these paths.
A mass singular→plural rename, or a blanket leading-slash sweep, would touch dozens of controllers
and their tests for negligible user benefit and real regression risk. Package-based routing in
tests (`ReverseRouter`) insulates *internal* links, but *external* references and hard-coded test
URLs do not move with a rename.

## Recommended approach

In priority order:

1. **Adopt a URL convention going forward** (below) as a short ADR plus, ideally, a lightweight
   ArchUnit/checkstyle guard, so *new* controllers stop adding to the inconsistency rather than
   retrofitting the existing 100.
2. **Standardise opportunistically** — when an area is already being changed for feature work,
   bring its URLs into line (Findings 1–4). The `licence.correction.*` subtree (Finding 2) and the
   `document`/`licence.*` LMS1 areas are the cheapest windows *before* they are permanently live.
3. **Do not mass-rename** existing live URLs solely for consistency.

### Proposed URL conventions (target standard)

- **Plural collection nouns**: `/licences/{id}/…`, `/licence-schedules/…` — never singular
  `/licence/{id}`.
- **Leading slash on every mapping**, class- and method-level.
- **HTTP-method semantics for JSON (`@RestController`) endpoints**: `DELETE` for deletes, `PUT`/
  `PATCH` for updates. HTML form controllers remain GET/POST-only (accepted).
- **Resource identity in the path, not query params**; reserve query params for filter/view state
  (`?tab=`, `?page=`, `?term=`).
- **Consistent plurality within a feature** — one noun for one resource (`position` xor
  `positions`, one spelling of the added-position id).
- **`@RestController` ⇔ JSON body; `@Controller` ⇔ `ModelAndView`** — never cross them.

## Appendix — how findings were gathered

Static sweep of all `@Controller`/`@RestController` classes under `src/main/java` (110 classes);
every request-mapped handler's full path (class + method), HTTP verb, and — for the mutating-GET
check — method body were read. Method bodies were inspected to confirm which GETs mutate state
(Finding set now fixed) and to clear false positives (e.g. `getOrCreateExpiry`).
