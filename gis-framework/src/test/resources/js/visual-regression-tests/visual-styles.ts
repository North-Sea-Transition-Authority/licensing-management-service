/** Loads GOV.UK Frontend CSS from a devDependency so govuk-* components render styled in tests. */
import { beforeAll } from "vitest";
import "govuk-frontend/dist/govuk/govuk-frontend.min.css";
import "ol/ol.css";
import "vue3-openlayers/vue3-openlayers.css";

beforeAll(async () => {
  // govuk-frontend only hides conditional reveals under this class, which host app layouts add
  document.body.classList.add("govuk-frontend-supported");
  // Wait for both font weights so screenshots capture GDS Transport, not the fallback.
  await Promise.all([
    document.fonts.load("400 19px \"GDS Transport\""),
    document.fonts.load("700 19px \"GDS Transport\""),
  ]);
});
