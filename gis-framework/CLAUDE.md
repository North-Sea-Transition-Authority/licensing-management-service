# CLAUDE.md

Guidance for Claude Code when working in the `gis-framework` subproject. The repository root has its own
`CLAUDE.md` covering the parent LMS application.

## What this is

A Spring Boot **starter / library** (group `uk.co.fivium`), not an executable app. It provides reusable
GIS capability — feature persistence, geometry operations, and map UI — to consuming apps such as LMS.

It is an included Gradle subproject but is designed to be separable, so it must **never depend on
`uk.co.nstauthority.licensingmanagementservice`**. LMS depends on this package, not the reverse. Keep APIs
generic and GIS-oriented; compose LMS-specific behaviour outside this package. An ArchUnit test guards
this boundary.

## Architecture

Three parts, two runtimes:

| Part | Location | Role |
| --- | --- | --- |
| Java starter | `src/main/java/uk/co/fivium/gisframework` | JPA entities, services, and a gRPC **client**. Auto-configured into the host app. |
| Vue frontend | `src/main/resources/js` + `templates/gis` | Vue 3 + OpenLayers components, Vite-built and packaged into the starter jar. |
| ArcGIS node server | `arcgis-node/` | Separate Node/TypeScript process. A gRPC **server** on `:8082` that does the geometry maths. |

The Java side never calculates geometry itself — it persists shapes and delegates every calculation over
gRPC. Java packages: `feature/` (domain), `grpc/` (client facade), `operator/` (split pipeline),
`migration/` (Oracle → Postgres, profile-gated), `configuration/` (auto-configuration).

### Domain model (`feature/`)

`Feature` → `Polygon` → `Line`, all JPA entities, all `@Audited` (Hibernate Envers), persisted to
PostgreSQL. `Feature` holds a `jsonb` `attributes` map, a `CoordinateSystem` (`ED50`, `ETRS89`,
`BRITISH_NATIONAL_GRID`), a self-referential `parentFeature` (sub-areas nest under their block) and a
`legacyId` back to the Oracle source. `Line` stores its geometry as **EsriJSON** plus `ringNumber`,
`ringConnectionOrder` and a `LineNavigationType` (`CARTESIAN` / `GEODESIC` / `LOXODROME`) — these drive
polygon reconstruction and area calculation on the node side.

`EntityBackedFeature` pairs a `Feature` with its pre-fetched polygon → lines map to avoid lazy-loading
issues, and is the unit passed to gRPC calls. Coordinate-system ↔ WKID conversion lives in
`CoordinateSystemUtils` (Java) and `coordinate-system-utils.ts` (frontend and node).

### Split pipeline (`operator/`)

`SplitOperatorService` → `OperatorResultProcessingService`: explode the resulting polygons into lines,
match them to parent lines, assign ring numbers and connection orders, rotate each ring to start at the
northwest-most line (canonical ordering), validate the reconstruction against the original, copy parent
attributes, persist. Every step is a gRPC call.

### Auto-configuration

Registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

- `GisFrameworkAutoConfiguration` — component-scans the package and enables configuration properties.
- `EntityAutoConfiguration` — registers JPA entity packages, **excluding `migration`**, so host apps pick
  up GIS entities without needing Oracle on the runtime classpath.
- `GisFrameworkFlywayConfiguration` — runs the framework's own migrations from
  `classpath:db/gis-framework-migration` against its own history table
  `gis_framework_flyway_schema_history`. The non-default location is deliberate: at the default location
  the starter's migrations would be shaded into the host app's and applied to its schema.

## gRPC — the Java ↔ node contract

`.proto` files in `src/main/proto` are the single source of truth for both runtimes. `ArcGisJs.proto`
declares the `ArcGisService`: one RPC per operation (`splitPolygon`, `buildPolygon`, `explodePolygon`,
`findParentLines`, `getLineStartAndEndPoints`, `findNorthwestMostLine`,
`validatePolygonReconstructionFromPolylines`, `calculateArea`, plus the migration RPCs). Each request and
response message lives in its own file under a per-operation directory.

`GrpcClientService` (`grpc/`) is the only Java-side entry point — a `@GrpcClient("node-server")` blocking
stub whose address is set in `application.properties`
(`grpc.client.node-server.address: static://localhost:8082`). Each of its methods maps to one handler
registered in `arcgis-node/src/grpc-server.ts`, implemented under `src/handlers` (or
`src/migration/handlers`), with the geometry logic in `src/geometric-operators`.

To change the wire protocol:

1. Edit the `.proto`, following <https://protobuf.dev/best-practices/dos-donts/>. In particular: never
   renumber or reuse a field number, never change a field's type, prefer adding a new field over renaming
   one, and `reserved` any retired numbers and names.
