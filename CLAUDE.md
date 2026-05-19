# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew :test --tests "uk.co.nstauthority.licensingmanagementservice.some.package.MyTest" --rerun

# Checkstyle (0 warnings permitted, fails the build)
./gradlew checkstyleMain checkstyleTest

# Coverage report (auto-runs after test)
./gradlew jacocoTestReport

# Executable JAR
./gradlew bootJar
```

The `gis-framework` subproject builds automatically as part of the root build.

## Architecture

### Overview

Spring Boot 3 web application (Java 21) for managing energy licence applications. Uses SAML2 authentication against the Energy Portal IDP, PostgreSQL with Flyway migrations, and Freemarker templates for server-side rendering. A `gis-framework` subproject provides geospatial support via a gRPC bridge to a Node.js/ArcGIS server.

### Access Control — Interceptor Rule System

The most distinctive pattern in the codebase. `AccessHandlerInterceptor` is a Spring MVC interceptor that chains through all `AccessInterceptorRule` beans (ordered via `@Order`). Each rule declares which annotation it `supports()` and is invoked when that annotation is present on the controller class or method. Rules return a `SecurityRuleResult` — either continue or fail with an HTTP status.

This means access rules are declared as annotations on controller methods rather than in security config:

```java
@GetMapping("/some-path")
@HasAnyRole(roles = {Role.SCHEDULE_ADMIN})
@ContinuationApplicationHasStatus(statuses = {SUBMITTED})
public ModelAndView myMethod(...) { ... }
```

New access rules require: an annotation type, an `AccessInterceptorRule` implementation, and `@Order` to control evaluation sequence.

### Authentication

SAML2 service provider. The SAML response is parsed by `SamlResponseParser` into a `ServiceUserDetail` record (holds `wuaId`, `personId`, name, email, and optional proxy user fields), which becomes the principal in a `ServiceSaml2Authentication` token.

`UserDetailService.getUserDetail()` is the standard way to retrieve the current user throughout the application — it wraps `SecurityContextHolder` and throws `InvalidAuthenticationException` if unauthenticated. In `@Controller` methods, `ServiceUserDetail` can also be injected directly as a parameter via `ServiceUserDetailArgumentResolver`.

### Path Variable Entity Resolution

Controllers can declare domain entities directly as method parameters — `@PathVariable` resolution is handled by `HandlerMethodArgumentResolver` implementations in the `endpointvalidation` package. For example, `ScheduleWorkProgrammeApplicationDetail` is resolved automatically from `{scheduleWorkProgrammeApplicationDetailId}` in the URL, rather than being fetched manually in each controller method. Add a new resolver by implementing `HandlerMethodArgumentResolver` and registering it in `WebMvcConfig`.

### Forms and Validation

Each form step uses a paired Form POJO + Validator class. Validators implement Spring's `Validator` interface:

```java
public class MyFormValidator implements Validator {
    @Override public boolean supports(Class<?> clazz) { return MyForm.class.equals(clazz); }
    @Override public void validate(Object target, Errors errors) { ... }
}
```

Controllers inject the validator, bind the form with `@ModelAttribute("form")`, and call `validator.isValid(form, bindingResult)` on POST — returning the same view on failure or redirecting on success. Conditional validation (e.g. field X is only required when field Y has a certain value) is handled in the validator, not via annotations. Custom composite input types like `ThreeFieldDurationInput` have their own validation utilities (e.g. `ThreeFieldDurationValidationUtil`).

### Displayable Enum Pattern

Enums used in dropdowns or radio buttons implement `Displayable`:

```java
public enum MyEnum implements Displayable {
  OPTION_ONE("Option one", 1);

  private final String displayName;
  private final int displayOrder;

  @Override public String getDisplayName() { return displayName; }
  @Override public int getDisplayOrder() { return displayOrder; }

