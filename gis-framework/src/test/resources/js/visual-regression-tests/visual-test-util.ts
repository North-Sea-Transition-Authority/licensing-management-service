import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import { userEvent } from "@vitest/browser/context";
import { EsriJSON } from "ol/format";
import { expect } from "vitest";

const esriJson = new EsriJSON();

/**
 * Parses an EsriJSON feature set fixture into OpenLayers features, mirroring how the application loads
 * features (WGS84, matching BaseMap's useGeographic()). Use this to feed BaseMap's `features` prop.
 */
export function parseFeatures(featureSet: unknown): Feature<Geometry>[] {
  return esriJson.readFeatures(featureSet, {
    dataProjection: "EPSG:4326",
    featureProjection: "EPSG:4326",
  }) as Feature<Geometry>[];
}

export async function waitForMapFullyLoaded() {
  await expect.poll(
    () => document.querySelectorAll(".ol-viewport canvas").length,
    { timeout: 15000, interval: 100 },
  ).toBeGreaterThan(0);

  // Give OpenLayers a couple of frames to paint tiles/vector layers.
  await new Promise(resolve => requestAnimationFrame(resolve));
  await new Promise(resolve => requestAnimationFrame(resolve));
}

export async function waitForZoomToSettle() {
  // OL keyboard zoom animation duration (100ms) × multiple presses + SnapPointsLayer debounce (100ms) + render buffer.
  await new Promise(resolve => setTimeout(resolve, 600));
  await new Promise(resolve => requestAnimationFrame(resolve));
  await new Promise(resolve => requestAnimationFrame(resolve));
}

/** Blur focus and let Vue/OpenLayers settle so no focus highlight bakes into the screenshot. */
export async function settleForScreenshot(): Promise<void> {
  if (document.activeElement instanceof HTMLElement) {
    document.activeElement.blur();
  }
  await new Promise(resolve => setTimeout(resolve, 100));
  await new Promise(resolve => requestAnimationFrame(resolve));
  await new Promise(resolve => requestAnimationFrame(resolve));
}

export async function pressKeyOnMap(key: string) {
  const viewport = document.querySelector<HTMLElement>(".ol-viewport")!;
  await userEvent.click(viewport);
  viewport.dispatchEvent(new MouseEvent("mouseleave", { bubbles: false, cancelable: true }));
  await userEvent.keyboard(key);
  await waitForZoomToSettle();
}

/** Zooms in to snap range and clicks the snap point nearest the middle of the map. */
export async function clickSnapPointNearCentre(): Promise<[number, number]> {
  await pressKeyOnMap("+");
  const viewport = getViewport();
  const rect = viewport.getBoundingClientRect();
  const [cx, cy] = await hoverOverSnapPoint(viewport, rect.left + rect.width / 2, rect.top + rect.height / 2);
  await clickAt(viewport, cx, cy);
  return [cx, cy];
}

/** Clicks the next snap point directly below the given one, completing a vertical line. */
export async function clickSnapPointBelow([cx, cy]: [number, number]): Promise<void> {
  const viewport = getViewport();
  const [cx2, cy2] = await hoverOverNextSnapPointBelow(viewport, cx, cy);
  await clickAt(viewport, cx2, cy2);
}

/**
 * Returns the client coordinates of the currently-shown snap tooltip's snap point
 */
export function getSnapPointClientPosition(): [number, number] | undefined {
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
export async function hoverOverSnapPoint(viewport: HTMLElement, searchCx: number, searchCy: number): Promise<[number, number]> {
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
export async function clickAt(viewport: HTMLElement, clientX: number, clientY: number): Promise<void> {
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
export async function hoverOverNextSnapPointBelow(viewport: HTMLElement, clientX: number, fromClientY: number): Promise<[number, number]> {
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

export function getViewport(): HTMLElement {
  const vp = document.querySelector<HTMLElement>(".ol-viewport");
  if (!vp)
    throw new Error("OL viewport not found");
  return vp;
}
