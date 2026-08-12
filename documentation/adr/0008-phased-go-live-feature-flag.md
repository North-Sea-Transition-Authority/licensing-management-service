# Investigation: Phased Go-Live Feature Flag
* Author: Danny Betts 
* Date: 04/08/2026
* Status: Accepted
* Reviewers: James Barnett, Dainis Grinbergs

## TL;DR

- **Goal:** go live in **three phases** — *Initial* (work area, teams, licence contacts),
  *LMS1* (schedules, licence search & management, schedule/continuation applications,
  document library), *LMS2* (everything else). Later phases must be invisible *and*
  unreachable until switched on.
- **Toggle:** Spring profiles. One new profile, `enable-lms1`; the existing `enable-lms2`
  broadens to "everything not in Initial or LMS1". Initial = no profiles; each phase adds
  one. Absence = locked (fail-safe).
- **Three layers to gate**, all reading one `FeatureFlagService`:
  1. **Navigation** — filter `TopNavigationService` by phase (hides *Licences* and *Document
     library* until LMS1).
  2. **In-page actions, options & categories** — a generic `ReleaseFeature` enum (Start
     application, edit schedule, each application type, …) each mapped to a phase;
     controllers/templates guard on `isEnabled(...)`, and collections — option lists *and*
     whole categories of results (e.g. work-area providers) — are filtered via
     `filterEnabled(...)`, with the submit path re-checked. Adding a toggle, a new application
     type, or a new provider = adding an enum constant / `PhaseGated` bean, not a method.
  3. **Direct URL access** — a **default-deny allow-list interceptor**
     (`PhasedReleaseInterceptor` + `PhasedReleasePolicy`). The policy classifies each request's
     **controller (by package)** to a phase; an unclassified controller → 404, so new
     controllers are locked until explicitly classified. Plain 404, no holding page.
- **Why an allow-list, not `@Profile` per controller:** default-deny means a *new* controller
  is off until listed — the correct posture for "everything else is hidden". `@Profile`
  scattered across ~50 controllers is opt-out and leaks new endpoints.
- **Migration:** each area's data lands with its phase, **except licences** (present from the
  Initial release, since licence contacts need them).
- **Retirement:** flags are scaffolding — once a phase is permanently live, delete its flag.
  Enum-constant checks make this compiler-guided; promote its allow-list rules to `NOT_FLAGGED`
  rather than deleting them (default-deny would 404 a live area).

## Problem

The LMS service is going live in **three phases**, gated by Spring profiles:

| Phase | Profile(s) | Scope |
|-------|-----------|-------|
| **Initial** | *(none)* | Work area, teams, licence contacts only |
| **LMS1** | `enable-lms1` (new) | + schedules management, licence search & management, schedule & continuation applications, document library |
| **LMS2** | `enable-lms2` (existing) | + all other functionality |

The **initial live release** must expose only:

- **Work area** (`/work-area`) — accessible, but *without* the "Start application" button
- **Teams** (`/team-management`)
- **Licence contacts** (`/licence-contacts`)

Everything else — the "Licences" and "Document library" navigation sections, every
application journey (schedule work programme, continuation), licence schedule editing,
licence corrections, licence search/overview, document templates, etc. — must be
**inaccessible**, both from navigation *and* by direct URL, until its phase is switched on.

The toggle must be a **Spring profile feature flag** so the same artefact can run fully
locked-down in the initial live environment and fully open in development/test. Only one new
profile (`enable-lms1`) is introduced; `enable-lms2` already exists.

The key nuance: this is not a simple "hide the menu" job. There are three distinct
enforcement layers, and a page can be *accessible* while specific functionality *on* that
page is hidden (the work area is the canonical example — the page stays, the Start button
goes).

## The three enforcement layers

| # | Layer | What it controls | Current injection point |
|---|-------|------------------|-------------------------|
| 1 | **Navigation visibility** | Which top-nav sections appear | `TopNavigationService.getTopNavigationItems()` |
| 2 | **In-page functionality** | Buttons/links/actions, individual options in a list (e.g. application types), and whole categories of results (e.g. work-area providers) inside an *accessible* page | Controller model attributes + Freemarker `<#if>` / filtered collections |
| 3 | **Direct URL access** | Whether a controller endpoint responds at all | `AccessHandlerInterceptor` chain / `@Profile` on controllers |

Hiding a nav item (layer 1) does **not** stop someone typing the URL, and hiding a button
(layer 2) does **not** stop them hitting the journey's entry endpoint directly. A robust
lock-down needs all three, and layer 3 is the one that actually enforces security.

## Current-state findings

### Layer 1 — Navigation

The nav is data-driven from a single enum, filtered by one service, and injected into
every page via a `@ControllerAdvice`.

- **`TopNavigationItem`** (`topnavigation/TopNavigationItem.java`) — enum of the five
  sections, each with a display name, order, pre-computed `ReverseRouter` URL, and optional
  required team-type/roles:

  | Constant | Display name | Order | Currently gated by |
  |----------|--------------|-------|--------------------|
  | `WORK_AREA` | Work area | 10 | always (logged in) |
  | `LICENCES` | Licences | 20 | always |
  | `TEAMS` | Teams | 30 | always |
  | `LICENCE_CONTACTS` | Licence contacts | 40 | always |
  | `DOCUMENT_LIBRARY` | Document library | 50 | `LICENCE_MANAGEMENT` team roles |

