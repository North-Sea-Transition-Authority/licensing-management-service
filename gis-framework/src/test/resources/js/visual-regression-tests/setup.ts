import { cleanup } from "@testing-library/vue";
import { http, HttpResponse } from "msw";
import { setupWorker } from "msw/browser";
import { afterAll, afterEach, beforeAll } from "vitest";

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

afterEach(async () => {
  // Unmount before resetting handlers so OL's rAF completes while MSW still intercepts tiles.
  cleanup();
  await new Promise(resolve => requestAnimationFrame(resolve));

  worker.resetHandlers();
});

afterAll(() => {
  worker.stop();
});
