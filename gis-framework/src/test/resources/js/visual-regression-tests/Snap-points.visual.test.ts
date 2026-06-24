import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "../../../../main/resources/js/components/baseMap/BaseMap.vue";
import { SupportedWkid } from "../../../../main/resources/js/coordinate-system-utils";
import singleBlockBng from "../fixtures/singleBlockBng.esriJson.json";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import { worker } from "./setup";
import { pressKeyOnMap, waitForMapFullyLoaded } from "./visual-test-util";

/**
 * Scans pixels near the map centre until a snap-point tooltip appears, then
 * leaves the pointer there for the screenshot. Throws if no snap point is
 * found within the search area.
 */
async function hoverOverNearestSnapPoint(): Promise<void> {
  const viewport = document.querySelector<HTMLElement>(".ol-viewport")!;
  const rect = viewport.getBoundingClientRect();
  const cx = rect.left + rect.width / 2;
  const cy = rect.top + rect.height / 2;

  // Snap points are spaced ~30–40 px. This will ensure we hit one point in the small search window.
  // Found it had better consistency than hardcoding the coordinates.
  for (let dx = -60; dx <= 60; dx += 2) {
    for (let dy = -60; dy <= 60; dy += 2) {
      viewport.dispatchEvent(new PointerEvent("pointermove", {
        bubbles: true,
        cancelable: true,
        clientX: cx + dx,
        clientY: cy + dy,
      }));
      await new Promise(resolve => requestAnimationFrame(resolve));
      if (document.querySelector(".snap-tooltip")) {
        return;
      }
    }
  }
  throw new Error("No snap point found near map centre — tooltip never appeared");
}

/**
 * Moves the pointer to the top-left corner of the viewport away from any
 * snap point and waits for the tooltip to disappear.
 */
async function movePointerOffSnapPoint(viewport: HTMLElement): Promise<void> {
  const rect = viewport.getBoundingClientRect();
  viewport.dispatchEvent(new PointerEvent("pointermove", {
    bubbles: true,
    cancelable: true,
    clientX: rect.left + 10,
    clientY: rect.top + 10,
  }));
  await expect.poll(
    () => document.querySelector(".snap-tooltip"),
    { timeout: 2000, interval: 50 },
  ).toBeNull();
}

describe("snap point layer", () => {
  it("renders snap points for ED50 at more zoom", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");

    await expect(screen.locator).toMatchScreenshot("snap-points-ed50-more-zoom");
  });

  it("renders snap points for ED50 at less zoom", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();

    await expect(screen.locator).toMatchScreenshot("snap-points-ed50-less-zoom");
  });

  it("no snap points rendered below min zoom level for ED50", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("-");
    await expect(screen.locator).toMatchScreenshot("snap-points-ed50-below-min-zoom");
  });

  it("renders snap points for BNG at more zoom", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockBng)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: SupportedWkid.BNG_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");
    await expect(screen.locator).toMatchScreenshot("snap-points-bng-more-zoom");
  });

  it("renders snap points for BNG at less zoom", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockBng)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: SupportedWkid.BNG_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();

    await expect(screen.locator).toMatchScreenshot("snap-points-bng-less-zoom");
  });

  it("no snap points rendered below min zoom level for BNG", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockBng)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: SupportedWkid.BNG_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("-");
    await expect(screen.locator).toMatchScreenshot("snap-points-bng-below-min-zoom");
  });

  it("shows tooltip when hovering over an ED50 snap point", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();

    await pressKeyOnMap("+");
    await hoverOverNearestSnapPoint();
    await expect(screen.locator).toMatchScreenshot("snap-tooltip-ed50-hover");
    const viewport = document.querySelector<HTMLElement>(".ol-viewport")!;
    await movePointerOffSnapPoint(viewport);
    await expect(screen.locator).toMatchScreenshot("snap-tooltip-ed50-no-hover");
  });

  it("shows tooltip when hovering over a BNG snap point", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockBng)),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        srsWkid: SupportedWkid.BNG_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();

    await pressKeyOnMap("+");
    await hoverOverNearestSnapPoint();
    await expect(screen.locator).toMatchScreenshot("snap-tooltip-bng-hover");
    const viewport = document.querySelector<HTMLElement>(".ol-viewport")!;
    await movePointerOffSnapPoint(viewport);
    await expect(screen.locator).toMatchScreenshot("snap-tooltip-bng-no-hover");
  });
});
