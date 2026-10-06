import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import { SupportedWkid } from "@/coordinate-system-utils";
import singleBlockBng from "../fixtures/singleBlockBng.esriJson.json";
import bngOutlineNodes from "../fixtures/singleBlockBng.outlineNodes.json";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import ed50OutlineNodes from "../fixtures/singleBlockEd50.outlineNodes.json";
import { parseFeatures, waitForMapFullyLoaded } from "./visual-test-util";

describe("nodeNumberingLayer visual", () => {
  it.each([
    {
      name: "ED50",
      esriJson: singleBlockEd50,
      outlineNodes: ed50OutlineNodes,
      srsWkid: SupportedWkid.ED50_WKID,
      screenshot: "node-numbering-layer-on-base-map-ed50",
    },
    {
      name: "BNG",
      esriJson: singleBlockBng,
      outlineNodes: bngOutlineNodes,
      srsWkid: SupportedWkid.BNG_WKID,
      screenshot: "node-numbering-layer-on-base-map-bng",
    },
  ])(
    "renders the numbered nodes over the $name feature on the base map",
    async ({ esriJson, outlineNodes, srsWkid, screenshot }) => {
      const screen = render(BaseMap, {
        props: {
          features: parseFeatures(esriJson),
          outlineNodes,
          srsWkid,
          includeSnapPoints: false,
          includeNstaQuadrants: false,
          includeNstaBlocks: false,
        },
        global: { plugins: [OpenLayersMap] },
      });

      await waitForMapFullyLoaded();

      await expect(screen.locator).toMatchScreenshot(screenshot);
    },
  );
});
