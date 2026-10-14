import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import { SupportedWkid } from "@/coordinate-system-utils";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import {
  clickAt,
  getViewport,
  hoverOverNextSnapPointBelow,
  hoverOverSnapPoint,
  parseFeatures,
  pressKeyOnMap,
  waitForMapFullyLoaded,
} from "./visual-test-util";

describe("draw line layer", () => {
  it("shows last-marker after selecting first snap point", async () => {
    const screen = render(BaseMap, {
      props: {
        features: parseFeatures(singleBlockEd50),
        outlineNodes: [],
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
        includeDrawLine: true,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");

    const viewport = getViewport();
    const rect = viewport.getBoundingClientRect();
    const [cx, cy] = await hoverOverSnapPoint(viewport, rect.left + rect.width / 2, rect.top + rect.height / 2);
    await clickAt(viewport, cx, cy);

    await expect(screen.locator).toMatchScreenshot("draw-line-first-point-selected");
  });

  it("shows dashed preview segment when hovering over a valid second point", async () => {
    const screen = render(BaseMap, {
      props: {
        features: parseFeatures(singleBlockEd50),
        outlineNodes: [],
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
        includeDrawLine: true,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");

    const viewport = getViewport();
    const rect = viewport.getBoundingClientRect();
    const [cx, cy] = await hoverOverSnapPoint(viewport, rect.left + rect.width / 2, rect.top + rect.height / 2);
    await clickAt(viewport, cx, cy);
    await hoverOverNextSnapPointBelow(viewport, cx, cy);

    await expect(screen.locator).toMatchScreenshot("draw-line-preview-segment");
  });

  it("commits a solid line segment after clicking a second aligned snap point", async () => {
    const screen = render(BaseMap, {
      props: {
        features: parseFeatures(singleBlockEd50),
        outlineNodes: [],
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
        includeDrawLine: true,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");

    const viewport = getViewport();
    const rect = viewport.getBoundingClientRect();
    const [cx, cy] = await hoverOverSnapPoint(viewport, rect.left + rect.width / 2, rect.top + rect.height / 2);
    await clickAt(viewport, cx, cy);
    const [cx2, cy2] = await hoverOverNextSnapPointBelow(viewport, cx, cy);
    await clickAt(viewport, cx2, cy2);

    await expect(screen.locator).toMatchScreenshot("draw-line-committed-segment");
  });

  it("committed line remains visible after zooming out below min snap zoom", async () => {
    const screen = render(BaseMap, {
      props: {
        features: parseFeatures(singleBlockEd50),
        outlineNodes: [],
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
        includeDrawLine: true,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");

    const viewport = getViewport();
    const rect = viewport.getBoundingClientRect();
    const [cx, cy] = await hoverOverSnapPoint(viewport, rect.left + rect.width / 2, rect.top + rect.height / 2);
    await clickAt(viewport, cx, cy);
    const [cx2, cy2] = await hoverOverNextSnapPointBelow(viewport, cx, cy);
    await clickAt(viewport, cx2, cy2);

    await pressKeyOnMap("-");
    await pressKeyOnMap("-");
    await pressKeyOnMap("-");
    // OL's zoom animation plus canvas repaint can take longer after multiple rapid presses
    await new Promise(resolve => setTimeout(resolve, 300));
    await new Promise(resolve => requestAnimationFrame(resolve));
    await new Promise(resolve => requestAnimationFrame(resolve));

    await expect(screen.locator).toMatchScreenshot("draw-line-persists-below-min-zoom");
  });
});
