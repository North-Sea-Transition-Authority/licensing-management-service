import { afterEach, describe, expect, it, vi } from "vitest";
import { getHistoryStatus } from "@/api/history.api";

describe("historyApi", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe("getHistoryStatus", () => {
    it("getHistoryStatus_whenResponseOk_returnsResponse", async () => {
      const expected = { canUndo: true };
      const fetchMock = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(expected),
      });
      vi.stubGlobal("fetch", fetchMock);

      const result = await getHistoryStatus("/api/gis-framework/history/journey-1");

      expect(result).toEqual(expected);
      expect(fetchMock).toHaveBeenCalledWith("/api/gis-framework/history/journey-1");
    });

    it("getHistoryStatus_whenResponseNotOk_rejects", async () => {
      const fetchMock = vi.fn().mockResolvedValue({
        ok: false,
        statusText: "Internal Server Error",
      });
      vi.stubGlobal("fetch", fetchMock);

      await expect(getHistoryStatus("/api/gis-framework/history/journey-1"))
        .rejects
        .toBe("Response status: Internal Server Error");
    });
  });
});