- **`TopNavigationService.getTopNavigationItems()`** (`topnavigation/TopNavigationService.java`)
  — streams `EnumSet.allOf(TopNavigationItem.class)`, filters by team/role, sorts by
  display order. **This is the single filter point for nav visibility.**
- **`DefaultPageControllerAdvice`** (`mvc/DefaultPageControllerAdvice.java`) — adds
  `navigationItems` to the model on every page.

For the initial release, `LICENCES` and `DOCUMENT_LIBRARY` must be removed from the nav;
`WORK_AREA`, `TEAMS`, `LICENCE_CONTACTS` remain.

### Layer 2 — In-page actions / features

A page that stays *accessible* can still host actions, links, buttons, or task-list rows
that belong to a later phase and must be hidden. These are not a single feature — there
will be many across the initial-phase pages (work area, teams, licence contacts), and more
will appear as functionality lands. They all share one existing shape: **a controller adds
a boolean (or a URL) to the model, and a Freemarker `<#if>` renders the element only when
it is truthy.**

The Start application button on the work area is the canonical example:

- **`WorkAreaController.getModelAndView()`** (`workarea/WorkAreaController.java`) adds:

  ```java
  .addObject("canStartApplication",
      applicationAccessService.userHasAccessToStartApplication(user.wuaId()))
  .addObject("startApplicationUrl",
      ReverseRouter.route(on(SelectApplicationTypeController.class).render()))
  ```

- **`workArea.ftl`** renders the button only when the flag is truthy:

  ```ftl
  <#if canStartApplication>
      <@fdsAction.link linkText="Start application" linkUrl=springUrl(startApplicationUrl) linkClass="govuk-button"/>
  </#if>
  ```

Here the condition is a pure role check (`userHasAccessToStartApplication`) today; under
phasing it must *also* be gated by the feature flag, while the page itself keeps rendering.

The general pattern to enforce for **any** such element:

1. The element is already (or becomes) guarded by a model boolean + `<#if>`.
2. That boolean is ANDed with the relevant `ReleaseFeature` flag (role-gated actions), or
   the element is guarded directly against the globally-exposed enabled-features set
   (phase-only elements).
3. The surrounding page is unaffected and continues to render.

The generic mechanism for (2) is defined under [Recommended implementation §4](#4-layer-2--in-page-actions--features);
the point here is that Layer 2 is a *class* of elements to sweep for, not one button.

### Layer 3 — Direct URL access

- **`AccessHandlerInterceptor`** (`authorisation/AccessHandlerInterceptor.java`) is a
  `HandlerInterceptor` that Spring injects with `List<AccessInterceptorRule>` (ordered by
  `@Order`). For each handler it finds the rule's marker annotation (**method first, then
  declaring class**, via `AnnotationUtils.findAnnotation`) and, if present, calls
  `rule.check(...)` → `SecurityRuleResult`. A failed result throws `ResponseStatusException`
  with the rule's status. Registered in `WebMvcConfiguration.addInterceptors()`.
- **`AccessInterceptorRule`** (`authorisation/rules/AccessInterceptorRule.java`) —
  `supports()` returns the annotation class; `check()` returns
  `SecurityRuleResult.continueAsNormal()` or `checkFailedWithStatus(...)`.
- **Existing precedent — the `enable-lms2` profile.** Licence corrections are *already*
  feature-flagged: every controller under `licence/correction/**` and
  `licence/position/LicencePositionController` carries `@Profile("enable-lms2")`. When the
  profile is inactive the controller bean is never registered, so the endpoint 404s. This
  is the closest existing pattern to what we need, and confirms profile-based feature
  gating is an accepted approach in this codebase.
- **Security filter chain** (`configuration/WebSecurityConfiguration.java`) permits
  `/assets/**`, `/error`, `/api/v1/logout/*` anonymously; everything else requires the
  `LMS_ACCESS_PRIVILEGE` authority. Interceptors run *after* this chain, so any phase check
  runs with an authenticated user already established.

### Profiles already in use

`development`, `production`, `integration-test`, `test`, `test-harness`,
`test-account-bootstrap`, `migration`, `mockups`, `internal-only`, `debug`,
`gis-migration`, and the feature flag **`enable-lms2`**. Profiles are supplied externally
(`SPRING_PROFILES_ACTIVE` / run config); no `spring.profiles.active` is baked into the
property files. There are **no** `@ConditionalOnProperty` / boolean `feature.*` toggles —
gating is done exclusively through profiles.

## Design decisions

### Decision 1 — Toggle: profile vs. property

**Chosen: a Spring profile** (as requested, and consistent with `enable-lms2`).

There are **three release phases**, so only **two profiles** are needed — the existing
`enable-lms2` plus one new `enable-lms1`. The initial phase runs with *neither*; each
subsequent phase adds one profile.

