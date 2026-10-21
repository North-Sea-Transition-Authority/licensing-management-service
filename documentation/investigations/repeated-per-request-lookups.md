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
| `LicenceScheduleTermRequestCache`, `LicenceSchedulePhaseRequestCache` | `LicenceScheduleStateService.getScheduleState` | #841 |
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

Response times generally fell with the query counts (work area 705ms → 477ms, schedule application task list 412ms →
243ms), but these are single requests, so treat them as indicative.

### Awaiting deployment

| Page | Before | Expected | PR |
|---|---:|---:|---|
| Work area (user 300051) | 13 queries, 7 Energy Portal calls | about 7 queries, 3 Energy Portal calls | #839 |
| Continuation task list (user 300052) | 49 | about 25–30 | #841 |
| Continuation overview | 29 | lower; the schedule state was worked out twice | #841 |

## Remaining

- **Eager links on terms, phases and schedule details.** Loading a term or phase still reloads its schedule detail and
  schedule (14 queries on the schedule application task list). The repositories are shared by many schedule pages, so
  this needs measuring across them, including the timeline, before changing.
- **Correction and licence position pages**, which reload the correction and position from the URL in each access
  rule. With the corrections team.
