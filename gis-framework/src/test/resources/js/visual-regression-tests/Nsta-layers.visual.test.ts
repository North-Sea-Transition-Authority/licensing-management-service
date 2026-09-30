import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import { parseFeatures, pressKeyOnMap, waitForMapFullyLoaded } from "./visual-test-util";

const ED50_WKID = 4230;

describe("nsta layers", () => {
  it("renders quadrants", async () => {
    const screen = render(BaseMap, {
      props: {
        features: parseFeatures(singleBlockEd50),
        outlineNodes: [],
        srsWkid: ED50_WKID,
        includeSnapPoints: false,
        includeNstaQuadrants: true,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("-");
    await pressKeyOnMap("-");
    await expect(screen.locator).toMatchScreenshot("nsta-layers-quadrants");
  });

  it("renders blocks", async () => {
    const screen = render(BaseMap, {
      props: {
        features: parseFeatures(singleBlockEd50),
        outlineNodes: [],
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