| Phase | Profile(s) active | Unlocks |
|-------|-------------------|---------|
| **Initial** | *(none)* | Work area, teams, licence contacts only |
| **LMS1** | `enable-lms1` | Schedules management (licence schedule editing), licence search & management, schedule & continuation applications (incl. the Start application button), document library |
| **LMS2** | `enable-lms1` + `enable-lms2` | All other functionality (licence corrections, licence timeline/position, etc.) — via the **existing** `enable-lms2` profile |

`enable-lms2` already exists (it currently gates licence corrections and the licence
timeline/position), so no new profile is introduced there — its scope simply broadens to
"everything not in Initial or LMS1". Only `enable-lms1` is new.

> **Fail-safe direction.** Absence of a profile = locked down. The live environment starts
> with both profiles *off*, so a forgotten config leaves features hidden, never accidentally
> exposed. The main `application.properties` carries **no** phase profiles. Tests activate both
> via a `spring.profiles.group` in a **test-only** base properties file
> (`src/test/resources/application.properties`, keyed on the `test` and `integration-test`
> profiles) so the suite exercises the full app; local development enables `enable-lms1` /
> `enable-lms2` **manually** in the run configuration.
>
> The group must live in a *base* (non-profile-specific) source: a `spring.profiles.group`
> defined inside an `application-<profile>.properties` document is applied too late to activate
> the grouped profiles.

### Decision 2 — Enforcement: per-controller `@Profile` vs. allow-list interceptor

**Chosen: a default-deny allow-list interceptor**, not `@Profile` scattered across
controllers.

- **Option A — `@Profile("!...")` on every disabled controller.** Follows `enable-lms2`
  exactly. *Rejected as the primary mechanism* because it is opt-out per controller: there
  are ~50 controllers to annotate, it is easy to miss one, and — critically — **any
  controller added in future is exposed by default**. That is the wrong default for a
  "everything else is off" requirement.
- **Option B — a new `AccessInterceptorRule` + marker annotation.** Idiomatic, but still
  opt-in per controller (you must remember to annotate each one), so it shares Option A's
  "new controller leaks" flaw.
- **Option C (chosen) — one allow-list interceptor.** **Block every request whose controller
  is not on the allow-list (or whose phase is off).** New controllers are disabled by default
  until explicitly classified. A single, auditable place defines exactly what the initial
  release exposes. This inverts the default from "exposed unless annotated" to "hidden unless
  allow-listed", which is the correct posture for a go-live lock-down.

Option A/B remain useful *in addition* for individual sub-features that are hard to express
as a path prefix, but the interceptor is the backstop.

## Recommended implementation

### 1. A single source of truth: `FeatureFlagService`

A thin bean wrapping Spring's `Environment` so profile checks live in one place and both
Java and templates consult the same logic.

There are two concepts:

- **`ReleasePhase`** — the profile-backed phase (NOT_FLAGGED / LMS1 / LMS2). This is the *only*
  thing tied to a Spring profile.
- **`ReleaseFeature`** — the catalogue of individually toggleable **actions / in-page
  features** (Start application, edit schedule, add licence contact override, start
  correction, …). Each feature is declared once and mapped to the phase it belongs to.
  Nothing outside this enum needs to know *which profile* a feature lives behind — callers
  ask about the feature, not the phase.

```java
public enum ReleasePhase {
  NOT_FLAGGED(null),          // always on — initial release
  LMS1("enable-lms1"),    // new profile
  LMS2("enable-lms2");    // existing profile

  private final String profileName;
  // constructor + getProfileName()
}
```

```java
public enum ReleaseFeature {
  // --- LMS1 ---
  START_APPLICATION(ReleasePhase.LMS1),        // the Start button (coarse: any type available)
  SCHEDULE_APPLICATION(ReleasePhase.LMS1),     // an individual application-type option
  CONTINUATION_APPLICATION(ReleasePhase.LMS1), // "
  EDIT_SCHEDULE(ReleasePhase.LMS1),
  LICENCE_SEARCH(ReleasePhase.LMS1),
  DOCUMENT_LIBRARY(ReleasePhase.LMS1),
  // --- LMS2 ---
  START_CORRECTION(ReleasePhase.LMS2);
  // ...add one entry per action/feature/option that needs phasing — including each new
  //    application type as it is introduced (see §5)

  private final ReleasePhase releasePhase;
  // constructor + getReleasePhase()
}
```

A `ReleaseFeature` is granular enough to gate a single **option** (one application type), not
just a whole action — §5 uses these per-type constants. A new application type added in a
future release names its own `ReleaseFeature`, so it appears on the start screen only once
its phase is on.

The service exposes **one generic check** (plus a phase-level overload the interceptor and
nav filter use). There are **no** per-feature methods — adding a new toggle means adding an
enum constant, not a service method.

