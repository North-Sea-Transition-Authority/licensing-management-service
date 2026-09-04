import type { LinePoint } from "@/grid-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { redoSplit, splitFeature, undoSplit } from "@/api/split.api";

function point(originalSrsCoordinates: [number, number]): LinePoint {
  return { coordinates: [0, 0], originalSrsCoordinates };
}

describe("splitApi", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe("splitFeature", () => {
    it("splitFeature_whenResponseOk_buildsConsecutiveSegmentsAndReturnsResponse", async () => {
      const expected = { outputFeatureIds: ["feature-1", "feature-2"] };
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(expected),
      });
      vi.stubGlobal("fetch", fetchMock);

      const points = [point([1, 2]), point([3, 4]), point([5, 6])];
      const result = await splitFeature("/api/gis-framework/split", points, "journey-1", "X-CSRF-TOKEN", "csrf-token-1");

      expect(result).toEqual(expected);
      expect(fetchMock).toHaveBeenCalledWith("/api/gis-framework/split", {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": "csrf-token-1" },
        body: JSON.stringify({
          cutterLineOriginalSrsCoordinates: [
            [[1, 2], [3, 4]],
            [[3, 4], [5, 6]],
          ],
          commandJourneyId: "journey-1",
        }),
      });
    });

    it("splitFeature_whenResponseNotOk_rejects", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(splitFeature(
        "/api/gis-framework/split",
        [point([1, 2]), point([3, 4])],
        "journey-1",
        "X-CSRF-TOKEN",
        "csrf-token-1",
      )).rejects.toBe("Response status: Internal Server Error");
    });
  });

  describe("undoSplit", () => {
    it("undoSplit_whenResponseOk_returnsResponse", async () => {
      const expected = { outputFeatureIds: ["feature-1"] };
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(expected),
      });
      vi.stubGlobal("fetch", fetchMock);

      const result = await undoSplit("/api/gis-framework/undo/journey-1", "X-CSRF-TOKEN", "csrf-token-1");

      expect(result).toEqual(expected);
      expect(fetchMock).toHaveBeenCalledWith("/api/gis-framework/undo/journey-1", {
        method: "POST",
        headers: { "X-CSRF-TOKEN": "csrf-token-1" },
      });
    });

    it("undoSplit_whenResponseNotOk_rejects", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(undoSplit("/api/gis-framework/undo/journey-1", "X-CSRF-TOKEN", "csrf-token-1"))
        .rejects
        .toBe("Response status: Internal Server Error");
    });
  });

  describe("redoSplit", () => {
    it("redoSplit_whenResponseOk_returnsResponse", async () => {
      const expected = { outputFeatureIds: ["feature-1"] };
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(expected),
      });
      vi.stubGlobal("fetch", fetchMock);

      const result = await redoSplit("/api/gis-framework/redo/journey-1", "X-CSRF-TOKEN", "csrf-token-1");

      expect(result).toEqual(expected);
      expect(fetchMock).toHaveBeenCalledWith("/api/gis-framework/redo/journey-1", {
        method: "POST",
        headers: { "X-CSRF-TOKEN": "csrf-token-1" },
      });
    });

    it("redoSplit_whenResponseNotOk_rejects", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(redoSplit("/api/gis-framework/redo/journey-1", "X-CSRF-TOKEN", "csrf-token-1"))
        .rejects
        .toBe("Response status: Internal Server Error");
    });
  });
});
