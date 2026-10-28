import type { JsonBodyType } from "msw";
import type { JsonHistoryAction, JsonHistoryStatus } from "@/api/history.api";
import type { TransformationType } from "@/transformation-type";
import { page } from "@vitest/browser/context";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import { SupportedWkid } from "@/coordinate-system-utils";
import MapActionsPage from "@/pages/MapActionsPage.vue";
import depthMapEd50 from "../fixtures/depthMapEd50.esriJson.json";
import depthMapMergeEd50 from "../fixtures/depthMapEd50.merge.esriJson.json";
import depthMapOutlineNodes from "../fixtures/depthMapEd50.outlineNodes.json";
import depthMapSplitEd50 from "../fixtures/depthMapEd50.split.esriJson.json";
import depthMapStrataSplitEd50 from "../fixtures/depthMapEd50.strataSplit.esriJson.json";
import { worker } from "./setup";
import {
  clickSnapPointBelow,
  clickSnapPointNearCentre,
  settleForScreenshot,
  waitForMapFullyLoaded,
} from "./visual-test-util";

const textualDescriptionHtml = `<div class="gis-textual-description">
 <p class="govuk-body">The area is bounded by the following coordinates:</p>
 <ol class="govuk-list govuk-list--number">
 <li class="govuk-!-font-tabular-numbers">1E 1N</li>
 <li class="govuk-!-font-tabular-numbers">2E 2N</li>
 </ol>
</div>`;

const previousActions: JsonHistoryAction[] = [
  { number: 3, transformationType: "MERGE", inputFeatureNames: ["Block A_1", "Block B"], outputFeatureNames: ["mergeResult_4E03"] },
  { number: 2, transformationType: "STRATA_SPLIT", inputFeatureNames: ["Block A_1"], outputFeatureNames: ["Block A_1_above", "Block A_1_below"] },
  { number: 1, transformationType: "SPLIT", inputFeatureNames: ["Block A"], outputFeatureNames: ["Block A_1", "Block A_2"] },
];

/**
 * Registers stateful handlers: `GET features` returns the split, merge or strata split fixture once `POST split`,
 * `POST merge` or `POST strata-split` has been called, the original fixture again after `POST undo`, and the result
 * again after `POST redo`. History lists the actions taken, newest first, and reports the latest as undoable, or the
 * last undone as redoable. Returns a getter for the number of `GET features` requests, which is used to wait for the map to
 * reload.
 */
