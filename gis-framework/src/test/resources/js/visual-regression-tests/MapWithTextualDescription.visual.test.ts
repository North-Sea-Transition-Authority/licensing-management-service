import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import MapWithTextualDescription
  from "../../../../main/resources/js/components/textualDescription/MapWithTextualDescription.vue";
import { SupportedWkid } from "../../../../main/resources/js/coordinate-system-utils";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import { worker } from "./setup";
import { waitForMapFullyLoaded } from "./visual-test-util";

const textualDescriptionHtml = `<div class="gis-textual-description">
 <p class="govuk-body">The area is bounded by the following coordinates:</p>
 <ol class="govuk-list govuk-list--number">
 <li class="govuk-!-font-tabular-numbers">1E 1N</li>
 <li class="govuk-!-font-tabular-numbers">2E 2N</li>
 </ol>
</div>`;

function mockEndpoints() {
  worker.use(
    http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
    http.get("/api/outline-nodes", () => HttpResponse.json({ featureOutlineNodes: [] })),
    http.get("/api/textual-description", () => HttpResponse.json({ textualDescription: textualDescriptionHtml })),
  );
}

const baseProps = {
  featuresUrl: "/api/features",
  outlineNodesUrl: "/api/outline-nodes",
  textualDescriptionUrl: "/api/textual-description",
  srsWkid: SupportedWkid.ED50_WKID,
  includeNstaQuadrants: false,
  includeNstaBlocks: false,
} as const;

// Render into a fixed-width container so each aspect-ratio square stays smaller than the test viewport
// height (1280x800). Without this the component fills the full 1280px viewport width; the vertical
// square would then be 1280px tall — taller than the viewport — so OpenLayers only paints the visible
// portion and the rest of the baseline is blank.
//
// The horizontal layout splits its width across two side-by-side panels, so it needs roughly double
// the width of the vertical layout to give each square a comparable size. Sizing them this way keeps
// the map square consistent between both baselines and stops the vertical description panel from
// becoming an oversized, mostly-empty square stacked under the map.
//
// The container is torn down by cleanup() in setup.ts. screen.locator resolves to this container, so
// the screenshot captures the bounded layout.
function renderInFixedWidthContainer(layout: "horizontal" | "vertical") {
  const container = document.createElement("div");
  container.style.width = layout === "horizontal" ? "700px" : "350px";
  document.body.appendChild(container);

  return render(MapWithTextualDescription, {
    props: { ...baseProps, layout },
    global: { plugins: [OpenLayersMap] },
    container,
  });
}

describe("map with textual description", () => {
  it("renders the horizontal layout", async () => {
    mockEndpoints();

    const screen = renderInFixedWidthContainer("horizontal");

    await waitForMapFullyLoaded();

    await expect(screen.locator).toMatchScreenshot("map-with-textual-description-horizontal");
  });

  it("renders the vertical layout", async () => {
    mockEndpoints();

    const screen = renderInFixedWidthContainer("vertical");

    await waitForMapFullyLoaded();

    await expect(screen.locator).toMatchScreenshot("map-with-textual-description-vertical");
  });
});
