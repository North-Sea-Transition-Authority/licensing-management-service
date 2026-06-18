import { cleanup } from "@testing-library/vue";
import { http, HttpResponse } from "msw";
import { setupWorker } from "msw/browser";
import { afterAll, afterEach, beforeAll, beforeEach } from "vitest";
import { page } from "vitest/browser";

/**
 * Mocks the map OSM tile layer.
 */
function createMockTilePng(): ArrayBuffer {
  const canvas = document.createElement("canvas");
  canvas.width = 256;
  canvas.height = 256;
  const ctx = canvas.getContext("2d")!;
  ctx.fillStyle = "#e8e8e8";
  ctx.fillRect(0, 0, 256, 256);
  const base64 = canvas.toDataURL("image/png").split(",")[1];
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

const mockTilePng = createMockTilePng();

// Passed as an initial handler so it survives worker.resetHandlers() between tests.
// All visual tests that render the map get OSM tiles mocked automatically.
export const worker = setupWorker(
  http.get("https://tile.openstreetmap.org/:z/:x/:y.png", () =>
    HttpResponse.arrayBuffer(mockTilePng, { headers: { "Content-Type": "image/png" } })),
);

beforeAll(async () => {
  await worker.start({ onUnhandledRequest: "bypass", quiet: true });
});

beforeEach(async () => {
  await page.viewport(1280, 800);

  // OL needs explicit pixel height — height:100% from mapStyle resolves to 0
  // inside the Vitest iframe without a defined parent height.
  const style = document.createElement("style");
  style.id = "vitest-map-height-override";
  style.textContent = ".gis-framework-map { height: 600px !important; }";
  document.head.appendChild(style);
});

afterEach(async () => {
  // Explicitly unmount before resetting handlers so OL's rAF completes
  // while MSW is still intercepting tile requests.
  cleanup();
  await new Promise(resolve => requestAnimationFrame(resolve));

  document.getElementById("vitest-map-height-override")?.remove();
  worker.resetHandlers();
});

afterAll(() => {
  worker.stop();
});
