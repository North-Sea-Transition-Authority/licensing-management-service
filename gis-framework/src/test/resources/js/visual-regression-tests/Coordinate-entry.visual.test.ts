import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import { page } from "vitest/browser";
import OpenLayersMap from "vue3-openlayers";
import { SupportedWkid } from "@/coordinate-system-utils";
import SplitByCoordinateEntryPage from "@/pages/SplitByCoordinateEntryPage.vue";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
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

describe("coordinate entry page", () => {
  it("draws a line from coordinates typed into the offshore (DMS) form", async () => {
    worker.use(
      http.get("/api/features/1", () => HttpResponse.json(singleBlockEd50)),
      http.get("/api/outline-nodes/1", () => HttpResponse.json({ featureOutlineNodes: [] })),
      http.get("/api/split-history/1", () => HttpResponse.json({ canUndo: false, canRedo: false })),
      http.get("/api/textual-description/1", () => HttpResponse.text("")),
    );

    const screen = render(SplitByCoordinateEntryPage, {
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

    await waitForMapFullyLoaded();

    // First point, then add a second card and enter its coordinates — a two-segment-free single line.
    await enterDmsPoint(0, [53, 45, 0], [3, 32, 0]);
    clickAddAfter(0);
    await settle();
    await enterDmsPoint(1, [53, 48, 0], [3, 26, 0]);
    await settleForScreenshot();

    await expect(screen.locator).toMatchScreenshot("coordinate-entry-line-drawn");
  });
});
