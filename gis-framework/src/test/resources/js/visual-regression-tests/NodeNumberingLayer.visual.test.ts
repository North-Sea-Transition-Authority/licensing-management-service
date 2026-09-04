import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import { SupportedWkid } from "@/coordinate-system-utils";
import singleBlockBng from "../fixtures/singleBlockBng.esriJson.json";
import bngOutlineNodes from "../fixtures/singleBlockBng.outlineNodes.json";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import ed50OutlineNodes from "../fixtures/singleBlockEd50.outlineNodes.json";
import { worker } from "./setup";

async function waitForMapFullyLoaded() {
  await expect.poll(
    () => document.querySelectorAll(".ol-viewport canvas").length,
    { timeout: 15000, interval: 100 },
  ).toBeGreaterThan(0);

  // Give OpenLayers a couple of frames to paint tiles/vector layers.
  await new Promise(resolve => requestAnimationFrame(resolve));
  await new Promise(resolve => requestAnimationFrame(resolve));
}

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
      worker.use(
        http.get("/api/features", () => HttpResponse.json(esriJson)),
        http.get("/api/outline-nodes", () => HttpResponse.json({ featureOutlineNodes: outlineNodes })),
      );

      const screen = render(BaseMap, {
        props: {
          featuresUrl: "/api/features",
          outlineNodesUrl: "/api/outline-nodes",
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
