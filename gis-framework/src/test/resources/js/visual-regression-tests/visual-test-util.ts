import { userEvent } from "@vitest/browser/context";
import { expect } from "vitest";

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

export async function pressKeyOnMap(key: string) {
  const viewport = document.querySelector<HTMLElement>(".ol-viewport")!;
  await userEvent.click(viewport);
  viewport.dispatchEvent(new MouseEvent("mouseleave", { bubbles: false, cancelable: true }));
  await userEvent.keyboard(key);
  await waitForZoomToSettle();
}
