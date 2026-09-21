import type { LinePoint } from "@/grid-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { mergeFeatures, redo, splitFeature, undo } from "@/api/operator.api";

function point(originalSrsCoordinates: [number, number]): LinePoint {
  return { coordinates: [0, 0], originalSrsCoordinates };
}

describe("operatorApi", () => {
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

  describe("mergeFeatures", () => {
    it("mergeFeatures_whenResponseOk_postsFeatureIdsAndReturnsResponse", async () => {
      const expected = { outputFeatureId: "feature-3" };
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(expected),
      });
      vi.stubGlobal("fetch", fetchMock);

      const result = await mergeFeatures(
        "/api/gis-framework/merge",
        ["feature-1", "feature-2"],
        "journey-1",
        "X-CSRF-TOKEN",
        "csrf-token-1",
      );

      expect(result).toEqual(expected);
      expect(fetchMock).toHaveBeenCalledWith("/api/gis-framework/merge", {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": "csrf-token-1" },
        body: JSON.stringify({
          featureIds: ["feature-1", "feature-2"],
          commandJourneyId: "journey-1",
        }),
      });
    });

    it("mergeFeatures_whenResponseNotOk_rejects", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(mergeFeatures(
        "/api/gis-framework/merge",
        ["feature-1", "feature-2"],
        "journey-1",
        "X-CSRF-TOKEN",
        "csrf-token-1",
      )).rejects.toBe("Response status: Internal Server Error");
    });
  });

  describe("undo", () => {
    it("undo_whenResponseOk_resolvesWithoutBody", async () => {
      const fetchMock = vi.fn().mockResolvedValue({ ok: true });
      vi.stubGlobal("fetch", fetchMock);

      await expect(undo("/api/gis-framework/undo/journey-1", "X-CSRF-TOKEN", "csrf-token-1"))
        .resolves
        .toBeUndefined();

      expect(fetchMock).toHaveBeenCalledWith("/api/gis-framework/undo/journey-1", {
        method: "POST",
        headers: { "X-CSRF-TOKEN": "csrf-token-1" },
      });
    });

    it("undo_whenResponseNotOk_rejects", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(undo("/api/gis-framework/undo/journey-1", "X-CSRF-TOKEN", "csrf-token-1"))
        .rejects
        .toBe("Response status: Internal Server Error");
    });
  });

  describe("redo", () => {
    it("redo_whenResponseOk_resolvesWithoutBody", async () => {
      const fetchMock = vi.fn().mockResolvedValue({ ok: true });
      vi.stubGlobal("fetch", fetchMock);

      await expect(redo("/api/gis-framework/redo/journey-1", "X-CSRF-TOKEN", "csrf-token-1"))
        .resolves
        .toBeUndefined();

      expect(fetchMock).toHaveBeenCalledWith("/api/gis-framework/redo/journey-1", {
        method: "POST",
        headers: { "X-CSRF-TOKEN": "csrf-token-1" },
      });
    });

    it("redo_whenResponseNotOk_rejects", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(redo("/api/gis-framework/redo/journey-1", "X-CSRF-TOKEN", "csrf-token-1"))
        .rejects
        .toBe("Response status: Internal Server Error");
    });
  });
});
