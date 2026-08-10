# Browser Component Testing

The GIS framework includes browser-level component tests that verify the map renders correctly in a real Chromium browser. These sit above the existing jsdom unit tests and catch visual and rendering failures that unit tests cannot — for example, a broken feature polygon, a collapsed map height, or a missing canvas element.

## Tools considered

Three approaches were evaluated during the spike on this branch.

### WebdriverIO

Other repositories in this organisation use WebdriverIO for end-to-end testing against a running application. E2E tests were ruled out for this use case because they require the full Spring backend to be running, which is too heavy for component-level rendering checks. WebdriverIO's component testing runner (`@wdio/browser-runner`) was also considered but is less mature than the alternatives below.

LambdaTest (used alongside WebdriverIO in other repos for cross-browser cloud testing) was ruled out for the same reason — it is aimed at E2E against a running app, not component testing.

### Playwright Component Testing

`@playwright/experimental-ct-vue` mounts Vue components in a real browser via Playwright. Network interception uses `page.route()`, which is a single-line server-side intercept to mock HTTP calls made by the vue components.
However, its package name says `experimental` meaning its API is not stable yet.

### Vitest Browser Mode

`@vitest/browser` runs Vitest tests inside a real Playwright-managed Chromium browser rather than jsdom. It is actively developed as part of Vitest core, and keeps the test suite in one framework as we already use vitest for component unit testing.
Network interception uses MSW (Mock Service Worker), which requires a small amount of extra setup compared to `page.route()` but is a well-established standard.

The Vitest's browser framework also has support to run tests on Firefox and webkit (iOS) browsers, which we can add support for if needed in the future.

This is the approach we will be using for the GIS framework.

## What is tested

`BaseMap.vue` is used as the mount target rather than individual child layers. It provides the real OpenLayers map context (canvas, view, tile layer) that child components like `FeatureLayer` and `SnapPointsLayer` need to initialise correctly.
We also mock http requests that would be made to the spring app using MSW to serve static fixtures, such as the esriJson of a feature to render on the feature layer. The OSM tile layer used for the basemap is mocked to serve a single tile png to avoid a dependency on changes made to the map tiles.

Following the [Vitest recommendation](https://vitest.dev/guide/browser/visual-regression-testing.html#organizing-your-tests), functional browser tests and visual regression tests are kept in separate files and run with separate configs so that we only run visual tests on the CI pipeline to have the same environment across tests.

## Styling in the browser tests

The tests can't build the host app's `main.css`, so they load GOV.UK Frontend's prebuilt CSS from a devDependency in
`src/test/resources/js/visual-regression-tests/visual-styles.ts` (the first `setupFiles` entry):

```ts
import "govuk-frontend/dist/govuk/govuk-frontend.min.css";
import "ol/ol.css";
import "vue3-openlayers/vue3-openlayers.css";
```

- Pin `govuk-frontend` to match the version `fivium-design-system-core` uses; keep the two in step or baselines drift.
- Keep it a devDependency out of `src/main` so `vite build` can't pull it into `gis-framework.css`.
- The `gis-visual-tests-govuk-assets` plugin in `vitest.browser.visual.config.ts` serves the GDS Transport fonts from
  `node_modules`, and `visual-styles.ts` awaits `document.fonts.load` so screenshots use the real typeface.
- Components must not use FDS-only (`fds-*`) classes. For styling GOV.UK Frontend lacks (e.g. a link-styled button), use a
  `<style scoped>` block.

## Viewport and scaling

Vitest scales the tester iframe to fit the browser window, so the Playwright window and `test.browser.viewport` must be the same
size — they share one `viewport` const in `vitest.browser.visual.config.ts`. If they differ, screenshots are silently downscaled
and clipped past the shorter dimension.

Keep the height **at or above 834px** so `BaseMap`'s `clamp(18.75rem, 60vh, 31.25rem)` pins to its 500px maximum, above which the
map's size no longer depends on the viewport.

## Visual regression testing

The screenshot test uses Vitest's built-in `toMatchScreenshot` assertion backed by `pixelmatch` for pixel-level diffing. On the first run (or after `--update`), Vitest writes a baseline PNG to:

```text
src/test/resources/js/visual-regression-tests/__screenshots__/...
```

On subsequent runs it diffs the live render against that baseline. If the difference exceeds `allowedMismatchedPixelRatio: 0.01` (1% of pixels) the test fails.

Baseline files are committed to the repository. The filename includes the browser and platform (e.g. `base-map-single-block-chromium-linux.png`) so that images generated across different environments do not fail the tests.

## Updating baselines

When a change intentionally affects how the map renders (colour change, new feature geometry, layout adjustment), the committed baseline needs to be regenerated. This is done via a Docker Compose service that runs the same `node:24.16.0` Linux environment as the drone build, ensuring the generated images have the correct `-linux` filename suffix.

### Running the Docker Compose service

Do this if you need to regenerate the baseline screenshots used on the drone build.

From the `gis-framework` directory, run:

```bash
docker compose -f devtools-gis-framework/update-screenshots-compose.yml run --rm update-gis-screenshots
```

Or via the IDE navigate to `gis-framework/devtools-gis-framework/update-screenshots-compose.yml` and run the script with the green arrow.

The service will:

1. Install Node dependencies (cached in a named Docker volume — fast on repeat runs)
2. Install Chromium via Playwright (also cached)
3. Delete all existing baseline PNGs
4. Regenerate all baselines via `test:vitest:visual:update`

Once the container exits, review the new files under `src/test/resources/js/visual-regression-tests/__screenshots__/` and commit them to your branch.

## Running the tests locally

These tests are meant to be run only on the drone pipeline as different local environments may render the map differently and therefore fail the tests.
However, you can still run them locally by following the steps below.

1. Commit your changes and checkout the `develop` branch
2. Run the following commands to generate your local baseline images:

```bash
# Vitest visual regression tests
cd gis-framework
npm run test:vitest:visual:update
```

3. You should now have a new baseline image in `src/test/resources/js/visual-regression-tests/__screenshots__/..`. Do not commit these images.
4. Check out your feature branch and run the visual tests with:

```bash
# Vitest visual regression tests
cd gis-framework
npm run test:vitest:visual
```

5. You can run `npx vite preview --outDir html` after the tests run to see the test report in a browser. Alternatively, you can look at the logs to see where the generated diff images are.
6. Delete the local baseline and diff images once you are done, do not commit them to the repo. (They will have a `-win32.png` suffix)

## CI

The Vitest browser suite runs in a dedicated Drone step on every push and pull request:

- `gis-frontend-visual-tests` — runs `test:vitest:visual`

The step uses `node:24.16.0` and installs Chromium via `npx playwright install --with-deps chromium`.

Visual regression failures in CI mean a committed baseline no longer matches what Chromium renders. You can see the diff images on the test report generated by drone. Use the docker compose file to regenerate the baseline images if the changes were intentional.

Visual tests are not run as part of the normal build java backend step, as we intend to keep them separate as per the Vitest guidance already mentioned.
