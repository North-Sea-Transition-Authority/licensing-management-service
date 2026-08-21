---
name: writing-tests
description: >
  Test conventions for this codebase — naming, setup/act/assert structure, whole-object
  assertions, TestUtil entity builders, mocking rules, and accessibility-first vitest
  queries. Use this whenever writing, reviewing, refactoring, or fixing ANY test in this
  repo (JUnit/Mockito unit tests, integration tests, or vitest frontend tests), including
  when the user just says "add a test", "why is this test failing", or asks you to change
  existing test code. Consult it before writing the first assertion, not after.
---

# Writing tests

## All tests

- Structure every test as setup -> act -> assert. Keeping the three phases visually
  distinct (blank lines between them) is what makes a failing test readable at a glance
  six months later.
- Naming:
  - When a method needs more than one test, name each one
    `methodName_whenSomethingHappens_thenThisShouldHappen`.
  - When a method needs only one test, give the test the same name as the method.
- Do not add comments unless absolutely necessary. A well-named test with clear
  setup/act/assert phases already says what it does; a comment restating that just goes
  stale.
- Test the forks in logic, not the plumbing. If a method is simple — a service method
  wrapping a repository call, say — one test is enough. Writing separate "when something
  is returned" and "when nothing is returned" tests there asserts that Mockito returns
  what you told it to, which tests the framework rather than your code and adds a
  maintenance cost to every future refactor. Spend tests on branches, boundaries, and
  error paths.
- When the same behaviour needs checking against several values, use a parameterized test
  rather than copy-pasted near-identical methods — the intent (this rule holds across
  these inputs) becomes explicit, and adding a case is a one-line change.
- Assert on the whole returned object, not a handful of fields. Build an expected object
  and compare against it. Field-by-field assertions quietly stop covering anything when a
  new field is added, so the test passes while the new field is wrong.
- Avoid catch-all argument matchers when mocking. `any()` keeps matching after the
  production code starts passing the wrong argument, so the test stops protecting you.
  Reach for it only when the argument genuinely can't be constructed in the test, or is
  irrelevant to what you're asserting — and prefer a narrower matcher over `any()` where
  one exists.
- A test isn't finished until it has been run. Verify with
  `./gradlew :test --tests "uk.co.nstauthority.licensingmanagementservice.some.package.MyTest" --rerun`.
  `checkstyleTest` is not required.

## Java tests

- Choose the cheapest test that covers the behaviour. Default to a unit test with
  `@ExtendWith(MockitoExtension.class)`. Use `@IntegrationTest` (Spring context +
  Testcontainers PostgreSQL) only when the behaviour under test is the wiring itself —
  interceptor rules, argument resolvers, Flyway migrations, real query behaviour.
- Before hand-building an entity, look for an existing `EntityNameTestUtil.java` and use
  it. These builders keep test setup short and keep every test in step when the entity
  gains a required field.
- Compare whole objects with `.usingRecursiveComparison()`, `.isEqualTo(...)` or similar:

  ```java
  // Prefer this
  var expected = new ScheduleState(currentTerm, currentPhase, nextTerm, null);
  assertThat(result).usingRecursiveComparison().isEqualTo(expected);

  // Not this — silently stops covering anything when ScheduleState gains a field
  assertThat(result.currentTerm()).isEqualTo(currentTerm);
  assertThat(result.currentPhase()).isEqualTo(currentPhase);
  ```

- Mockito runs with strict stubs. If the method under test calls
  `mock.getAttribute(X)` and `mock.getAttribute(Y)`, stub both explicitly — stubbing only
  the one you care about raises `PotentialStubbingProblem` rather than returning null.
  When a stub genuinely isn't reached on every path, prefer restructuring the test over
  loosening strictness.

## TypeScript/JavaScript vitest

- Write new test files in TypeScript (`.ts`/`.tsx`). Existing `.js` test files don't need
  converting — leave them as they are.
- Query and assert on what a user can see or reach through accessibility, not on
  implementation details. Prefer `screen.findByText` / `screen.getByRole` /
  `screen.getByLabelText` (and their `queryBy*` variants for absence checks), targeting
  visible text, roles, labels, and legends. Tests written this way survive refactors of
  the markup and fail when the user-facing behaviour actually breaks.
- Treat `data-testid` / `queryByTestId` and other test-only markers as a very last
  resort — only when there is genuinely no user-visible text, role, or accessible name to
  target. Reaching for a testid is usually a sign the test is coupled to implementation
  detail; render the real component and assert on what a user perceives instead. It can
  also be a hint that the component is missing an accessible name it ought to have.
