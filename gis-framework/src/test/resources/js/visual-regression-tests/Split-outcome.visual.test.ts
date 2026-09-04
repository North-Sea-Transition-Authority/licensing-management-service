import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import { page } from "vitest/browser";
import OpenLayersMap from "vue3-openlayers";
import { SupportedWkid } from "@/coordinate-system-utils";
import SplitByCoordinateEntryPage from "@/pages/SplitByCoordinateEntryPage.vue";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import splitResultEd50 from "../fixtures/splitResultEd50.esriJson.json";
import { worker } from "./setup";
import { settleForScreenshot, waitForMapFullyLoaded } from "./visual-test-util";

/**
 * Enters a full DMS latitude and longitude for the point card at the given index. The Nth
 * Latitude/Longitude fieldset corresponds to the Nth card, so `.nth(index)` scopes the
 * label lookups to the right card.
 */
async function enterDmsPoint(index: number, lat: [number, number, number], lon: [number, number, number]): Promise<void> {
  const latGroup = page.getByRole("group", { name: "Latitude" }).nth(index);
  const lonGroup = page.getByRole("group", { name: "Longitude" }).nth(index);

  await latGroup.getByLabelText("Degrees").fill(String(lat[0]));
  await latGroup.getByLabelText("Minutes").fill(String(lat[1]));
  await latGroup.getByLabelText("Seconds").fill(String(lat[2]));
  await lonGroup.getByLabelText("Degrees").fill(String(lon[0]));
  await lonGroup.getByLabelText("Minutes").fill(String(lon[1]));
  await lonGroup.getByLabelText("Seconds").fill(String(lon[2]));
  await lonGroup.getByLabelText("Hemisphere").selectOptions("W");
}

function clickAddAfter(cardIndex: number): void {
  const cards = document.querySelectorAll(".govuk-summary-card");
  const button = Array.from(cards[cardIndex].querySelectorAll<HTMLButtonElement>("button"))
    .find(b => b.textContent?.trim() === "Add after")!;
  button.click();
}

async function settle(): Promise<void> {
  await new Promise(resolve => setTimeout(resolve, 100));
  await new Promise(resolve => requestAnimationFrame(resolve));
  await new Promise(resolve => requestAnimationFrame(resolve));
}

/**
 * Registers stateful handlers for the split page. `GET features` returns the split result once a split
 * has taken place and the original block otherwise, so the map redraws whenever the page bumps its
 * refresh counter. `canUndo`/`canRedo` track the same state so the undo/redo buttons enable correctly.
 * Returns a getter for the number of `GET features` requests, used to wait for the map to reload.
 */
function installSplitPageHandlers({ hasSplit = false, hasUndone = false } = {}): { featuresRequests: () => number } {
  let split = hasSplit;
  let undone = hasUndone;
  let featuresRequests = 0;

  worker.use(
    http.get("/api/features/1", () => {
      featuresRequests++;
      return HttpResponse.json(split ? splitResultEd50 : singleBlockEd50);
    }),
    http.get("/api/outline-nodes/1", () => HttpResponse.json({ featureOutlineNodes: [] })),
    http.get("/api/textual-description/1", () => HttpResponse.text("")),
    http.get("/api/split-history/1", () => HttpResponse.json({ canUndo: split, canRedo: undone })),
    http.post("/api/split", () => {
      split = true;
      undone = false;
      return HttpResponse.json({ outputFeatureIds: ["feature-2", "feature-3"] });
    }),
    http.post("/api/undo/1", () => {
      split = false;
      undone = true;
      return HttpResponse.json({ outputFeatureIds: ["feature-1"] });
    }),
    http.post("/api/redo/1", () => {
      split = true;
      undone = false;
      return HttpResponse.json({ outputFeatureIds: ["feature-2", "feature-3"] });
    }),
  );

  return { featuresRequests: () => featuresRequests };
}

function renderPage() {
  return render(SplitByCoordinateEntryPage, {
    props: {
      commandJourneyId: "1",
      srsWkid: SupportedWkid.ED50_WKID,
      featuresBaseUrl: "/api/features",
      outlineNodesBaseUrl: "/api/outline-nodes",
      splitUrl: "/api/split",
      historyBaseUrl: "/api/split-history",
      undoBaseUrl: "/api/undo",
      redoBaseUrl: "/api/redo",
      textualDescriptionUrl: "/api/textual-description",
      csrfHeaderName: "X-CSRF-TOKEN",
      csrfToken: "csrf-token",
      coordinatePrecision: 3,
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

describe("split lifecycle", () => {
  it("redraws the map with the split result after the split button is clicked", async () => {
    const { featuresRequests } = installSplitPageHandlers();

    const screen = renderPage();
    await waitForMapFullyLoaded();

    await enterDmsPoint(0, [53, 45, 0], [3, 32, 0]);
    clickAddAfter(0);
    await settle();
    await enterDmsPoint(1, [53, 48, 0], [3, 26, 0]);
    await settle();

    const before = featuresRequests();
    await page.getByRole("button", { name: "Split", exact: true }).click();
    await waitForMapReload(before, featuresRequests);

    await expect(screen.locator).toMatchScreenshot("split-result-drawn");
  });

  it("restores the original block on the map after undo", async () => {
    const { featuresRequests } = installSplitPageHandlers({ hasSplit: true });

    const screen = renderPage();
    await waitForMapFullyLoaded();
    await expect.element(page.getByRole("button", { name: "Undo split" })).toBeEnabled();

    const before = featuresRequests();
    await page.getByRole("button", { name: "Undo split" }).click();
    await waitForMapReload(before, featuresRequests);

    await expect(screen.locator).toMatchScreenshot("split-undone");
  });

  it("redraws the split result on the map after redo", async () => {
    const { featuresRequests } = installSplitPageHandlers({ hasSplit: false, hasUndone: true });

    const screen = renderPage();
    await waitForMapFullyLoaded();
    await expect.element(page.getByRole("button", { name: "Redo split" })).toBeEnabled();

    const before = featuresRequests();
    await page.getByRole("button", { name: "Redo split" }).click();
    await waitForMapReload(before, featuresRequests);

    await expect(screen.locator).toMatchScreenshot("split-redone");
  });
});
