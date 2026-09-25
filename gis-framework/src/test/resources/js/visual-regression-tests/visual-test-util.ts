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
