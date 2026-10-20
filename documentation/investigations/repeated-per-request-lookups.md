# Investigation: Repeated Per-Request Lookups

## Problem

The work area request was running 10 `team_roles` / `teams` queries, all asking about the current user's
roles: the top navigation, the controller, each work area section and (on other pages) the access interceptor
rules each loaded them separately. PR #811 fixed that by having `TeamQueryService` load a user's roles once per
HTTP request through a request-scoped `TeamRoleRequestCache`.

This investigation looks for other data that is fetched more than once within the same request and could be
shared in the same way.

The schedule work programme application task list has been measured (see
[Measured: schedule application task list](#measured-schedule-application-task-list)). The other counts below
come from reading the code and are estimates; see [Next steps](#next-steps) for the pages still to measure.

## Status

| Fix | Status |
|---|---|
| Fix 4: entity graphs on the child repository methods | Done for the schedule application, and for the continuation other requirement and supporting information |
| Fix 5: stop repeating the schedule work in the task list | Done for the schedule application task list; the continuation overview shows the same pattern, not started |
| Fix 3: route `getTeamTypesUserIsMemberOf` through `TeamQueryService` | Done |
| Fix 1: request-scoped store for URL entities | Done for the schedule application detail and the continuation application detail; licence correction and licence position are with another team; licence and schedule detail not started |
| Fix 2: request-scoped cache for organisation unit lookups | Not started; a repeated lookup is confirmed on the continuation overview |

**Confirmed in the deployed request log: the schedule application task list went from 56 queries to 33**, and
loads by id from 35 to 17 (PR #828). See
[Result for the schedule application task list](#result-for-the-schedule-application-task-list).

The continuation overview has been measured before the changes in PR #830: 32 queries. See
[Measured: continuation overview](#measured-continuation-overview).

## Why repeated loads cost queries here

`spring.jpa.open-in-view=false` (set in `application.properties` since the initial commit) means every
repository call made outside a transaction gets its own Hibernate session. The session's first-level cache
never spans the request, so every repeated `findById` / `getDetailByIdOrThrow` is a real `SELECT`, and it
re-runs that entity's eager `@ManyToOne` / `@OneToOne` associations too. Loading one application detail
therefore costs more than one query, every time.

## Answer in one line

**Rows the request has already loaded are loaded again, mostly by id.** On the measured task list, 35 of 56
queries are loads by id, driven by the URL entity being loaded by each access rule and the argument resolver,
and by child entities whose eager back-reference reloads that same entity in every new session.

## Measured: schedule application task list

`GET /licence/schedule-work-programme-application/{scheduleWorkProgrammeApplicationDetailId}/task-list`, from
the `RequestLogFilter` log on 2026-10-20: **56 Hibernate queries**, 2 Energy Portal calls, 412ms.

`ScheduleWorkProgrammeApplicationTaskListController` has `@ScheduleAmendmentApplicationHasStatus`,
`@InvokingUserCanAccessScheduleApplication` and `@LogWorkAreaItemView`, receives the detail through
`ScheduleWorkProgrammeApplicationDetailArgumentResolver`, and builds its sections through
`ScheduleWorkProgrammeApplicationTaskListService.getAllSections`.

| Group | Queries | Breakdown |
|---|---:|---|
| Application detail loaded by id | 7 | `schedule_work_programme_application_details by id` (see finding 1 and finding 4) |
| Schedule rows loaded by id, as a knock-on of those detail loads | 27 | `licence_schedules by id` 9, `licence_schedule_phases by id` 8, `licence_schedule_details by id` 6, `licence_schedule_terms by id` 3, `licences by id` 1 |
| Task list section queries | 19 | `licence_schedule_extension_request` 3, `licence_schedule_details by id, status` 3, `licence_schedule_terms by licence_schedule_detail_id` 3, `licence_schedule_phases by licence_schedule_term_id` 4, `swp_application_request_purpose` 1, `licence_schedule_supporting_information` 1, `swp_external_contributor_request` 1, `work_programme_activities` 1, `file_upload_library_uploaded_files` 1, `licence_responsible_organisations` 1 |
| Other | 3 | `team_roles` 1 (shared by the request cache from PR #811), `work_area_item_views` 1, `schedule_work_programme_applications` 1 |

35 of the 56 queries (63%) are loads by id, and most of them fetch rows already loaded earlier in the request.

The Energy Portal calls (one organisation unit lookup, one org unit to group lookup) are not repeated on this
page, so fix 2 does not help it.

## Measured: continuation overview

`GET /licence/continuation-application/{licenceContinuationApplicationDetailId}/overview`, from the `RequestLogFilter`
log on 2026-10-20 before PR #830: **32 Hibernate queries**, 4 Energy Portal calls, 284ms.

`LicenceContinuationApplicationOverviewController` has `@ContinuationApplicationHasStatus`,
`@InvokingUserCanAccessContinuationApplication` and `@LogWorkAreaItemView`, and receives the detail through
`LicenceContinuationApplicationDetailArgumentResolver`.

| Repeat | Queries | Cause | Status |
|---|---:|---|---|
| `licence_continuation_application_details by id` | 4 | 3 from the status rule, the access rule and the argument resolver (finding 1); the 4th from a child entity's eager back-reference to the detail (finding 4) | Fixed in PR #830; expected to drop to 1 |
| Schedule state worked out twice | 10 | `licence_schedule_terms by licence_schedule_detail_id` 2, `licence_schedule_phases by licence_schedule_term_id` 2, `work_programme_activities by licence_schedule_term_id` 2, `other_schedule_events by licence_schedule_term_id, date_option` 2 and `other_schedule_events by licence_schedule_phase_id, date_option` 2, probably with knock-on rows (`licence_schedule_details by id` 2, `licence_schedules by id` 2) | Not started; the callers need tracing, likely `getCurrentWorkProgrammeActivitiesViews` in the controller and the summary sections |
| `file_upload_library_uploaded_files by usage_id, usage_type, document_type` | 3 | Probably different file types (supporting information, letters) | Likely genuine; not checked |
| Energy Portal "Get organisation units by ids" | 2 | The access rule's licensee lookup and the overview's licensee display (finding 2) | Fix 2, not started |

The other 11 queries run once each.

Expected after PR #830: the 4 detail loads drop to 1, so the page should be around 29 queries, depending on how
many knock-on rows each detail load brought with it. Confirm with the request log once deployed.

## Findings

### 1. The URL entity loaded by several rules and the argument resolver

Access rules run in `AccessHandlerInterceptor.preHandle`, then the argument resolver runs when the handler
method is invoked. Each rule resolves the path variable and loads the entity itself, and the resolver loads it
again.

| Entity | Loaded in the same request by | How widespread | Loads per request |
|---|---|---|---|
| `ScheduleWorkProgrammeApplicationDetail` | `ScheduleAmendmentApplicationHasStatusInterceptorRule`, `InvokingUserCanAccessScheduleApplicationInterceptorRule`, `ScheduleWorkProgrammeApplicationActionEndPointInterceptorRule`, `RequestPurposeChoiceMustBeApplicableRule`, `ScheduleWorkProgrammeApplicationDetailArgumentResolver` | First two rules on 22 controllers each; action end point 10; request purpose 5; resolver used by 19 | about 3 to 5 from rules and resolver (3 on the task list); **7 measured on the task list** once finding 4 is included |
| `LicenceContinuationApplicationDetail` | `ContinuationApplicationHasStatusInterceptorRule` (13 controllers), `InvokingUserCanAccessContinuationApplicationInterceptorRule` (9), `LicenceContinuationActionEndPointInterceptorRule` (2), `ContinuationApplicationHasWorkProgrammeActivitiesInterceptorRule` (1), `LicenceContinuationApplicationDetailArgumentResolver` | | about 2 to 4 (estimate) |
| `LicenceCorrection` | `InvokingUserCanViewCorrectionInterceptorRule` (26), `LicencePositionBelongsToCorrectionLicenceRule` (17), `LicencePositionIsNotRemovedInCorrectionRule` (14), `LicencePositionCorrectionBelongsToCorrectionRule` (12), `CorrectionLicenceIsTypeRule` (10), `CorrectionHasStatusInterceptorRule` (3), `LicenceCorrectionArgumentResolver` | Correction position pages stack several of these | up to about 5 (estimate) |
| `LicencePosition` | `LicencePositionBelongsToCorrectionLicenceRule`, `LicencePositionIsNotRemovedInCorrectionRule`, `InvokingUserCanRemoveLicencePositionInterceptorRule`, `LicencePositionCanBeReinstantiatedRule`, `LicencePositionArgumentResolver` | | about 2 to 3 (estimate) |
| `Licence` | `LicenceActionEndPointInterceptorRule` (4 controllers), `LicenceArgumentResolver` | | 2 (estimate) |
| `LicenceScheduleDetail` | `LicenceScheduleDetailHasStatusInterceptorRule` (which also loads the term / phase / rate to find the detail), `LicenceScheduleDetailArgumentResolver` | | 2 or more (estimate) |

### 2. Responsible organisations and Energy Portal calls in the access rules

`InvokingUserCanAccessScheduleApplicationInterceptorRule` and
`InvokingUserCanAccessContinuationApplicationInterceptorRule` call
`LicenceResponsibleOrganisationService.getOrgUnitToGroupIdMap(licence)` on every request. Each call costs:

- one `licence_responsible_organisations` query
- one Energy Portal call for organisation unit names (`getResponsibleOrganisationsByLicences`)
- one Energy Portal call for the org unit to org group map (`findOrganisationGroupIdMapByUnitIds`)

On the measured schedule application task list these run once each and are not repeated. On the measured
continuation overview, "Get organisation units by ids" runs twice: once for the access rule and once for the
licensees the page displays. Energy Portal calls are slower than database queries, so where they are repeated
this may be the largest saving in response time.

### 3. Team lookup that bypasses `TeamQueryService`

`TeamManagementService.getTeamTypesUserIsMemberOf` calls `teamRoleRepository.findAllByWuaId` directly, so it
does not use the request cache added in PR #811. It is used by `HasAnyRoleInTeamTypeInterceptorRule`, which
guards 3 controllers: `DocumentTemplateSearchController`, `DocumentTemplateController` and
`DocumentTemplatePdfController`. Each of those requests runs one extra `team_roles` query.

The other direct `findAllByWuaId` calls in `TeamManagementService` sit around writes (deciding whether a user
is new, or has no roles left after a removal) and should keep reading the database directly.

### 4. Child entities that reload the URL entity through eager back-references

Found by measuring the task list. Several entities that belong to an application detail map their link back to
it with an eager `@ManyToOne` (the JPA default):

| Entity | Eager associations |
|---|---|
| `SwpApplicationRequestPurpose` | `scheduleWorkProgrammeApplicationDetail` |
| `LicenceScheduleExtensionRequest` | `scheduleWorkProgrammeApplicationDetails`, `licenceSchedulePhase`, `licenceScheduleTerm` |
| `LicenceScheduleSupportingInformation` | `scheduleWorkProgrammeApplicationDetails` |

The task list sections load each of these with a repository query that runs in its own session:

| Repository method | Eager associations it drags in |
|---|---|
| `SwpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail` | `scheduleWorkProgrammeApplicationDetail`, plus the detail's own chain |
| `LicenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails` | `scheduleWorkProgrammeApplicationDetails`, `licenceSchedulePhase`, `licenceScheduleTerm`, plus each one's chain |
| `LicenceScheduleSupportingInformationRepository.findByScheduleWorkProgrammeApplicationDetails` | `scheduleWorkProgrammeApplicationDetails`, plus the detail's own chain |

Because the detail the controller already holds belongs to a different, closed session, Hibernate loads the
detail again for every child, and every reload re-runs the detail's own eager chain: application, licence
schedule, submitted schedule detail and licence. That accounts for about 4 of the 7 detail loads on the task
list, and for most of the 27 schedule rows loaded by id.

No production code navigates from these three entities back to the detail: the
`getScheduleWorkProgrammeApplicationDetail(s)()` getters exist but nothing outside the entities calls them.
The reload is pure cost.

The continuation overview shows the same pattern. `LicenceContinuationOtherRequirementRequest` and
`LicenceContinuationSupportingInformation` each have an eager `@ManyToOne` back to `LicenceContinuationApplicationDetail`,
and their repository methods (`findByLicenceContinuationApplicationDetail`) had no entity graph, so each row found
reloaded the detail. `LicenceContinuationWpaRequirementRepository` already had a graph. Again, nothing navigates from
these entities back to the detail.

The same page also repeats work inside the sections:

- `LicenceScheduleExtensionRequest` is loaded 3 times, and each load also pulls in its phase and term. The
  extension section's completeness check (`LicenceScheduleExtensionSubmissionService.isSectionSubmittable`,
  which builds the extension form) is one caller; the others on this page have not been traced yet.
- The current schedule detail (`licence_schedule_details by id, status`) is looked up 3 times, and the schedule
  state built from it (terms by schedule detail 3, phases by term 4) is rebuilt each time.
  `SwpApplicationRequestPurposeService.hasAmendableWorkProgrammeActivities` is one caller of
  `getScheduleDetailFromApplicationDetail`; the others on this page have not been traced yet.

### Checked and not worth changing

- `LogWorkAreaItemViewInterceptorRule` makes one read and possibly one write. They are different operations,
  not a repeat.
- `FeatureFlagService`, `PhasedReleaseInterceptor` and the controller advice attributes other than the top
  navigation do not touch the database.

## Recommendation

Revised after measuring the task list: fix 1 on its own would only remove 2 of the 7 detail loads there (and
their knock-on schedule rows). Most of the cost came from finding 4, which entity graphs on the child repository
methods (fix 4) address directly. The repeated section queries needed a code change (fix 5), because neither
entity graphs nor a transaction stop a query with a filter from running again. Fixes 4 and 5 are now done for
the schedule application.

### Fix 3: route `getTeamTypesUserIsMemberOf` through `TeamQueryService` (done)

Replace `teamRoleRepository.findAllByWuaId(wuaId)` with `teamQueryService.getTeamRolesForUser(wuaId)`. This
is a one-line change and saves one query per request on the document template pages. Done: the document template
pages now share the request's single team role lookup.

### Fix 4: entity graphs on the child repository methods (done for the schedule application)

Each repository method in finding 4 now has an `@EntityGraph`. This is the same approach used for the work area
N+1s (`findAllByStatusIn` on the application and continuation detail repositories).

Spring Data applies `@EntityGraph` as a fetch graph: Hibernate fetches the listed attributes in the same query
and leaves eager associations that are not listed as unloaded proxies. So each child load is a single query, and
the detail's chain (application, licence schedule, submitted schedule detail) is no longer reloaded.

| Repository method | Graph |
|---|---|
| `SwpApplicationRequestPurposeRepository.getByScheduleWorkProgrammeApplicationDetail` | `scheduleWorkProgrammeApplicationDetail` |
| `LicenceScheduleSupportingInformationRepository.findByScheduleWorkProgrammeApplicationDetails` | `scheduleWorkProgrammeApplicationDetails` |
| `LicenceScheduleExtensionRepository.findAllByScheduleWorkProgrammeApplicationDetails` | `licenceSchedulePhase.licenceScheduleTerm`, `licenceScheduleTerm` |

The extension graph lists what its callers read: the phase and its term (for the term order and display names)
and the term. It deliberately leaves out the back-reference to the detail, which nothing navigates. Its callers
(`LicenceScheduleExtensionService`, and `CrossLicenceEventTrackerService` through
`LicenceScheduleExtensionRequestQueryService`) only read the phase's and term's own fields.

Measured effect on building the task list sections: 43 → 37 queries. All 4 detail reloads from the child
entities are gone, and `licence_schedule_phases by id` drops from 4 to 2.

**Also done for the continuation application** (PR #830): `LicenceContinuationOtherRequirementRepository` and
`LicenceContinuationSupportingInformationRepository` now have `@EntityGraph(attributePaths =
"licenceContinuationApplicationDetail")` on `findByLicenceContinuationApplicationDetail`, the same graph
`LicenceContinuationWpaRequirementRepository` already used. Their callers (`LicenceContinuationOtherRequirementService`
and `LicenceContinuationSupportingInformationService`) only read and set the entity's own fields and save it. Measured
with a throwaway integration test: loading both children issued 4 queries before (each reloaded the detail) and 2
after.

### Fix 5: stop repeating the schedule work in the task list (done for the schedule application)

The repeats came from two places, and both are fixed:

- **The extension check built the extension views twice.**
  `LicenceScheduleExtensionSubmissionService.isSectionSubmittable` built the extension form, and then
  `LicenceScheduleExtensionFormValidator` built the same views again to validate it. Each build loaded the
  extension requests, looked up the current schedule detail and queried its terms and phases. The submission
  service now builds the views once and passes them to `LicenceScheduleExtensionService.buildLicenceScheduleExtensionForm`
  and `LicenceScheduleExtensionFormValidator.isValidAgainstExtensionViews`. The existing methods that take an
  application detail still work as before and delegate to these.
- **The current schedule detail was looked up separately by each check.**
  `ScheduleWorkProgrammeApplicationTaskListSectionService` now looks it up once with
  `getScheduleDetailFromApplicationDetail` and passes it to
  `SwpApplicationRequestPurposeService.scheduleHasAmendableWorkProgrammeActivities` and to
  `LicenceScheduleExtensionSubmissionService.isSectionSubmittable`, which now takes the schedule detail as well
  as the application detail (the task list was its only caller). It uses the new
  `LicenceScheduleExtensionService.getLicenceScheduleExtensionViews(applicationDetail, licenceScheduleDetail)`.

This was done inside the section service rather than in `getAllSections`, because all of the repeated work is in
the one section, so the `TaskListSectionService` interface did not need to change.

Measured effect on building the task list sections: 37 → 26 after the extension views change, then 26 → 24 after
sharing the schedule detail.

### Result for the schedule application task list

Measured with a throwaway integration test that builds data shaped like the logged page (an active schedule with
a current phase and work programme activity, a draft application requesting an extension, an extension request
and supporting information) and counts the queries `getAllSections` runs inside a web request. It reproduced the
log's section queries exactly before the change. The task list it produces is identical before and after.

| Query | Before | After fix 4 | After fix 5 |
|---|---:|---:|---:|
| `schedule_work_programme_application_details by id` | 4 | 0 | 0 |
| `licence_schedules by id` | 9 | 9 | 5 |
| `licence_schedule_details by id` | 6 | 6 | 4 |
| `licence_schedule_phases by id` | 4 | 2 | 1 |
| `licence_schedule_terms by id` | 3 | 3 | 2 |
| `licence_schedule_details by id, status` | 3 | 3 | 1 |
| `licence_schedule_terms by licence_schedule_detail_id` | 3 | 3 | 2 |
| `licence_schedule_phases by licence_schedule_term_id` | 3 | 3 | 2 |
| `licence_schedule_extension_request by …` | 3 | 3 | 2 |
| Other section queries (purpose, supporting information, external contributors, activities, file uploads) | 5 | 5 | 5 |
| **Total for building the sections** | **43** | **37** | **24** |

#### Confirmed in the deployed request log

The same task list request (same application, same user), from the `RequestLogFilter` log on 2026-10-20 after
PR #828 was deployed, with fixes 4, 5 and 1 (for the application detail) in place:

| Query | Before | After |
|---|---:|---:|
| `schedule_work_programme_application_details by id` | 7 | 1 |
| `licence_schedules by id` | 9 | 5 |
| `licence_schedule_phases by id` | 8 | 3 |
| `licence_schedule_details by id` | 6 | 4 |
| `licence_schedule_terms by id` | 3 | 2 |
| `licence_schedule_details by id, status` | 3 | 1 |
| `licence_schedule_extension_request by …` | 3 | 2 |
| `licence_schedule_phases by licence_schedule_term_id` | 4 | 3 |
| `licence_schedule_terms by licence_schedule_detail_id` | 3 | 2 |
| Other queries, one each (`schedule_work_programme_applications`, `licences`, `team_roles`, `work_area_item_views`, `licence_responsible_organisations`, request purpose, supporting information, external contributors, activities, file uploads) | 10 | 10 |
| **Total** | **56** | **33** |

- 56 → 33 queries, within the "below 37" that was expected. Loads by id fell from 35 to 17.
- The application detail is loaded once (from 7), the current schedule detail lookup runs once (from 3), and the
  extension requests take 2 queries (one load and the `exists` check), all as predicted.
- Response time was 412ms before and 243ms after. That is one request each, so treat it as indicative only.
- Energy Portal calls are unchanged: one of each, as before.

The phase counts are a little higher than in the throwaway test (3 phases by id and 3 phases by term, against 1
and 2), because this application's schedule has more terms and phases than the test data.

What is left in the section build:

- The 2 remaining extension request queries are one load of the requests and one `exists` check from the
  supporting information section (`LicenceScheduleSupportingInformationHelperService` via
  `isExtensionRequested`). The `exists` check loads no entities.
- The schedule state is still worked out twice: once by `LicenceScheduleStateService` for the amendable check and
  once by `LicenceScheduleExtensionService.getExtendableTermAndPhases`, which query terms and phases separately.
- The remaining schedule rows loaded by id (in the deployed log: `licence_schedules` 5, `licence_schedule_details`
  4, `licence_schedule_phases` 3, `licence_schedule_terms` 2, 14 queries in total) are knock-ons of the term, phase
  and schedule detail queries: those entities map their links to their schedule detail and licence schedule
  eagerly. Their repositories are shared by the timeline and other schedule pages, so changing their fetching is a
  separate piece of work. Together with the schedule state being worked out twice (5 queries in the deployed log),
  this is the largest remaining cost on the page.

### Fix 1: a request-scoped store for URL entities

**Done for `ScheduleWorkProgrammeApplicationDetail`.** It follows the review feedback on PR #811 that a cache
should be an implementation detail of the service rather than something callers opt into:

- `ScheduleWorkProgrammeApplicationDetailRequestCache` loads details through
  `ScheduleWorkProgrammeApplicationDetailRepository` and keeps them for the rest of the request.
- `ScheduleWorkProgrammeApplicationService.getDetailByIdOrThrow` reads through it.
- Its only callers are the four access rules in finding 1 and
  `ScheduleWorkProgrammeApplicationDetailArgumentResolver`, so none of them changed. They now share one load of
  the detail, and the controller receives the same instance the rules checked.

Measured against the real beans: three calls inside one request issue 1 query and return the same instance;
three calls outside a request issue 3, as before. On the task list this cuts the 3 detail loads to 1. Pages with
the action end point or request purpose rules as well save up to 4.

The remaining URL entities (continuation detail, correction, position, licence, schedule detail) can follow the
same pattern. The general design is below.

### Shared base class: `RequestScopedCache`

Both request caches (`TeamRoleRequestCache` and `ScheduleWorkProgrammeApplicationDetailRequestCache`) extend
`mvc/RequestScopedCache`. It holds each cache's values in the current request's attributes, keyed by the cache's
class name, so caches never see each other's values. Outside a web request (scheduled jobs, event listeners) there
is no request to hold them, so every lookup loads directly. This is the one place that checks whether a request is
in progress; the caches are ordinary singletons and the services that use them do not check anything themselves.

An earlier version made each cache a `@RequestScope` bean. That needed every service to check
`RequestContextHolder` before touching the cache, because the scoped proxy throws `ScopeNotActiveException`
outside a request, and a shared base class could not do that check for them.

### Remaining URL entities

**Done for the continuation application detail**, the same way as for the schedule application detail:

- `LicenceContinuationApplicationDetailRequestCache`, used by `LicenceContinuationService.getDetailByIdOrThrow`. Its
  only callers are the four continuation access rules and `LicenceContinuationApplicationDetailArgumentResolver`.

Measured against the real beans: three lookups issue 1 query inside a request, and 3 outside one, as before. Not
yet confirmed from request logs.

The licence correction and licence position lookups are being handled by another team. Still to do here: licence
and schedule detail.


Use the same pattern: a cache per entity type, extending `RequestScopedCache` and keyed by id, used by that
entity's service. Access rules and argument resolvers both read through it, so each URL entity is loaded once
per request and the rules and the controller receive the same instance.

- The access rules and argument resolvers in finding 1 already load each entity through its service (for example
  `LicenceContinuationService.getDetailByIdOrThrow` and `LicenceCorrectionService.findById`), so, as for the
  application detail, only the service method needs to read through the cache and none of its callers change.
- Extending `RequestScopedCache` gives the outside-a-request behaviour without any extra code.

### Fix 2: request-scoped cache for organisation unit lookups

A request-scoped cache of Energy Portal organisation unit lookups by id in `OrganisationUnitQueryService`
would remove repeated calls between the access rules, services and page rendering. As with the URL entities,
extending `RequestScopedCache` makes it fall back to calling the Energy Portal directly when there is no web
request. It does not help the
task list, which makes each call once.

### Longer term: make the child-to-detail back-references lazy

Mapping the back-references in finding 4 as `FetchType.LAZY` would stop the reloads at their source. It is a
wider change: every place that reads `getScheduleWorkProgrammeApplicationDetail(s)()` from one of these
entities outside a session would need checking for `LazyInitializationException`.

### Alternative to fix 4: build the task list inside one read-only transaction

Annotating `ScheduleWorkProgrammeApplicationTaskListService.getAllSections` with Spring's
`@Transactional(readOnly = true)` (`org.springframework.transaction.annotation.Transactional`; the
`jakarta.transaction.Transactional` annotation also used in this codebase has no read-only option) would make
every section share one session. Loads by id, including the child entities' back-references, would then be
reused within the session.

It does not stop queries with a filter from running again, so the repeated schedule detail, term, phase and
extension request queries would remain; fix 5 is still needed. It also holds a database connection while the
sections are built, and `readOnly = true` is not used anywhere in the codebase yet. Fix 4 is preferred.

### Alternative to fixes 1 and 4: enable open-in-view

Setting `spring.jpa.open-in-view=true` would make one Hibernate session span the whole request, removing
repeated by-id loads everywhere without new cache code. Queries with a filter (`findAllBy...`) would still run
each time. It has been off since the initial commit, and turning it on has known costs:

- a database connection is held for the whole request, including template rendering
- templates can trigger lazy loading without anyone noticing
- entities stay attached to the session for the whole request

This should be a team decision. The targeted fixes are the safer option.

## Risk Areas

- **Lazy associations on shared instances.** An entity held in the store is detached once its loading session
  closes, so touching a lazy association later in the request would throw `LazyInitializationException`. The
  entities above currently map their associations eagerly (the JPA default for `@ManyToOne` / `@OneToOne`), so
  this is safe today, but it would need checking if any of them move to `FetchType.LAZY`.
- **Stale data after a write.** Rules run before the handler and the handler receives the same instance, so
  the controller does not see a stale copy. Requests that change these entities redirect straight after the
  write, so nothing reads the store again in that request. Any new endpoint that writes and then re-reads in
  the same request would need to bypass or update the store.
- **Unloaded proxies from entity graphs.** With fix 4, associations not listed in a graph are left as unloaded
  proxies, and navigating into one after its session has closed throws `LazyInitializationException`. Nothing
  navigates from the finding 4 entities back to the detail today, so this is safe, but the tests will not catch
  a new caller that starts doing so. Any graph should list every association its callers read.
- **Test fixtures.** Controller tests mock the services that rules and resolvers call today. Moving the lookups
  behind a shared store changes what those tests need to stub, as the team roles change did in its first form.

## Affected Files

- `teams/management/TeamManagementService.java` (fix 3, done)
- `licence/continuation/LicenceContinuationApplicationDetailRequestCache.java` (new) and
  `licence/continuation/LicenceContinuationService.java` (fix 1 for the continuation detail, done)
- `licence/scheduleworkprogrammeapplication/requestpurpose/SwpApplicationRequestPurposeRepository.java`,
  `licence/scheduleworkprogrammeapplication/extendjourney/LicenceScheduleExtensionRepository.java` and
  `licence/scheduleworkprogrammeapplication/overallrequest/LicenceScheduleSupportingInformationRepository.java`
  (fix 4, done)
- `licence/continuation/requirementjourney/LicenceContinuationOtherRequirementRepository.java` and
  `licence/continuation/supportinginformation/LicenceContinuationSupportingInformationRepository.java` (fix 4 for the
  continuation application, done)
- `licence/scheduleworkprogrammeapplication/tasklist/ScheduleWorkProgrammeApplicationTaskListSectionService.java`,
  `licence/scheduleworkprogrammeapplication/requestpurpose/SwpApplicationRequestPurposeService.java` and, in
  `licence/scheduleworkprogrammeapplication/extendjourney/`, `LicenceScheduleExtensionService.java`,
  `LicenceScheduleExtensionFormValidator.java` and `LicenceScheduleExtensionSubmissionService.java` (fix 5, done)
- `licence/scheduleworkprogrammeapplication/ScheduleWorkProgrammeApplicationDetailRequestCache.java` (new) and
  `licence/scheduleworkprogrammeapplication/ScheduleWorkProgrammeApplicationService.java` (fix 1 for the application
  detail, done)
- `argumentresolver/HandlerMethodEntityResolver.java` and the argument resolvers listed in finding 1 (fix 1)
- The access interceptor rules listed in finding 1 (fix 1)
- `energyportal/organisations/OrganisationUnitQueryService.java` (fix 2)
- `licence/licenceresponsibleorganisation/LicenceResponsibleOrganisationService.java` (fix 2)
- The entities listed in finding 4 (longer-term lazy back-references)

Paths are relative to `src/main/java/uk/co/nstauthority/licensingmanagementservice/`.

## Next steps

1. Capture a `RequestLogFilter` line (overall Hibernate count, count by query and Energy Portal count by
   purpose) for:
   - ~~an application task list page~~ done: 56 queries, see
     [Measured: schedule application task list](#measured-schedule-application-task-list)
   - ~~a continuation overview page~~ done: 32 queries before PR #830, see
     [Measured: continuation overview](#measured-continuation-overview)
   - a correction position page (with another team)
2. ~~Capture the schedule application task list log again once fixes 4, 5 and 1 (for the application detail)
   are deployed~~ done: 56 → 33 queries, see
   [Confirmed in the deployed request log](#confirmed-in-the-deployed-request-log)
3. Use the remaining logs to confirm the estimates and size the gain before changing anything.
4. Capture the continuation overview log again once PR #830 is deployed (expected around 29 queries, from 32),
   and a document template page log to confirm fix 3.
5. Trace why the continuation overview works out the schedule state twice (10 queries), and fix it the same way as
   fix 5 did for the schedule application task list.
6. Fix 2: the continuation overview confirms a repeated Energy Portal organisation unit lookup.
7. Fix 1 for licence and schedule detail, measuring after each (licence correction and licence position are with
   another team). Check the continuation task list log for the same patterns.
8. Consider the remaining cost on the schedule application task list: the eager links on terms, phases and
   schedule details (14 queries) and the schedule state being worked out twice (5 queries). Both sit in the
   shared schedule services, so measure the timeline and other schedule pages before and after.
