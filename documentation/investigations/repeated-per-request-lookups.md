# Repeated Per-Request Lookups

## Problem

Pages were loading the same data several times within one request. With `spring.jpa.open-in-view=false`, every
repository call outside a transaction gets its own Hibernate session, so nothing loaded earlier in a request is
reused: each repeat is a real `SELECT`, and it re-runs the entity's eager `@ManyToOne` associations too.

The repeats came from four places:

- **The current user's team roles**, loaded separately by the top navigation, the controller, each work area section
  and each access rule.
- **The entity named in the URL**, loaded by each access rule and again by the argument resolver.
- **Child entities with eager back-references**, which reloaded their parent (application detail, schedule detail,
  schedule) in every new session.
- **Shared work**, such as the schedule state or a licence's licensees, worked out separately by each section of a
  page.

## Changes

### Request-scoped caches

`RequestScopedCache<K, V>` (in `mvc`) holds loaded values in the request's attributes for the rest of the request.
Outside a request, such as in scheduled jobs, it loads directly. It has `get`, `getAll` (fetches only the keys not
already held, in one call) and a `cacheKey` hook so entity keys can be held by id. Each cache is used internally by
one service, so callers did not change.

| Cache | Used by | PR |
|---|---|---|
| `TeamRoleRequestCache` | `TeamQueryService`, including the access rules and team type checks | #811, #830 |
| `ScheduleWorkProgrammeApplicationDetailRequestCache` | `ScheduleWorkProgrammeApplicationService` | #828 |
| `LicenceContinuationApplicationDetailRequestCache` | `LicenceContinuationService` | #830 |
| `OrganisationUnitRequestCache` | `OrganisationUnitQueryService` (Energy Portal "Get organisation units by ids") | #832 |
| `LicenceScheduleTermRequestCache`, `LicenceSchedulePhaseRequestCache` | `LicenceScheduleStateService.getScheduleState`; the term cache also by `LicenceScheduleTimelineService` | #841 |
| `LicenceContinuationScheduleDetailRequestCache` | `LicenceContinuationService` (a draft continuation's active schedule detail) | #841 |

Caches are only used for reads. Lookups that a request can change before reading again, such as
`LicenceScheduleDetailService.getScheduleDetailByLicenceAndStatusOrThrow` when a draft schedule is applied, are left
uncached.

### Entity graphs

`@EntityGraph` on the work area's application and continuation queries, and on the child repository methods of the
schedule application and continuation (extension requests, supporting information, other requirements), so the
children arrive with their parent instead of reloading it.

### Doing shared work once

- **Schedule application task list** (#828): the sections share one schedule state and one current schedule detail.
- **Work area** (#839): each section loads its rows, then `WorkAreaContextService` looks up licensees, organisation
  groups, the licensee group filter and viewed items once for all sections.

### Smaller fixes

- `TeamManagementService.getTeamTypesUserIsMemberOf` goes through `TeamQueryService` (#830).
- `LicenceService.getLicencePageCaption` uses the licence it is given instead of reloading it, on its 34 callers.
- `LicenceScheduleDetailHasStatusInterceptorRule` uses the detail the term, phase, rate, activity or other event already
  holds instead of reloading it.
- `OtherScheduleEventService.hasEventWithinScheduleWindow`, the mandatory relinquishment check, skips a missing next term or
  phase and does not look up the same term twice when the next phase is in the current term. On the continuation
  overview it took the check from 5 queries to 2. The other repeats traced on that page are different queries: an
  `exists` check by the work programme summary section and the activity list for the controller.
- `LicenceScheduleTimelineService` loads the schedule's work programme activities and rates once, reusing its pre-fetched
  event data for the work programme statuses and the rate dates (`LicenceScheduleCalculationService` has an overload
  taking the loaded rates), and reads terms through the term request cache so the summary card and the event views
  share one load. This covers the schedule timeline, the licence overview's schedule tab and the schedule history.

## Results

From the `RequestLogFilter` request logs on the deployed environment, same page, record and user before and after.

| Page | Before | After | Change |
|---|---:|---:|---|
| Work area (user 300051) | 130 | **13** | Entity graphs (130 → 22), team role cache (22 → 13) |
| Schedule application task list | 56 | **33** | Entity graphs, shared schedule state, application detail cache. Loads by id 35 → 17 |
| Continuation overview | 32 | **29** | Continuation detail cache. Energy Portal calls 4 → 3 with the organisation unit cache |
| Document template overview | — | **5** | One team role lookup per request, shared by the access rule and the controller |
| New schedule draft start page | 8 | **7** | Page caption no longer reloads the licence |
| Schedule term update page | 6 | **4** | Page caption and status rule no longer reload the licence and detail (173ms → 111ms) |
| Continuation task list (user 300052) | 49 | **27** | Schedule term, phase and active schedule detail caches: the active detail looked up once (from 4), terms and phases loaded once (9 → 3), eager reloads 19 → 6 (587ms → 319ms) |
| Continuation overview, after #841 | 29 | **24** | Schedule term and phase caches: terms, phases and their eager reloads loaded once (8 → 4). Measured as user 300051, who had viewed the application before, against the 29 for user 300052 |
| Continuation overview, after the relinquishment check fix | 24 | **21** | The check skips a missing next term or phase and repeated terms: 5 queries to 2. This application has no current or next phase, so both phase lookups had been for a missing phase. Same user (300051) and application |
| Schedule timeline, draft (user 300051) | 27 | **22** | Terms, work programme activities and rates loaded once (each from 2), which also removed a schedule detail and a schedule reload. The `schedule_events` and remaining detail reloads, from event comments and work programme statuses, are unchanged |

Response times generally fell with the query counts (work area 705ms → 477ms, schedule application task list 412ms →
243ms), but these are single requests, so treat them as indicative.

### Still to confirm

| Page | Before | Expected | PR |
|---|---:|---:|---|
| Work area (user 300051) | 13 queries, 7 Energy Portal calls | about 7 queries, 3 Energy Portal calls | #839 |

## Remaining

From a review of the code on 2026-10-21. Query counts are estimates from reading the code, not from request logs;
measure each page before and after changing it.

### Highest impact

| # | Where | Problem | Estimated saving | Suggested fix |
|---|---|---|---|---|
| 1 | Licence page shell on every tabbed licence page (`TabbedLicencePageService.hydrateModel`); the schedule tab is the default licence page | The licence actions are worked out twice (top-level and tab, `LicenceActionService.filterLicenceActionItems`). Licence status is read about 4 times (actions twice, `LicenceSummaryCardService`, `LicenceScheduleTimelineService`). The licence's schedule details are loaded about 5 times (actions twice, the tab's ACTIVE and DRAFT lookups, the history options), each reloading the schedule and licence through eager links. Licensees are loaded twice | Schedule tab about 12–16 queries, history about 8–10, position tab about 4–5 | Load once in `hydrateModel` and pass down; derive ACTIVE, DRAFT and the history options from one list of details. Not a request cache: `LicenceActionEndPointInterceptorRule` runs the same action checks on POSTs that create or apply drafts |
| 2 | Eager parents loaded one by one on list queries | Nothing is mapped lazy, so when a query returns many rows without joining in their parent, each distinct parent is loaded with its own select. Affects the position timeline (`LicencePositionChangeRepository.findByLicencePositionIn`, 10–100+ positions), the timeline's event comments (`EventCommentRepository.getAllByScheduleEvent_LicenceScheduleAndStatus`), the cross-licence event tracker table (`ScheduleWorkProgrammeApplicationDetailRepository.findAllByScheduleWorkProgrammeApplication_IdIn` and `LicenceContinuationApplicationDetailRepository.findAllByLicenceContinuationApplication_IdIn`), correction position corrections (`LicencePositionCorrectionRepository.findByLicenceCorrection`) and `EnergyPortalDataService` (`TeamRoleRepository.findAll`) | Tens to hundreds of queries on large licences | Targeted `@EntityGraph`s on those repository methods (overriding `findAll` on `TeamRoleRepository`), so the parents arrive in the same query; a projection where only a few columns are used, such as the event tracker. The global `hibernate.default_batch_fetch_size` was considered and not chosen |
| 3 | Every schedule edit save (`LicenceScheduleCalculationService`): terms, phases, rates, activities, events, start date, deletes and schedule duplication | Dates are recalculated term by term and phase by phase, each with its own activity, rate and other event queries and `saveAll` | About 40–50 queries and 40 saves per save | Load activities, rates and other events once per schedule detail, group them in memory, and save each type once |
| 4 | Application access rules on all 31 schedule application and continuation pages (`InvokingUserCanAccessScheduleApplicationInterceptorRule`, `InvokingUserCanAccessContinuationApplicationInterceptorRule`) | `LicenceResponsibleOrganisationService.getOrgUnitToGroupIdMap(licence)` makes an Energy Portal names call it discards and an uncached Energy Portal groups call (`OrganisationUnitQueryService.findOrganisationGroupIdMapByUnitIds`), even for regulators viewing submitted applications, where `ApplicationAccessService` never uses it. `userCanSubmitApplication` makes a third call on review pages | 1–2 Energy Portal calls per page | A request cache for organisation unit → group id, and build the map only when the access check needs it |
| 5 | Schedule application review and submit (`ScheduleAmendmentApplicationReviewAndSubmitController`) | The page runs the task list (`isSubmittable`) and then the summary sections, which reload the request purpose (3 times), the extension and amendment requests, the supporting information and files, the external contributor request, and run `isExtensionOrAmendment` (2 `exists` queries) again | About 10–12 queries; about 3 on the submitted overview; about 4–6 on the continuation review page | One per-application context passed to the task list and summary sections. Not a request cache: the section save POSTs read and write these records |

### Medium

- **Schedule child edit and delete pages** (20 endpoints): `LicenceScheduleDetailHasStatusInterceptorRule` loads the
  term, phase, rate, activity or other event, then the controller or `WorkProgrammeActivityArgumentResolver` loads it
  again. 1 query each.
- **Schedule detail create pages** (about 14 endpoints: `/{licenceScheduleDetailId}/.../create`, expiry, start date):
  the status rule and `LicenceScheduleDetailArgumentResolver` both load the detail. 1 query each.
- **Licence action endpoints** (`StartLicenceCorrectionController`, `LicenceScheduleDetailDuplicationController`,
  `LicenceStartDateController`, `StartLicenceScheduleJourneyController`): the rule and `LicenceArgumentResolver` both
  load the licence, and the rule evaluates every licence action rather than only the ones the annotation asks for.
- **Schedule application request purpose** (`RequestPurposeChoiceMustBeApplicableRule`,
  `SelectLicenceWorkAmendmentController`, `SwpApplicationRequestPurposeController`): the active schedule detail and the
  work programme activity check run in both the rule and the controller.
  `ScheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail` is not cached, unlike the
  continuation equivalent.
- **Letter pages and PDF previews** (`ApplicationLetterController`, `ApplicationDocumentActionsController`): each mail
  merge field in `DocumentLinkingService` reloads the application and its latest detail, as do the page title and
  `LmsDocumentInstanceService.renderPdf`. About 10–14 queries per page; used less often.
- **Record of decision pages:** `RecordOfDecisionSectionService` loads the decision record 4 times;
  `RecordWorkProgrammeAmendmentDetailsService` loads a work programme status per activity although only the due date
  is used; `WorkProgrammeDecisionSummarySectionService` loads target licences per work programme decision;
  `RecordDurationChangesService.getScheduleInOrder` loads phases per term.
- **Extension request page** (`LicenceScheduleExtensionService`): phase checks run per term and are repeated by the
  page, the validator and the submission check; the save looks up each term and phase by id.
- **Schedule form relative date options** (`LicenceScheduleRelativeOptionsService`): phases loaded per term on the
  rate, activity and other event forms.
- **Energy Portal users:** the application overviews and the record of decision task list fetch the submitter and the
  steward in two calls; `TeamManagementService.getActiveTeamMembersViewsForTeamAndRole` makes one call per team member
  when a continuation decision is issued or a continuation is withdrawn.
- **Organisation groups:** the contact update page, and the licence search and contacts list with a licensee group
  filter, fetch the same organisation group twice.
- **Eager links on terms, phases and schedule details.** Loading a term or phase still reloads its schedule detail and
  schedule (14 queries on the schedule application task list, none on the schedule timeline). The repositories are
  shared by many schedule pages, so only worth changing if a page needs it, with entity graphs on that page's queries.
- **Timeline reloads from event comments and work programme statuses.** Both link eagerly to their schedule event,
  reloading `schedule_events` (3) and the schedule detail behind them (2 of the 3 remaining detail loads). Entity graphs
  on those queries would remove them; the event comments query is listed in item 2.

### Owned by other teams

- **Correction and position pages:** the correction, position and position change are each reloaded by several stacked
  access rules and the argument resolver, up to 6–8 redundant queries per request. `LicencePositionViewService` and
  `LicencePositionCorrectionRepository.findByLicenceCorrection` reload positions through eager links (item 2).

### Bug found during the review

The registered address mail merge field is probably always empty.
`OrganisationUnitQueryService.getOrganisationUnitAddressById` reads `getRegisteredAddress()`, but
`ORGANISATION_UNITS_PROJECTION_ROOT` only requests the id, name and registered number. This predates the organisation
unit request cache; check a generated letter.

### Checked and fine

The page layout (`DefaultPageControllerAdvice`, `TopNavigationService`) only reads cached team roles. The licence
search, licence correction search, document template search, contacts table, cross-licence event tracker main query,
case events and work programme activity view lists are already batched.
