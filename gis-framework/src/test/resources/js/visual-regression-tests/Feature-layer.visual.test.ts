import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import { SupportedWkid } from "@/coordinate-system-utils";
import singleBlockBng from "../fixtures/singleBlockBng.esriJson.json";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import { parseFeatures, waitForMapFullyLoaded } from "./visual-test-util";

describe("feature layer", () => {
  it("renders the singleBlock ED50 feature", async () => {
    const screen = render(BaseMap, {
      props: {
        features: parseFeatures(singleBlockEd50),
        outlineNodes: [],
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: false,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();

    await expect(screen.locator).toMatchScreenshot("base-map-single-ed50-block");
  });

  it("renders the singleBlock BNG feature", async () => {
    const screen = render(BaseMap, {
      props: {
        features: parseFeatures(singleBlockBng),
        outlineNodes: [],
        srsWkid: SupportedWkid.BNG_WKID,
        includeSnapPoints: false,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();

    await expect(screen.locator).toMatchScreenshot("base-map-single-bng-block");
  });
});