```java
@Service
public class FeatureFlagService {

  private final Environment environment;

  public FeatureFlagService(Environment environment) {
    this.environment = environment;
  }

  /** Phase-level check — used by the interceptor and nav filter. */
  public boolean isEnabled(ReleasePhase phase) {
    return phase == ReleasePhase.NOT_FLAGGED
        || environment.acceptsProfiles(Profiles.of(phase.getProfileName()));
  }

  /** Feature-level check — the generic entry point for actions / in-page features. */
  public boolean isEnabled(ReleaseFeature feature) {
    return isEnabled(feature.getReleasePhase());
  }

  /** All features currently switched on — handy for exposing to templates in one shot. */
  public Set<ReleaseFeature> getEnabledFeatures() {
    return Arrays.stream(ReleaseFeature.values())
        .filter(this::isEnabled)
        .collect(toUnmodifiableSet());
  }
}
```

Call sites read as `featureFlagService.isEnabled(ReleaseFeature.START_APPLICATION)` — the
feature names the intent, and its phase mapping lives in one place.

### 2. Layer 3 — allow-list interceptor

Two collaborating pieces, deliberately separated:

- **`PhasedReleasePolicy`** — *the data.* The single, auditable definition of the allow-list:
  each controller, classified **by package**, is mapped to the `ReleasePhase` it belongs to.
  It answers one question — *"which phase does this request's controller belong to?"* — and
  returns nothing for an unclassified controller (which is what makes the default deny).
- **`PhasedReleaseInterceptor`** — *the plumbing.* A `HandlerInterceptor` that, for each
  request, takes the resolved controller from the `HandlerMethod`, asks the policy for its
  phase, and asks `FeatureFlagService` whether that phase is on. No classification lives in
  the interceptor; it is generic.

> **Why classify by controller package, not URL pattern?** The application has request
> mappings a pure path allow-list cannot express safely — notably `LicenceInternalApiRestController`
> whose `@RestController("/internal/api/licences")` value is the *bean name*, not a path, so
> the endpoint actually maps at root `/{slug}`; plus `FooterController`, which maps several
> root paths (`/accessibility-statement`, `/cookies`, `/contact`). A single root-level `/{var}`
> mapping makes a prefix allow-list either catch everything or miss it. Classifying the resolved
> controller by package sidesteps all of this and is more robust: it does not depend on how a
> controller happens to spell its `@RequestMapping`.

#### `PhasedReleasePolicy` — the allow-list as code

The "The allow-list" section below is the human-readable version of exactly this mapping.
Longest / most-specific prefix first, first match wins; **an unclassified controller returns
empty and is therefore denied** — so a new controller is locked until someone adds its package
here (enforced by the ArchUnit test in Testing).

```java
public final class PhasedReleasePolicy {

  private static final String BASE = "uk.co.nstauthority.licensingmanagementservice.";

  // Ordered — first match wins. More specific / differing-phase packages precede the broad
  // "licence" rule (licence.contact is NOT_FLAGGED, licence.correction/position are LMS2).
  private static final List<Map.Entry<String, ReleasePhase>> RULES = List.of(
      Map.entry(BASE + "licence.contact",    ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "licence.correction", ReleasePhase.LMS2),
      Map.entry(BASE + "licence.position",   ReleasePhase.LMS2),
      Map.entry(BASE + "licence",            ReleasePhase.LMS1),   // search, overview, schedule, apps, internal API…
      Map.entry(BASE + "document",           ReleasePhase.LMS1),
      Map.entry(BASE + "workarea",           ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "teams",              ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "feedback",           ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "fds",                ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "file",               ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "mvc",                ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "authentication",     ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "energyportal",       ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "migration",          ReleasePhase.NOT_FLAGGED),  // dev/test-only, already @Profile-gated
      Map.entry(BASE + "mockups",            ReleasePhase.NOT_FLAGGED),
      Map.entry(BASE + "testharness",        ReleasePhase.NOT_FLAGGED)
  );

  /** Empty = controller is not on the allow-list → deny. */
  public static Optional<ReleasePhase> phaseFor(Class<?> controllerType) {
    var packageName = controllerType.getPackageName();
    return RULES.stream()
        .filter(rule -> packageName.equals(rule.getKey()) || packageName.startsWith(rule.getKey() + "."))
        .map(Map.Entry::getValue)
        .findFirst();
  }
}
```

#### `PhasedReleaseInterceptor` — the enforcement

```java
@Component
public class PhasedReleaseInterceptor implements HandlerInterceptor {

  private final FeatureFlagService featureFlagService;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;  // static resources and other non-controller handlers pass through
    }
    var phase = PhasedReleasePolicy.phaseFor(handlerMethod.getBeanType());
    if (phase.isPresent() && featureFlagService.isEnabled(phase.get())) {
      return true;
    }
    // Unclassified controller, or its phase is off → hide it.
    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  }
}
```

Registered in `WebMvcConfiguration.addInterceptors()`, before `AccessHandlerInterceptor`;
runs post-authentication. Non-`HandlerMethod` handlers (static resources) pass straight
through, and infrastructure controllers (`DefaultErrorController`, `LogoutController`, …) are
classified NOT_FLAGGED by their package, so nothing infra-critical is ever blocked.

The interceptor returns a plain **404** (rather than 403) — this hides the feature entirely
and avoids advertising that a hidden endpoint exists. There is no "coming soon" / holding
page.

