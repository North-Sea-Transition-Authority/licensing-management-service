import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { render } from "vitest-browser-vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "../../../../main/resources/js/components/baseMap/BaseMap.vue";
import { SupportedWkid } from "../../../../main/resources/js/coordinate-system-utils";
import singleBlockEd50 from "../fixtures/singleBlockEd50.esriJson.json";
import { worker } from "./setup";
import { pressKeyOnMap, waitForMapFullyLoaded } from "./visual-test-util";

/**
 * Returns the client coordinates of the currently-shown snap tooltip's snap point
 */
function getSnapPointClientPosition(): [number, number] | undefined {
  const tooltip = document.querySelector<HTMLElement>(".snap-tooltip");
  if (!tooltip)
    return undefined;
  const overlayContainer = tooltip.closest<HTMLElement>(".ol-overlay-container");
  if (!overlayContainer)
    return undefined;
  const rect = overlayContainer.getBoundingClientRect();
  return [rect.left - 8, rect.top - 8];
}

/**
 * Scans around the given center pixel until a snap-point tooltip appears.
 * Leaves the pointer hovering over the found point and returns the snap point's
 * actual client coordinates (derived from the OL overlay position, not the cursor).
 */
async function hoverOverSnapPoint(viewport: HTMLElement, searchCx: number, searchCy: number): Promise<[number, number]> {
  for (let dx = -80; dx <= 80; dx += 2) {
    for (let dy = -80; dy <= 80; dy += 2) {
      const clientX = searchCx + dx;
      const clientY = searchCy + dy;
      viewport.dispatchEvent(new PointerEvent("pointermove", {
        bubbles: true,
        cancelable: true,
        clientX,
        clientY,
      }));
      await new Promise(resolve => requestAnimationFrame(resolve));
      if (document.querySelector(".snap-tooltip")) {
        const actual = getSnapPointClientPosition();
        if (actual) {
          viewport.dispatchEvent(new PointerEvent("pointermove", {
            bubbles: true,
            cancelable: true,
            clientX: actual[0],
            clientY: actual[1],
          }));
          await new Promise(resolve => requestAnimationFrame(resolve));
        }
        return actual ?? [clientX, clientY];
      }
    }
  }
  throw new Error("No snap point found near search area");
}

/**
 * Dispatches a click at the given coordinates and waits for OL's singleclick
 */
async function clickAt(viewport: HTMLElement, clientX: number, clientY: number): Promise<void> {
  viewport.dispatchEvent(new PointerEvent("pointerdown", {
    bubbles: true,
    cancelable: true,
    clientX,
    clientY,
    button: 0,
    buttons: 1,
  }));
  viewport.dispatchEvent(new PointerEvent("pointerup", {
    bubbles: true,
    cancelable: true,
    clientX,
    clientY,
    button: 0,
  }));
  viewport.dispatchEvent(new MouseEvent("click", {
    bubbles: true,
    cancelable: true,
    clientX,
    clientY,
    button: 0,
  }));
  await new Promise(resolve => setTimeout(resolve, 300));
  // Vue watchEffect updates OL geometry
  await new Promise(resolve => requestAnimationFrame(resolve));
  await new Promise(resolve => requestAnimationFrame(resolve));
}

/**
 * Starting from (clientX, fromClientY), scans downward along the same X until a snap
 * tooltip disappears and then reappears indicating a new snap point in the same column.
 * Returns the new snap point's client coordinates from the OL overlay.
 */
async function hoverOverNextSnapPointBelow(viewport: HTMLElement, clientX: number, fromClientY: number): Promise<[number, number]> {
  let tooltipWasGone = false;
  for (let clientY = fromClientY + 4; clientY <= fromClientY + 250; clientY += 4) {
    viewport.dispatchEvent(new PointerEvent("pointermove", {
      bubbles: true,
      cancelable: true,
      clientX,
      clientY,
    }));
    await new Promise(resolve => requestAnimationFrame(resolve));
    const hasTooltip = !!document.querySelector(".snap-tooltip");
    if (!hasTooltip) {
      tooltipWasGone = true;
    } else if (tooltipWasGone) {
      const actual = getSnapPointClientPosition();
      if (actual) {
        viewport.dispatchEvent(new PointerEvent("pointermove", {
          bubbles: true,
          cancelable: true,
          clientX: actual[0],
          clientY: actual[1],
        }));
      }
      // Wait two extra frames: one for OL's repaint to be scheduled and one for it to run.
      await new Promise(resolve => requestAnimationFrame(resolve));
      await new Promise(resolve => requestAnimationFrame(resolve));
      return actual ?? [clientX, clientY];
    }
  }
  throw new Error("No second aligned snap point found below");
}

function getViewport(): HTMLElement {
  const vp = document.querySelector<HTMLElement>(".ol-viewport");
  if (!vp)
    throw new Error("OL viewport not found");
  return vp;
}

describe("draw line layer", () => {
  it("shows last-marker after selecting first snap point", async () => {
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
