import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import { SupportedWkid } from "@/coordinate-system-utils";
import singleBlockBng from "../fixtures/singleBlockBng.esriJson.json";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import { worker } from "./setup";
import { pressKeyOnMap, waitForMapFullyLoaded } from "./visual-test-util";

/**
 * Scans pixels outward from the map centre, ring by ring, until a snap-point
 * tooltip appears, then leaves the pointer there for the screenshot. Throws if
 * none is found anywhere within the viewport.
 */
async function hoverOverNearestSnapPoint(): Promise<void> {
  const viewport = document.querySelector<HTMLElement>(".ol-viewport")!;
  const rect = viewport.getBoundingClientRect();
  const cx = rect.left + rect.width / 2;
  const cy = rect.top + rect.height / 2;
  const step = 2;
  const maxRadius = Math.ceil(Math.min(rect.width, rect.height) / 2);

  const isOverSnapPoint = async (dx: number, dy: number): Promise<boolean> => {
    viewport.dispatchEvent(new PointerEvent("pointermove", {
      bubbles: true,
      cancelable: true,
      clientX: cx + dx,
      clientY: cy + dy,
    }));
    await new Promise(resolve => requestAnimationFrame(resolve));
    return document.querySelector(".snap-tooltip") !== null;
  };

  if (await isOverSnapPoint(0, 0)) {
    return;
  }

  for (let radius = step; radius <= maxRadius; radius += step) {
    for (let dx = -radius; dx <= radius; dx += step) {
      if (await isOverSnapPoint(dx, -radius) || await isOverSnapPoint(dx, radius)) {
        return;
      }
    }
    for (let dy = -radius + step; dy <= radius - step; dy += step) {
      if (await isOverSnapPoint(-radius, dy) || await isOverSnapPoint(radius, dy)) {
        return;
      }
    }
  }
  throw new Error("No snap point found within the map viewport — tooltip never appeared");
}

/**
 * Simulates the pointer leaving the viewport, which triggers clearHovered in
 * SnapPointsLayer and hides the tooltip regardless of snap-point layout.
 */
async function movePointerOffSnapPoint(viewport: HTMLElement): Promise<void> {
  viewport.dispatchEvent(new MouseEvent("mouseleave", { bubbles: false, cancelable: true }));
  await expect.poll(
    () => document.querySelector(".snap-tooltip"),
    { timeout: 2000, interval: 50 },
  ).toBeNull();
}

describe("snap point layer", () => {
  it("renders snap points for ED50 at more zoom", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");
    await pressKeyOnMap("+");

    await expect(screen.locator).toMatchScreenshot("snap-points-ed50-more-zoom");
  });

  it("renders snap points for ED50 at less zoom", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
        srsWkid: SupportedWkid.ED50_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");

    await expect(screen.locator).toMatchScreenshot("snap-points-ed50-less-zoom");
  });

  it("no snap points rendered below min zoom level for ED50", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockEd50)),
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
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
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
        srsWkid: SupportedWkid.BNG_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");
    await pressKeyOnMap("+");
    await expect(screen.locator).toMatchScreenshot("snap-points-bng-more-zoom");
  });

  it("renders snap points for BNG at less zoom", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockBng)),
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
        srsWkid: SupportedWkid.BNG_WKID,
        includeSnapPoints: true,
        includeNstaQuadrants: false,
        includeNstaBlocks: false,
      },
      global: { plugins: [OpenLayersMap] },
    });

    await waitForMapFullyLoaded();
    await pressKeyOnMap("+");
    await expect(screen.locator).toMatchScreenshot("snap-points-bng-less-zoom");
  });

  it("no snap points rendered below min zoom level for BNG", async () => {
    worker.use(
      http.get("/api/features", () => HttpResponse.json(singleBlockBng)),
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
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
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
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
      http.get("/api/outline-nodes?featureId=feature-id", () => HttpResponse.json({ featureOutlineNodes: [] })),
    );

    const screen = render(BaseMap, {
      props: {
        featuresUrl: "/api/features",
        outlineNodesUrl: "/api/outline-nodes?featureId=1",
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