> **Why an interceptor, not the security filter chain?** The existing lock-down needs to
> know the authenticated user and integrate with the same MVC layer as `AccessHandlerInterceptor`.
> Adding it as a `HandlerInterceptor` next to the existing ones keeps it consistent and
> testable with the current `@WebMvcTest`-style tests. It could equally be an
> `AccessInterceptorRule`, but that pattern is annotation-driven (opt-in), which defeats
> default-deny — so a standalone interceptor is the better fit.

### 3. Layer 1 — nav filter

Add one predicate to `TopNavigationService.getTopNavigationItems()`, mapping each nav item
to its `ReleasePhase`:

```java
.filter(item -> featureFlagService.isEnabled(item.getReleasePhase()))
```

Give `TopNavigationItem` a `ReleasePhase` field: `WORK_AREA/TEAMS/LICENCE_CONTACTS →
NOT_FLAGGED`, `LICENCES → LMS1`, `DOCUMENT_LIBRARY → LMS1`. The existing team/role filter stays;
the phase filter is ANDed on top. Covered by the existing `TopNavigationServiceTest`.

### 4. Layer 2 — in-page actions / features

This is where the generic `ReleaseFeature` catalogue earns its keep — there will be *many*
of these (buttons, links, task-list rows, inline actions), not just Start application. Two
consumption styles, both driven by the same enum:

**a) In a controller — when the action also depends on the user's role/permission.** AND
the feature flag with the existing check:

```java
.addObject("canStartApplication",
    featureFlagService.isEnabled(ReleaseFeature.START_APPLICATION)
        && applicationAccessService.userHasAccessToStartApplication(user.wuaId()))
```

The template stays as `<#if canStartApplication>`. Every other role-gated action follows
the identical shape — just swap the `ReleaseFeature` constant and the permission check.

**b) In a template — for phase-only toggles with no per-user condition.** Expose the whole
set once, globally, via `DefaultPageControllerAdvice`, so any `.ftl` can guard a feature
without touching its controller:

```java
// DefaultPageControllerAdvice
model.addAttribute("enabledFeatures",
    featureFlagService.getEnabledFeatures().stream().map(Enum::name).collect(toSet()));
```

```ftl
<#if enabledFeatures?seq_contains("DOCUMENT_LIBRARY")>
    <@fdsAction.link linkText="Manage templates" .../>
</#if>
```

New actions plug in with **no service change**: add a `ReleaseFeature` constant (mapped to
its phase) and reference it. This keeps the "which phase is this action in?" decision in
one enum rather than scattered across controllers and templates.

### 5. Filtering collections by phase (options *and* whole categories)

Layer 2 hides a *whole* element. This section covers the two "filter a collection" cases —
both handled by one mechanism: **individual options within a page** (e.g. application types
on the start screen) and **whole categories of results contributed by beans** (e.g. a
work-area provider). A page stays fully accessible; only some of what it lists is shown.

The options case first. The canonical example is the **start-application screen**
(`SelectApplicationTypeController`), which renders a radio list from the `ApplicationType`
enum: schedule-amendment and continuation today, and **new application types will be added
over time**, each of which may belong to a later phase. The same shape recurs for any
selectable list — dropdown values, filter options, a menu of sub-journeys.

Model this with a small marker interface so *any* option type can opt in, and one filter
method on the service:

```java
/** Anything whose visibility is gated by a release phase — enum options, menu items, … */
public interface PhaseGated {
  ReleaseFeature getReleaseFeature();
}
```

```java
public enum ApplicationType implements Displayable, PathVariableEnum, PhaseGated {
  SCHEDULE_AMENDMENT_APPLICATION(..., ReleaseFeature.SCHEDULE_APPLICATION),
  CONTINUATION_APPLICATION(...,     ReleaseFeature.CONTINUATION_APPLICATION);
  // a type added later simply names the ReleaseFeature (hence phase) it belongs to

  @Override public ReleaseFeature getReleaseFeature() { return releaseFeature; }
}
```

```java
// FeatureFlagService — one generic collection filter, reused by every page
public <T extends PhaseGated> List<T> filterEnabled(Collection<T> options) {
  return options.stream().filter(o -> isEnabled(o.getReleaseFeature())).toList();
}
```

The screen builds its options from the **filtered** set rather than `values()`:

```java
// SelectApplicationTypeService / controller
var enabledTypes = featureFlagService.filterEnabled(List.of(ApplicationType.values()));
var applicationTypeOptions = ApplicationType.getSelectionDisplayOptions(enabledTypes);
```

(i.e. give the `Displayable` options helper an overload that takes a pre-filtered collection
instead of reading `values()` itself.)

**Backstop — the submit path must re-check, not just the render path.** Hiding an option
from the radio list does not stop a crafted POST selecting a not-yet-live type — exactly the
same "hidden ≠ inaccessible" gap the interceptor closes for URLs. So the form validator
rejects any submitted option whose feature is off:

```java
// SelectApplicationTypeFormValidator
if (!featureFlagService.isEnabled(selectedType.getReleaseFeature())) {
  errors.rejectValue("selectedApplicationType", "selectedApplicationType.notAvailable",
      "Select a valid application type");
}
```

#### Same mechanism, whole categories — work-area providers

The work area aggregates rows from several `WorkAreaItemProvider` beans (schedule, schedule +
work-programme, continuation, correction), injected as a `List` and concatenated by
`WorkAreaService`. To hide a **whole category** until its phase — e.g. corrections until LMS2
— make the provider interface `PhaseGated` so each provider declares the feature it belongs
to, and filter the list with the *same* `filterEnabled`:

```java
public interface WorkAreaItemProvider extends PhaseGated {
  List<SearchResultItem> getWorkAreaItems(WorkAreaFilterForm form, ServiceUserDetail user);
  // ReleaseFeature getReleaseFeature();  // inherited from PhaseGated
}
```

```java
@Service
class CorrectionWorkAreaService implements WorkAreaItemProvider {
  @Override public ReleaseFeature getReleaseFeature() { return ReleaseFeature.START_CORRECTION; } // LMS2
  @Override public List<SearchResultItem> getWorkAreaItems(...) { ... }
}
```

```java
// WorkAreaService — filter the providers once, at the aggregator
return featureFlagService.filterEnabled(workAreaItemProviders).stream()
    .flatMap(provider -> provider.getWorkAreaItems(form, user).stream())
    .sorted(comparing(SearchResultItem::transactionDatetime).reversed())
    .toList();
```

When `enable-lms2` is off, `CorrectionWorkAreaService` is filtered out and its rows are never
fetched. The bean stays registered — it is simply skipped — so it remains trivially testable.
Filter **once at the aggregator** rather than making each provider self-guard (`return
List.of()` when off): that keeps the flag check in one place while the *mapping* still lives
on each provider via `getReleaseFeature()`. And because `WorkAreaItemProvider extends
PhaseGated`, a new provider is **forced at compile time to declare a feature** — a category
cannot silently appear unclassified.

> Keep a provider's mapped feature aligned with the interceptor gating of the pages its rows
> link to, so an enabled category never renders a row that leads to a 404. The migration
> assumption (data lands with its phase) covers most of this for whole categories.

This gives a consistent story across granularities, all reading the same `FeatureFlagService`
via one `filterEnabled` / `isEnabled`:

- **The interceptor** gates whole URLs.
- **Layer 2** gates whole in-page elements.
- **`PhaseGated` + `filterEnabled`** gates individual **options** within a page (+ a validator
  re-check on submit) *and* whole **categories** contributed by beans (e.g. work-area
  providers).

Adding a new application type, option, or provider is just a new constant / a new bean naming
its `ReleaseFeature`.

## The allow-list (initial release)

Controllers are classified **by package** (relative to
`uk.co.nstauthority.licensingmanagementservice`). **Every controller not classified is
denied**; a classified controller is reachable only while its phase is on.

**Always available (NOT_FLAGGED):**

- `workarea` — work area (Start button hidden via layer 2)
- `teams` — team management
- `licence.contact` — licence contacts
- `energyportal` — org unit/group REST (`OrganisationUnitRestController`,
  `OrganisationGroupRestController`), **required** by autocompletes on the team-management and
  licence-contact pages
- `feedback`, `fds` (footer), `file` (unlinked file uploads)
- Infrastructure classified NOT_FLAGGED so it is never phase-blocked: `mvc` (`DefaultErrorController`),
  `authentication` (`LogoutController`). Static resources bypass the interceptor entirely
  (non-`HandlerMethod`). SAML ACS / `/login` are filter-chain concerns and never reach the MVC
  handler.
- Dev/test-only controllers, each already `@Profile`-gated: `migration`, `mockups`, `testharness`

**Locked behind `enable-lms1` (LMS1 phase):** everything under `licence.*` **except** the LMS2
packages below — i.e. licence search & management, licence overview, schedules management
(`licence.schedule.*`), schedule & continuation applications
(`licence.scheduleworkprogrammeapplication.*`, `licence.continuation.*`), application letters
(`licence.application.*`), the licence internal search API (`licence.internalapi`), the base
`LicenceController`/`LicenceRedirectorController` (`licence`) — plus `document` (document library).

**Locked behind `enable-lms2` (LMS2 phase — all other functionality):** `licence.correction.*`
and `licence.position`.

> The licence-correction controllers and `LicencePositionController` **already** carry
> `@Profile("enable-lms2")`, so they are unregistered (404) when that profile is off,
> independent of this interceptor — the interceptor's LMS2 entries are the backstop that keeps
> the same phase boundary explicit and auditable in one place. Note the document-library
> controllers are **not** `@Profile`-annotated (they are always registered), so for the LMS1
> phase the interceptor is the *only* thing gating the `document` package — it must be present
> in the policy, mapped to LMS1.

> **Ordering matters.** `licence.contact` (NOT_FLAGGED) and `licence.correction` / `licence.position`
> (LMS2) sit *under* the broad `licence` (LMS1) prefix, so they must precede it in the rule list —
> first match wins. The ArchUnit test (below) asserts every controller resolves to a phase and
> that all three phases are reachable, so a mis-ordering or a new unclassified package fails the
> build rather than silently 404-ing at runtime.

