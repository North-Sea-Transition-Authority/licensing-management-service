import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import { SupportedWkid } from "@/coordinate-system-utils";
import MapActionsPage from "@/pages/MapActionsPage.vue";
import depthMapEd50 from "../fixtures/depthMapEd50.esriJson.json";
import depthMapOutlineNodes from "../fixtures/depthMapEd50.outlineNodes.json";
import { worker } from "./setup";
import { settleForScreenshot, waitForMapFullyLoaded } from "./visual-test-util";

const textualDescriptionHtml = `<div class="gis-textual-description">
 <p class="govuk-body">The area is bounded by the following coordinates:</p>
 <ol class="govuk-list govuk-list--number">
 <li class="govuk-!-font-tabular-numbers">1E 1N</li>
 <li class="govuk-!-font-tabular-numbers">2E 2N</li>
 </ol>
</div>`;

function mockEndpoints() {
  worker.use(
    http.get("/api/features/1", () => HttpResponse.json(depthMapEd50)),
    http.get("/api/outline-nodes/1", () => HttpResponse.json({ featureOutlineNodes: depthMapOutlineNodes })),
    http.get("/api/textual-description/1", () => HttpResponse.json({ textualDescription: textualDescriptionHtml })),
  );
}

function renderInFixedWidthContainer() {
  const container = document.createElement("div");
  container.style.width = "900px";
  document.body.appendChild(container);

  return render(MapActionsPage, {
    props: {
      commandJourneyId: "1",
      srsWkid: SupportedWkid.ED50_WKID,
      featuresBaseUrl: "/api/features",
      outlineNodesBaseUrl: "/api/outline-nodes",
      textualDescriptionUrl: "/api/textual-description",
      includeNstaQuadrants: false,
      includeNstaBlocks: false,
    },
    global: { plugins: [OpenLayersMap] },
    container,
  });
}

async function waitForPageLoaded() {
  await waitForMapFullyLoaded();
  await expect.poll(() => document.querySelector(".gis-textual-description")).not.toBeNull();
  await expect.poll(() => document.querySelectorAll("input[type=radio]").length).toBeGreaterThan(0);
  await expect.poll(() => document.querySelectorAll(".depth-marker").length).toBe(3);
}

describe("depth actions page", () => {
  it("renders the shallowest depth band by default", async () => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();

    await waitForPageLoaded();
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("depth-actions-shallowest-band");
  });

  it.each([
    { band: "-100 to -200", screenshot: "depth-actions-middle-band" },
    { band: "-200 to -infinity", screenshot: "depth-actions-deepest-band" },
  ])("renders the $band band when it is selected", async ({ band, screenshot }) => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();

    await screen.getByText(band, { exact: true }).click();
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot(screenshot);
  });

  it("shows the error when continuing without selecting an action", async () => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();

    await screen.getByRole("button", { name: "Continue" }).click();
    await expect.element(screen.getByRole("link", { name: "Select what action you would like to perform" })).toBeVisible();
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("depth-actions-no-action-selected");
  });
});