function mockEndpoints(initialActions: JsonHistoryAction[] = []): { featuresRequests: () => number } {
  let features: JsonBodyType = depthMapEd50;
  let featuresRequests = 0;
  let actions = [...initialActions];
  let undone: { action: JsonHistoryAction, result: JsonBodyType } | null = null;

  function perform(transformationType: TransformationType, result: JsonBodyType, inputFeatureNames: string[], outputFeatureNames: string[]) {
    features = result;
    actions = [{ number: actions.length + 1, transformationType, inputFeatureNames, outputFeatureNames }, ...actions];
    undone = null;
  }

  worker.use(
    http.get("/api/features/1", () => {
      featuresRequests++;
      return HttpResponse.json(features);
    }),
    http.get("/api/outline-nodes/1", () => HttpResponse.json({ featureOutlineNodes: depthMapOutlineNodes })),
    http.get("/api/textual-description/1", () => HttpResponse.json({ textualDescription: textualDescriptionHtml })),
    http.get("/api/history/1", () => HttpResponse.json({
      undoTransformationType: actions[0]?.transformationType ?? null,
      redoTransformationType: undone?.action.transformationType ?? null,
      actions,
    } satisfies JsonHistoryStatus)),
    http.post("/api/split", () => {
      perform("SPLIT", depthMapSplitEd50, ["Middle block"], ["Middle block_1", "Middle block_2"]);
      return HttpResponse.json({ outputFeatureIds: ["middle-west", "middle-east"] });
    }),
    http.post("/api/merge", () => {
      perform("MERGE", depthMapMergeEd50, ["Middle block", "Shallow block"], ["Merged block"]);
      return HttpResponse.json({ outputFeatureId: "merged" });
    }),
    http.post("/api/strata-split", () => {
      perform("STRATA_SPLIT", depthMapStrataSplitEd50, ["Middle block"], ["Middle block_above", "Middle block_below"]);
      return HttpResponse.json({ outputFeatureIds: ["middle-above", "middle-below"] });
    }),
    http.post("/api/undo/1", () => {
      const [action, ...remaining] = actions;
      undone = { action, result: features };
      actions = remaining;
      features = depthMapEd50;
      return new HttpResponse(null, { status: 200 });
    }),
    http.post("/api/redo/1", () => {
      if (undone) {
        actions = [undone.action, ...actions];
        features = undone.result;
        undone = null;
      }
      return new HttpResponse(null, { status: 200 });
    }),
  );

  return { featuresRequests: () => featuresRequests };
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
      baseUrl: "/api",
      csrfHeaderName: "X-CSRF-TOKEN",
      csrfToken: "csrf-token",
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

async function chooseSplitAtMiddleBand(screen: ReturnType<typeof renderInFixedWidthContainer>) {
  await screen.getByText("-100m to -200m", { exact: true }).click();
  await screen.getByRole("radio", { name: "Split" }).click();
  await screen.getByRole("button", { name: "Continue" }).click();
  await expect.element(screen.getByRole("heading", { name: "Split" })).toBeVisible();
}

async function chooseMerge(screen: ReturnType<typeof renderInFixedWidthContainer>) {
  await screen.getByRole("radio", { name: "Merge" }).click();
  await screen.getByRole("button", { name: "Continue" }).click();
  await expect.element(screen.getByRole("heading", { name: "Merge" })).toBeVisible();
}

async function chooseAddStrata(screen: ReturnType<typeof renderInFixedWidthContainer>) {
  await screen.getByRole("radio", { name: "Add strata" }).click();
  await screen.getByRole("button", { name: "Continue" }).click();
  await expect.element(screen.getByRole("heading", { name: "Add strata" })).toBeVisible();
}

async function addStrataToMiddleBlockAt(screen: ReturnType<typeof renderInFixedWidthContainer>, depth: string) {
  await screen.getByRole("textbox", { name: "What depth is the new stratigraphic boundary at?" }).fill(depth);
  await screen.getByRole("checkbox", { name: "Middle block" }).click();
  await screen.getByRole("button", { name: "Add strata" }).click();
}

async function tickShallowAndMiddleBlocks(screen: ReturnType<typeof renderInFixedWidthContainer>) {
  await screen.getByRole("checkbox", { name: "Shallow block" }).click();
  await screen.getByRole("checkbox", { name: "Middle block" }).click();
  await expect.element(screen.getByText("The attributes for the merged subarea will need to be defined again."))
    .toBeVisible();
}

async function openActionHistory(screen: ReturnType<typeof renderInFixedWidthContainer>, actionCount: number) {
  await screen.getByText(`Show action history (${actionCount} ${actionCount === 1 ? "action" : "actions"})`).click();
}

describe("map actions page", () => {
  beforeEach(async () => {
    await page.viewport(1280, 1600);
  });

  it("renders the shallowest depth band by default", async () => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();

    await waitForPageLoaded();
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-shallowest-band");
  });

  it.each([
    { band: "-100m to -200m", screenshot: "map-actions-middle-band" },
    { band: "-200m to -∞m", screenshot: "map-actions-deepest-band" },
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

    await expect(screen.locator).toMatchScreenshot("map-actions-no-action-selected");
  });

  it("shows the split panel and snap points when split is chosen", async () => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();

    await chooseSplitAtMiddleBand(screen);
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-split-mode");
  });

  it("locks the depth slider and lists the point while a line is being drawn", async () => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();
    await chooseSplitAtMiddleBand(screen);

    await clickSnapPointNearCentre();
    await expect.element(screen.getByRole("slider", { name: "Select depth" })).toBeDisabled();
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-split-line-in-progress");
  });

  it("redraws the split band and returns to the action select once cut", async () => {
    const { featuresRequests } = mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();
    await chooseSplitAtMiddleBand(screen);
    const before = featuresRequests();

    await clickSnapPointBelow(await clickSnapPointNearCentre());

    await expect.poll(() => featuresRequests(), { timeout: 15000, interval: 100 }).toBeGreaterThan(before);
    await expect.element(screen.getByRole("group", { name: "What action would you like to perform next?" }))
      .toBeVisible();
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-split-result");
  });

  it("shows the merge panel listing every feature when merge is chosen", async () => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();

    await chooseMerge(screen);
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-merge-mode");
  });

  it("highlights the ticked features and shows the warning when two features are ticked", async () => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();
    await chooseMerge(screen);

    await tickShallowAndMiddleBlocks(screen);
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-merge-selected");
  });

  it("redraws the merged feature and returns to the action select once merged", async () => {
    const { featuresRequests } = mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();
    await chooseMerge(screen);
    await tickShallowAndMiddleBlocks(screen);
    const before = featuresRequests();

    await screen.getByRole("button", { name: "Merge" }).click();

    await expect.poll(() => featuresRequests(), { timeout: 15000, interval: 100 }).toBeGreaterThan(before);
    await expect.element(screen.getByRole("group", { name: "What action would you like to perform next?" }))
      .toBeVisible();
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-merge-result");
  });

  it("shows the add strata panel listing every feature with its depth ranges when add strata is chosen", async () => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();

    await chooseAddStrata(screen);
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-add-strata-mode");
  });

  it("shows the error and highlights the ticked feature when the depth is already a boundary", async () => {
    mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();
    await chooseAddStrata(screen);

    await addStrataToMiddleBlockAt(screen, "-100");
    await expect.element(screen.getByRole("link", {
      name: "Middle block cannot be split at -100 as it is outside the subarea or is already a boundary",
    })).toBeVisible();
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-add-strata-validation");
  });

  it("adds the new depth band and returns to the action select once strata is added", async () => {
    const { featuresRequests } = mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();
    await chooseAddStrata(screen);
    const before = featuresRequests();

    await addStrataToMiddleBlockAt(screen, "-150");

    await expect.poll(() => featuresRequests(), { timeout: 15000, interval: 100 }).toBeGreaterThan(before);
    await expect.element(screen.getByRole("group", { name: "What action would you like to perform next?" }))
      .toBeVisible();
    await expect.poll(() => document.querySelectorAll(".depth-marker").length).toBe(4);
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-add-strata-result");
  });

  it("removes the added depth band and asks for the first action again once add strata is undone", async () => {
    const { featuresRequests } = mockEndpoints();
    const screen = renderInFixedWidthContainer();
    await waitForPageLoaded();
    await chooseAddStrata(screen);
    await addStrataToMiddleBlockAt(screen, "-150");
    await expect.poll(() => document.querySelectorAll(".depth-marker").length).toBe(4);
    await screen.getByText("-150m to -200m", { exact: true }).click();
    const before = featuresRequests();

    await screen.getByRole("button", { name: "Undo add strata" }).click();

    await expect.poll(() => featuresRequests(), { timeout: 15000, interval: 100 }).toBeGreaterThan(before);
    await expect.element(screen.getByRole("group", { name: "What action would you like to perform first?" }))
      .toBeVisible();
    await expect.poll(() => document.querySelectorAll(".depth-marker").length).toBe(3);
    await expect.element(screen.getByRole("button", { name: "Redo add strata" })).toBeEnabled();
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("map-actions-add-strata-undone");
  });

  describe("action history", () => {
    it("shows that no actions have been taken", async () => {
      mockEndpoints();
      const screen = renderInFixedWidthContainer();
      await waitForPageLoaded();

      await openActionHistory(screen, 0);
      await expect.element(screen.getByText("No actions taken yet.")).toBeVisible();
      await settleForScreenshot();

      await expect(screen.locator).toMatchScreenshot("map-actions-history-empty");
    });

    it("lists the actions already taken, newest first", async () => {
      mockEndpoints(previousActions);
      const screen = renderInFixedWidthContainer();
      await waitForPageLoaded();

      await openActionHistory(screen, 3);
      await expect.element(screen.getByText("mergeResult_4E03")).toBeVisible();
      await settleForScreenshot();

      await expect(screen.locator).toMatchScreenshot("map-actions-history-populated");
    });

    it("adds the action once strata is added", async () => {
      const { featuresRequests } = mockEndpoints(previousActions);
      const screen = renderInFixedWidthContainer();
      await waitForPageLoaded();
      await chooseAddStrata(screen);
      const before = featuresRequests();

      await addStrataToMiddleBlockAt(screen, "-150");
      await expect.poll(() => featuresRequests(), { timeout: 15000, interval: 100 }).toBeGreaterThan(before);
      await openActionHistory(screen, 4);
      await expect.element(screen.getByText("Middle block_above, Middle block_below")).toBeVisible();
      await settleForScreenshot();

      await expect(screen.locator).toMatchScreenshot("map-actions-history-after-add-strata");
    });

    it("removes the action once undone and restores it once redone", async () => {
      const { featuresRequests } = mockEndpoints(previousActions);
      const screen = renderInFixedWidthContainer();
      await waitForPageLoaded();
      await openActionHistory(screen, 3);

      await screen.getByRole("button", { name: "Undo merge" }).click();
      await expect.element(screen.getByText("Show action history (2 actions)")).toBeVisible();
      await expect.element(screen.getByText("mergeResult_4E03")).not.toBeInTheDocument();
      await settleForScreenshot();
      await expect(screen.locator).toMatchScreenshot("map-actions-history-undone");

      const before = featuresRequests();
      await screen.getByRole("button", { name: "Redo merge" }).click();
      await expect.poll(() => featuresRequests(), { timeout: 15000, interval: 100 }).toBeGreaterThan(before);
      await expect.element(screen.getByText("mergeResult_4E03")).toBeVisible();
      await settleForScreenshot();

      await expect(screen.locator).toMatchScreenshot("map-actions-history-redone");
    });
  });
});