## Testing

- **`FeatureFlagServiceTest`** — for both overloads: `NOT_FLAGGED` always on; each
  `ReleaseFeature` enabled iff its phase's profile is active; `getEnabledFeatures()` returns
  exactly the features whose phase is on. A parameterised test over all `ReleaseFeature`
  values guards against an unmapped constant.
- **`PhasedReleaseInterceptorTest`** — drive `preHandle` with a `HandlerMethod` per phase: an
  NOT_FLAGGED controller (`WorkAreaController`) is always allowed; an LMS1 controller
  (`LicenceSearchController`) throws `NOT_FOUND` with no profiles and is allowed under
  `enable-lms1`; an LMS2 controller (`LicenceCorrectionController`) is blocked under
  `enable-lms1` only and allowed under both; an unclassified controller is blocked; a
  non-`HandlerMethod` handler passes through.
- **`TopNavigationServiceTest`** — extend to assert `LICENCES` and `DOCUMENT_LIBRARY` absent
  unless `enable-lms1`; core three always present.
- **In-page actions** — for a representative role-gated feature (e.g. Start application),
  `isEnabled(ReleaseFeature.START_APPLICATION)` false when `enable-lms1` off even for a user
  with the editor/submitter role, so the model attribute is false and the button is absent
  from rendered HTML. For a phase-only feature, assert its name is absent from
  `enabledFeatures` and the template guard hides it.
- **Collection filtering** — `filterEnabled` returns only members whose phase is on. Options:
  for the start-application screen, with a type's phase off that type is absent from the
  rendered radio options **and** a POST selecting it fails validation
  (`selectedApplicationType.notAvailable`) — render and submit paths must agree. Categories:
  with `enable-lms2` off, `WorkAreaService` yields no correction rows (its provider is filtered
  out); with it on, they appear. A parameterised test over all `PhaseGated` implementations
  (option enums *and* providers) guards against an unmapped constant.
- **`PhasedReleasePolicyTest`** (ArchUnit `ClassFileImporter`, main sources only) — assert
  every `@Controller`/`@RestController` is classified by `PhasedReleasePolicy` (a new,
  unclassified controller fails the build rather than silently 404-ing), and that all three
  phases are reachable from the mapping (catches a typo that drops a phase). Static analysis,
  so `@Profile`-gated controllers are included.
- A test-only base `src/test/resources/application.properties` defines
  `spring.profiles.group.test` and `spring.profiles.group.integration-test` = `enable-lms1,
  enable-lms2`, so both the `@WebMvcTest` slices (`test` profile) and the `@IntegrationTest`
  runs (`development` + `integration-test`) exercise the full app unchanged.

## Risks & edge cases

- **Package-based classification vs. package moves.** Because a controller's phase is derived
  from its package, moving a controller to a different package can silently change its phase.
  The `PhasedReleasePolicyTest` completeness check catches a controller that lands in an
  *unclassified* package (build fails), but a move into an already-classified package of the
  *wrong* phase would pass. Keep controllers in packages that match their phase, and treat the
  `PhasedReleasePolicy` rule list as the source of truth when adding a package.
- **Migrated data is phased with its feature — except licences.** Assumption: data for a
  given area is only migrated in once that area's phase is switched on. The **exception is
  licence records**, which are migrated as part of the **initial release** (they underpin
  licence contacts). For every *other* area — schedules, applications, document templates —
  the initial phase has no migrated data, so the work area cannot surface rows/links into
  locked areas. Consequently the `WorkAreaItemProvider` implementations do **not** need
  per-phase gating for correctness (nothing to show); if you gate them anyway it is
  belt-and-braces, not a requirement. Hold one thing: keep the migration schedule and the
  profile switch in lock-step for the phased areas — turning a profile on before its data
  lands (or vice-versa) reintroduces the mismatch this assumption removes.
- **`enable-lms2` scope broadening.** `enable-lms2` currently means "licence corrections /
  position". Under this scheme it becomes "everything not in Initial or LMS1". Confirm no
  controller currently relies on `enable-lms2` meaning *only* corrections. Document the
  profile matrix per environment (Initial = none, LMS1 = `enable-lms1`, LMS2 =
  `enable-lms1` + `enable-lms2`).
- **Document library has no `@Profile`.** Unlike licence corrections, the document-library
  controllers are always registered, so the allow-list interceptor is the sole gate keeping
  them hidden in the initial phase and switching them on for LMS1. If the `document` package
  mapping is missing or set to the wrong phase, the document library leaks — the completeness
  test guards against it being missing.
- **Interceptor ordering.** Register `PhasedReleaseInterceptor` so it runs early (before
  `AccessHandlerInterceptor` does expensive work) but after authentication is established —
  its position in `addInterceptors()` and reliance on the security filter chain already
  guarantee the latter.

## Rollout