  public static Map<String, String> getOptions() {
    return DisplayableEnumOptionUtil.getDisplayableOptions(MyEnum.class);
  }
}
```

`DisplayableEnumOptionUtil.getDisplayableOptions()` returns a `LinkedHashMap<enumName, displayName>` sorted by `displayOrder`, ready to pass to a Freemarker template. Use `getDisplayableOptionsWithDescription()` when the enum also implements `DisplayableEnumWithDescription`.

### Application Journey Pattern

Multi-step application workflows (e.g. schedule work programme amendments, licence continuations) follow a consistent structure:

- A root `Application` entity holds immutable data; a paired `ApplicationDetail` entity tracks mutable state (status, timestamps, responsible org).
- Each journey step is a controller in a subpackage (e.g. `amendjourney/`, `startjourney/`). Forward/back navigation uses `ReverseRouter.route(on(ControllerClass.class).method(...))`.
- Status transitions are explicit — controllers annotated with `@SomeApplicationHasStatus(statuses = {DRAFT})` enforce that an application must be in the right state before a step is accessible.
- A task list controller (`ScheduleWorkProgrammeApplicationTaskListController`) tracks section completion by querying per-section submission services.
- Summary pages are built entirely by service methods that return immutable view objects, keeping controllers thin.

### Work Area Pattern

`WorkAreaItemProvider` is the interface all work area sections implement:

```java
List<SearchResultItem> getWorkAreaItems(WorkAreaFilterForm form, ServiceUserDetail user);
```

Each implementation fetches its domain's applications by status, applies access/filter checks (using `FilterUtil` and `ApplicationAccessService`), then maps to `SearchResultItem` via `ReverseRouter`. The URL and caption text typically vary by status — DRAFT items link to the task list and show "Created {date}", while submitted/post-submission items link to the overview and show "Submitted {date}". Filter state across requests is held in `WorkAreaSession` (`@SessionAttributes`).

### Entity Duplication System

Used when creating new draft versions of licence schedule details and similar versioned entities. The `duplication` package provides:

- `@DuplicateThisOnUpdate` — marks a repository method that fetches child entities to be duplicated
- `DuplicationSource<T>` — marker interface for repositories participating in duplication
- `DuplicationUtil.copyProperties(source, target, fieldsToIgnore...)` — field-level copy using reflection (filters by **field name**, not setter name)
- `ScheduleDetailLinked<D>` — interface implemented by child entities (e.g. `LicenceScheduleRate`, `LicenceScheduleTerm`) that provides `setLicenceScheduleDetail(D)`, used by the duplication service to relink copied entities without reflection-based method lookup

### Licence Schedule and ScheduleState

`LicenceScheduleService.getScheduleState(licenceScheduleDetail)` returns a `ScheduleState` record with `currentTerm`, `currentPhase`, `nextTerm`, `nextPhase` — any of which may be null. "Current" means today falls within the entity's `startDate`/`endDate` range (inclusive start, exclusive end) using an injected `Clock` for testability. Schedule details have a status lifecycle: DRAFT → ACTIVE → REPLACED/DELETED.

`WorkProgrammeActivity` due dates resolve differently depending on `dateOption`: `WITHIN_A_TERM` and `WITHIN_A_PHASE` use the relevant entity's `endDate`; `RELATIVE_DATE` references an `EventReference` entity (a separate table, one row per anchored event on a `LicenceSchedule`) plus a duration offset.

### Persistence

Flyway manages the schema (migrations in `src/main/resources/db/migration`). Hibernate Envers provides audit trails — `AuditRevisionListener` stamps each revision with the current user's `wuaId`. Spring Session persists to PostgreSQL (JDBC). ShedLock coordinates scheduled tasks across instances.

### Freemarker Templates

Templates live under `src/main/resources/templates/`. The `lms/` subtree holds application templates; `fds/` holds Foundation Design System base components. All pages extend the `defaultPage` macro in `lms/layout/layout.ftl`, which accepts parameters for title, heading, breadcrumbs, back link, error summary, and page size. Reusable macros are in `lms/macros/` (prefixed with `_`) and `lms/component/`. Controllers add template variables via `ModelAndView.addObject()`.

### Testing

- **Unit tests** use `@ExtendWith(MockitoExtension.class)` with strict stubs (the default). When stubbing `mock.getAttribute(X)` and the code also calls `mock.getAttribute(Y)` in the same method, stub both explicitly to avoid `PotentialStubbingProblem`.
- **Integration tests** are annotated with `@IntegrationTest` (a meta-annotation that sets up `@SpringBootTest` with `development` + `integration-test` profiles and a Testcontainers PostgreSQL instance).
- **Architecture tests** use ArchUnit in the `architecture` package.
- Test method naming convention: `methodName_whenCondition_assertExpectedBehaviour`.