2. Regenerate **both** sides — Java: `./gradlew :gis-framework:generateProto`; TypeScript:
   `npm run proto-gen` in `arcgis-node`.
3. Update the Java client and the node handler in the same change, with tests on both sides.

**Never hand-edit generated output** (`build/generated` for Java, `arcgis-node/generated` for TypeScript).

## ArcGIS JS SDK

`@arcgis/core` is Esri's JavaScript SDK and the actual geometry engine. It is a browser-oriented library
with a WASM core, which is why it runs in node rather than the JVM — that constraint is the whole reason
the gRPC bridge exists. `arcgis-node/src/geometric-operators` wraps its operators (cut,
multipart-to-singlepart, union, contains, densify, …) and `src/util/esrijson-util.ts` converts between
EsriJSON strings and SDK geometry objects.

Two consequences worth remembering:

- The SDK has no *set* or *relationship* operators for geodesic shapes. Geodesic lines are therefore
  **densified** into many short loxodrome segments and stored densified, while still flagged `GEODESIC`.
  Dense points cascade from root blocks down to children so shared edges stay coincident.
- Its WASM assets are served locally by a small Express server on `:3000` from `arcgis-node/public/assets`
  (populated by `npm run copy:core`) rather than from Esri's CDN.

The frontend map is **OpenLayers**, not ArcGIS. The SDK is server-side only.

## Commands

Java — from the repo root or `gis-framework/` (both have a `gradlew`):

```bash
./gradlew :gis-framework:build            # compile + test + frontend build + jar
./gradlew :gis-framework:test             # Java tests (runs frontend + node lint first)
./gradlew :gis-framework:test --tests "uk.co.fivium.gisframework.feature.LineServiceTest" --rerun
./gradlew :gis-framework:check            # test + testFrontend
./gradlew :gis-framework:checkstyleMain   # 0 warnings; generated proto excluded
./gradlew :gis-framework:generateProto    # regenerate Java proto/gRPC stubs only
```

The `checkstyleTest` task is not required — don't run it.

Frontend and full regen — from `gis-framework/`:

```bash
npm run build-all            # proto:java (via gradlew) + Vite build + arcgis-node install/setup
npm run build                # vue-tsc --noEmit + vite build → src/main/resources/public/gis/dist
npm run test                 # Vue unit tests (Vitest + jsdom)
npm run test:vitest:visual   # visual regression suite — read Testing below before running
npm run lint                 # ESLint over frontend + arcgis-node (lint:fix to auto-fix)
npm run dev                  # Vite dev server on :5173 for HMR
```

Gradle wrappers that use the pinned Node 24.16.0 / npm 10.9.0 (no system node needed):
`installFrontendDependencies`, `buildFrontend`, `testFrontend`, `lintFrontendAndArcGisNode`.
`processResources` depends on `buildFrontend`, so a plain `build` packages the Vite assets into the jar.

ArcGIS node server — from `gis-framework/arcgis-node/`:

```bash
npm install && npm run setup   # setup = copy:core (ArcGIS assets) + proto-gen (TS types), in parallel
npx tsx src/grpc-server.ts     # start the gRPC server (:8082) and asset server (:3000)
npm run test                   # Vitest + V8 coverage
npm run lint                   # lint:fix to auto-fix
```

Running the app: this starter is not executable. Start the LMS app from the repo root
(`./gradlew bootRun`, or an IntelliJ Spring run configuration) **and** the node server above — without the
node server every geometry call fails. For live Vue reloading, add the `localdev-vue-hmr` profile and run
`npm run dev`; otherwise rebuild the frontend and restart after Vue or Java changes. Husky runs lint on
commit.

## Testing

- **Java** — `src/test/java/uk/co/fivium/gisframework`. Unit tests use `@ExtendWith(MockitoExtension.class)`
  with strict stubs; integration tests use Testcontainers PostgreSQL; ArchUnit tests in `architecture`
  guard the dependency boundary. Method names: `methodName_whenCondition_assertExpectedBehaviour`.
- **Vue unit** — `src/test/resources/js/**/*.test.ts` (Vitest + jsdom + Testing Library). Query by
  accessible role or label rather than CSS selectors, so markup changes don't break tests.
- **arcgis-node** — its own Vitest suite with coverage.
- **Geometry regresses easily.** When touching coordinate-system conversion, line or polygon operations, or
  migration validation, add boundary cases on **both** the Java and node sides — rings with holes,
  self-touching boundaries, and each supported coordinate system.
