import { afterEach, describe, expect, it, vi } from "vitest";
import { getSplitHistoryStatus } from "../../../../main/resources/js/api/split-history.api";

describe("splitHistoryApi", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe("getSplitHistoryStatus", () => {
    it("getSplitHistoryStatus_whenResponseOk_returnsResponse", async () => {
      const expected = { canUndo: true };
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(expected),
      });
      vi.stubGlobal("fetch", fetchMock);

      const result = await getSplitHistoryStatus("/api/gis-framework/split-history/journey-1");

      expect(result).toEqual(expected);
      expect(fetchMock).toHaveBeenCalledWith("/api/gis-framework/split-history/journey-1");
    });

    it("getSplitHistoryStatus_whenResponseNotOk_rejects", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(getSplitHistoryStatus("/api/gis-framework/split-history/journey-1"))
        .rejects
        .toBe("Response status: Internal Server Error");
    });
  });
});
