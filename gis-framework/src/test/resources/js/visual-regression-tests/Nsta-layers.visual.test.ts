import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "../../../../main/resources/js/components/baseMap/BaseMap.vue";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import { worker } from "./setup";
import { pressKeyOnMap, waitForMapFullyLoaded } from "./visual-test-util";

const ED50_WKID = 4230;

describe("nsta layers", () => {
  it("renders quadrants", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: ED50_WKID,
        includeSnapPoints: false,
        includeNstaQuadrants: true,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("-");
    await expect(screen.locator).toMatchScreenshot("nsta-layers-quadrants");
  });

  it("renders blocks", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: ED50_WKID,
        includeSnapPoints: false,
        includeNstaQuadrants: false,
        includeNstaBlocks: true,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("-");
    // give time to load blocks as they have more data than quadrants
    await new Promise(resolve => setTimeout(resolve, 1000));
    await expect(screen.locator).toMatchScreenshot("nsta-layers-blocks");
  });
});
