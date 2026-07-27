import { afterEach, describe, expect, it, vi } from "vitest";
import { getOutlineNodes, getTextualDescription } from "../../../../main/resources/js/api/features.api";

describe("featuresApi", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe("getOutlineNodes", () => {
    it("getOutlineNodes_whenResponseOk", async () => {
      const expected = [
        { featureId: "feature-1", nodes: [] },
        { featureId: "feature-2", nodes: [] },
      ];
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({ featureOutlineNodes: expected }),
      });
      vi.stubGlobal("fetch", fetchMock);

      const result = await getOutlineNodes("/api/gis-framework/outline-nodes?featureId=feature-1&featureId=feature-2");

      expect(result).toEqual(expected);
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/gis-framework/outline-nodes?featureId=feature-1&featureId=feature-2",
      );
    });

    it("getOutlineNodes_whenResponseNotOk", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(getOutlineNodes("/api/gis-framework/outline-nodes?featureId=feature-1")).rejects.toBe("Response status: Internal Server Error");
    });
  });

  describe("getTextualDescription", () => {
    it("getTextualDescription_whenResponseOk", async () => {
      const expected = "SUBAREAS 30/1a is defined as 1 region:\n\nRegion 1 of SUBAREAS 30/1a is bounded by the following coordinates:";
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({ textualDescription: expected }),
      });
      vi.stubGlobal("fetch", fetchMock);

      const result = await getTextualDescription("/api/gis-framework/textual-description?featureId=feature-1");

      expect(result).toEqual(expected);
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/gis-framework/textual-description?featureId=feature-1",
      );
    });

    it("getTextualDescription_whenResponseNotOk", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(getTextualDescription("/api/gis-framework/textual-description?featureId=feature-1")).rejects.toBe("Response status: Internal Server Error");
    });
  });
});
