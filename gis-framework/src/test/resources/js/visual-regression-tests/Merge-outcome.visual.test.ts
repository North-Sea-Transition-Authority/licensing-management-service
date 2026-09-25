import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import { page } from "vitest/browser";
import OpenLayersMap from "vue3-openlayers";
import { SupportedWkid } from "@/coordinate-system-utils";
import MergePage from "@/pages/MergePage.vue";
import mergeResultEd50 from "../fixtures/mergeResultEd50.esriJson.json";
import splitResultEd50 from "../fixtures/splitResultEd50.esriJson.json";
import { worker } from "./setup";
import { settleForScreenshot, waitForMapFullyLoaded } from "./visual-test-util";

/**
 * Registers stateful handlers for the merge page. `GET features` returns the merge result once a merge
 * has taken place and the original blocks otherwise, so the map redraws whenever the page bumps its
 * refresh counter. `canUndo`/`canRedo` track the same state so the undo/redo buttons enable correctly.
 * Returns a getter for the number of `GET features` requests, used to wait for the map to reload.
 */
function installMergePageHandlers({ hasMerged = false, hasUndone = false } = {}): { featuresRequests: () => number } {
  let merged = hasMerged;
  let undone = hasUndone;
  let featuresRequests = 0;

  worker.use(
    http.get("/api/features/1", () => {
      featuresRequests++;
      return HttpResponse.json(merged ? mergeResultEd50 : splitResultEd50);
    }),
    http.get("/api/outline-nodes/1", () => HttpResponse.json({ featureOutlineNodes: [] })),
    http.get("/api/textual-description/1", () => HttpResponse.text("")),
    http.get("/api/history/1", () => HttpResponse.json({ canUndo: merged, canRedo: undone })),
    http.post("/api/merge", () => {
      merged = true;
      undone = false;
      return HttpResponse.json({ outputFeatureIds: ["feature-4"] });
    }),
    http.post("/api/undo/1", () => {
      merged = false;
      undone = true;
      return HttpResponse.json({ outputFeatureIds: ["feature-2", "feature-3"] });
    }),
    http.post("/api/redo/1", () => {
      merged = true;
      undone = false;
      return HttpResponse.json({ outputFeatureIds: ["feature-4"] });
    }),
  );

  return { featuresRequests: () => featuresRequests };
}

function renderPage() {
  return render(MergePage, {
    props: {
      commandJourneyId: "1",
      srsWkid: SupportedWkid.ED50_WKID,
      featuresBaseUrl: "/api/features",
      outlineNodesBaseUrl: "/api/outline-nodes",
      mergeUrl: "/api/merge",
      baseUrl: "/api",
      textualDescriptionUrl: "/api/textual-description",
      csrfHeaderName: "X-CSRF-TOKEN",
      csrfToken: "csrf-token",
      includeNstaQuadrants: false,
      includeNstaBlocks: false,
    },
    global: { plugins: [OpenLayersMap] },
  });
}

/** Waits for the feature layer to refetch after an action, then lets OpenLayers settle for the screenshot. */
async function waitForMapReload(before: number, featuresRequests: () => number): Promise<void> {
  await expect.poll(() => featuresRequests(), { timeout: 15000, interval: 100 }).toBeGreaterThan(before);
  await settleForScreenshot();
}

describe("merge lifecycle", () => {
  it("redraws the map with the merged feature after the merge button is clicked", async () => {
    const { featuresRequests } = installMergePageHandlers();

    const screen = renderPage();
    await waitForMapFullyLoaded();

    await page.getByLabelText("21/1a-1").click();
    await page.getByLabelText("21/1a-2").click();

    const before = featuresRequests();
    await page.getByRole("button", { name: "Merge", exact: true }).click();
    await waitForMapReload(before, featuresRequests);

    await expect(screen.locator).toMatchScreenshot("merge-result-drawn");
  });

  it("restores the separate blocks on the map after undo", async () => {
    const { featuresRequests } = installMergePageHandlers({ hasMerged: true });

    const screen = renderPage();
    await waitForMapFullyLoaded();
    await expect.element(page.getByRole("button", { name: "Undo merge" })).toBeEnabled();

    const before = featuresRequests();
    await page.getByRole("button", { name: "Undo merge" }).click();
    await waitForMapReload(before, featuresRequests);

    await expect(screen.locator).toMatchScreenshot("merge-undone");
  });

  it("redraws the merged feature on the map after redo", async () => {
    const { featuresRequests } = installMergePageHandlers({ hasMerged: false, hasUndone: true });

    const screen = renderPage();
    await waitForMapFullyLoaded();
    await expect.element(page.getByRole("button", { name: "Redo merge" })).toBeEnabled();

    const before = featuresRequests();
    await page.getByRole("button", { name: "Redo merge" }).click();
    await waitForMapReload(before, featuresRequests);

    await expect(screen.locator).toMatchScreenshot("merge-redone");
  });
});