- **Visual regression** — `src/test/resources/js/visual-regression-tests/*.visual.test.ts`, separate config,
  real Chromium via Playwright, `toMatchScreenshot` backed by pixelmatch. **Do not run these locally to
  verify a change, and never commit their output.** Committed baselines carry a `-linux` suffix and are
  generated for CI only; running on Windows finds no matching baseline and *writes* throwaway `-win32`
  PNGs under `__screenshots__/**` (not gitignored — delete them). Regenerate baselines only when a change
  actually alters what the map renders — test-only refactors leave the render identical — using
  `docker compose -f devtools-gis-framework/update-screenshots-compose.yml run --rm update-gis-screenshots`.
  Drone is the source of truth. See `documentation/browser-component-testing.md`.
- Avoid `any` in production TypeScript; tests may use it freely.

## Frontend standards

- Markup uses GDS classes (`govuk-form-group`, `govuk-input`, `govuk-error-message`, `govuk-summary-card`)
  so components inherit the host app's FDS stylesheet — the starter ships no bespoke design system.
- Errors follow the GDS pattern: `govuk-form-group--error` + `govuk-input--error` + a visible
  `govuk-error-message`; label every input.
- Scoped `<style>` blocks only for map / OpenLayers layout.
- Components are mounted by `gis-all.ts`, which discovers DOM mount points by `data-gis-component`
  attribute. Host apps do **not** compile this Vue source — they import the FreeMarker macros under
  `classpath:/templates/gis` and serve the built assets from the jar.
- Host app requirements: `<#include "../../gis/gisAssets/gisAssets.ftl">` in its `layout.ftl`, serve
  `/gis/**` from `classpath:/public/gis/`, and permit `/gis/**` in security and interceptor config.
- `src/main/resources/public/gis/dist` is build output — never edit it by hand.

## GIS migration (`migration/`)

A one-off port of the legacy Oracle GIS schema into `Feature`/`Polygon`/`Line`. Everything is gated behind
`@Profile("gis-migration")` and `oracle/` uses a separate datasource (`OracleDatasourceConfiguration`,
`ojdbc11`). Requires `ORACLE_DB_URL`, `ORACLE_DB_USERNAME` and `ORACLE_DB_PASSWORD`; ordinary unit tests
must never need Oracle connectivity. Keep migration code inside these packages.

Driven by `GisMigrationEndpoint`, an Actuator endpoint: `POST /actuator/gismigration` runs the pipeline,
`DELETE` clears the GIS tables down. The Oracle PL/SQL preprocessing scripts in `migration/oracle-scripts`
must be run first.

The pipeline is strictly ordered by migration-order number so densified geodesic points cascade from root
blocks downwards: root blocks (10) → redefinition points (15) → block changes (20) → sub-areas (30) →
block and sub-area validation → retention areas (40) → retention validation → reference blocks (50) →
reference-block validation. `MigrationService` handles everything except reference blocks, which have
`ReferenceBlockMigrationService` (treats cartesian lines as geodesic). `MigrationValidationService` covers
parent/child containment, sub-areas being topologically equal to their parent block, retention areas
falling within the union of their linked licence blocks, and reference blocks. Known-bad Oracle shapes are
hard-skipped, and `BrokenBlockConfigurationProperties` (`gis-framework.migration.*` in
`application.properties`) excludes broken reference-block ↔ licence-block pairs from validation.

Read `documentation/gis-migration.md` before changing any of this — it documents the ordering rationale,
the validation rules, and the known data oddities.

## Documentation (`documentation/`)

Read the relevant document before starting work:

| Read when | Document |
| --- | --- |
| Changing a Vue component, adding a macro, or wiring the starter into a host app | `frontend-components.md` |
| A change could alter what the map renders, or a visual test failed | `browser-component-testing.md` |
| Working on the migration, a validation failure, or geodesic densification | `gis-migration.md` |
| Rendering features, coordinate systems, or reprojection on the map | `displaying-features-on-map.md` |
| Adding or securing a REST endpoint in the starter | `rest-endpoint-authentication.md` |
| Touching snap-point spacing or zoom-dependent grids | `dynamic-snap-point-spacing-spike.md` (spike) |
| Resolving shape attribute or config differences between layers | `shape-data-standardisation.md` (spike) |

Diagrams live in `documentation/drawio` and `documentation/images`. The two spikes record decisions and
may not reflect current behaviour.

## Conventions

- Java 21; packages under `uk.co.fivium.gisframework`; role-suffixed class names (`PolygonService`,
  `OracleShapeRepository`). Checkstyle config `devtools-lms/checkstyle.xml`, zero warnings tolerated.
- TypeScript ESM; ESLint (`@antfu/eslint-config`) is also the formatter — run `npm run lint:fix`.
- Pino for node logging, SLF4J for Java.
