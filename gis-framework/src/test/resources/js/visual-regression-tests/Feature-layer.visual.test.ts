import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import { SupportedWkid } from "@/coordinate-system-utils";
import singleBlockBng from "../fixtures/singleBlockBng.esriJson.json";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import { worker } from "./setup";
import { waitForMapFullyLoaded } from "./visual-test-util";

describe("feature layer", () => {
  it("renders the singleBlock ED50 feature", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
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
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockBng)),
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
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