1. Ship the `FeatureFlagService`, `PhasedReleaseInterceptor` + policy, nav filter, and button
   gating — all behaviour-neutral when `enable-lms1` + `enable-lms2` are active (as in the test
   suite and a fully-enabled local dev run).
2. **Initial live release:** run with **neither** profile → only work area, teams, licence
   contacts; Start button hidden.
3. **LMS1 phase:** add `enable-lms1` to the live environment's `SPRING_PROFILES_ACTIVE` →
   schedules management, licence search & management, schedule & continuation applications
   (and the Start button) switch on. No code redeploy — config only.
4. **LMS2 phase:** add `enable-lms2` → all remaining functionality switches on. Once a phase
   is permanently live its flag can be retired — see [Retiring a flag](#retiring-a-flag-once-a-phase-is-permanently-live).

## Retiring a flag once a phase is permanently live

Feature flags are scaffolding, not a permanent fixture. Once a phase has been live in
production long enough that rollback is no longer wanted, its flag should be **deleted**, not
left dormant — dormant flags rot, accumulate untested "off" branches, and mislead the next
reader about what is really toggleable.

Retirement is deliberately cheap because every check goes through a **named enum constant**
(`ReleasePhase.LMS1`, `ReleaseFeature.START_APPLICATION`, …). Delete the constant and the
compiler lists every call site that must be cleaned up — there is no need to grep for magic
strings. Treat that compiler error list as the retirement checklist.

There are two shapes of retirement.

### A. Retiring one phase's flag while later phases remain flagged

E.g. LMS1 is permanently live but LMS2 is still pending. The goal: make everything LMS1
unconditionally available while leaving LMS2 gating intact.

1. **Remove the profile from every environment's config** (`enable-lms1` out of
   `SPRING_PROFILES_ACTIVE`, out of the test-only `spring.profiles.group` in
   `src/test/resources/application.properties`, and from local run configurations). Nothing
   should depend on it being toggleable any more.
2. **Promote — do not delete — the phase's package rules.** In `PhasedReleasePolicy`, change
   every LMS1 rule's phase to `NOT_FLAGGED`. ⚠️ **Do not just remove the rules** — an unclassified
   controller is *denied* under default-deny, so deleting the entry would 404 a now-live area.
   The rule stays in the allow-list; only its phase changes to "always on".
3. **Inline the in-page guards and collection filters.** At each `isEnabled(ReleaseFeature.LMS1_thing)`
   call site (surfaced by the compiler once you delete the constant), drop the flag term so the
   action always renders — keeping any co-existing role/permission check. For a collection whose
   members are **all** the retired phase (e.g. the application-type options list), drop the
   `filterEnabled` call and validator re-check entirely so everything shows. For a **mixed-phase**
   collection (e.g. the work-area providers, spanning LMS1 and LMS2), **keep** the `filterEnabled`
   call — it still gates the remaining LMS2 members — and repoint each retired provider to an
   always-on feature (one mapped to `NOT_FLAGGED`) so it now always passes the filter. Then remove
   the corresponding `ReleaseFeature` constants and any now-dead `enabledFeatures` template guards.
4. **Fold the nav mapping.** Point the phase's `TopNavigationItem`s at `NOT_FLAGGED` (or remove
   the phase term from the filter for them).
5. **Delete `ReleasePhase.LMS1`** once nothing references it. The compiler confirms when it is
   safe.
6. **Update tests.** Drop the "LMS1 controller 404s when profile off" cases; keep the
   `PhasedReleasePolicyTest` "every controller is classified" test — its expectation simply
   shifts the LMS1 packages from LMS1 to NOT_FLAGGED.

The same recipe retires `enable-lms2` later, with one extra step: **remove the
`@Profile("enable-lms2")` annotations** from the licence-correction / position controllers so
those beans are always registered.

### B. Final launch — removing the apparatus entirely

Once *all* phases are permanently live, the whole mechanism is dead weight. Delete, in
dependency order:

1. All `isEnabled(...)` / `filterEnabled(...)` call sites and the `enabledFeatures` model
   attribute in `DefaultPageControllerAdvice` (compiler-guided); drop the `PhaseGated` mappings
   (option enums *and* providers) and the validator re-checks so every option and category shows.
2. `PhasedReleaseInterceptor` and its registration in `WebMvcConfiguration`, then
   `PhasedReleasePolicy`.
3. The phase filter in `TopNavigationService` and the `ReleasePhase` field on
   `TopNavigationItem`.
4. `FeatureFlagService`, `ReleaseFeature`, `ReleasePhase`, `PhaseGated`, and any remaining
   `@Profile("enable-lms*")` annotations.
5. The `enable-lms*` profiles and profile-group config; the phase-flag tests.

The app is left in its naturally-open state, governed only by the normal access-rule
interceptors — as if the flags had never existed.

> **Guard against half-retirement.** Retire a flag in a single, self-contained change, not
> piecemeal. A partially-removed flag — profile gone but package rules still mapped to it, or the
> enum deleted but a template still guarding on a stale attribute — is worse than either
> state. The "delete the enum constant, fix every compiler error, done" discipline keeps each
> retirement atomic.
