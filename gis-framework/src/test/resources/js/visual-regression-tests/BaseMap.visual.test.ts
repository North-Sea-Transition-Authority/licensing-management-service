import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "../../../../main/resources/js/components/baseMap/BaseMap.vue";
import singleBlock from "../fixtures/singleBlock.esriJson.json";
import { worker } from "./setup";

const ED50_WKID = 4230;

async function waitForMapFullyLoaded() {
  await expect.poll(
    () => document.querySelectorAll(".ol-viewport canvas").length,
    { timeout: 15000, interval: 100 },
  ).toBeGreaterThan(0);

  // Give OpenLayers a couple of frames to paint tiles/vector layers.
  await new Promise(resolve => requestAnimationFrame(resolve));
  await new Promise(resolve => requestAnimationFrame(resolve));
}

describe("baseMap visual", () => {
  it("renders the singleBlock feature", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlock)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: ED50_WKID,
        includeSnapPoints: false,
        includeNstaQuadrants: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();

    await expect(screen.locator).toMatchScreenshot("base-map-single-block");
  });
});
