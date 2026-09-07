import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as mergePolygonsModule from "../../src/geometric-operators/merge-polygons";
import { mergePolygonsHandler } from "../../src/handlers/merge-polygons-handler";

vi.mock("../../src/geometric-operators/merge-polygons");

describe("mergePolygonsHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        inputPolygon1: "input polygon 1",
        inputPolygon2: "input polygon 2",
      },
    };
  });

  it("should merge the two input polygons and return the result", async () => {
    vi.mocked(mergePolygonsModule.mergePolygons).mockReturnValue("merged polygon");

    mergePolygonsHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, { resultPolygon: "merged polygon" }));
    expect(mergePolygonsModule.mergePolygons).toHaveBeenCalledWith("input polygon 1", "input polygon 2");
  });

  it("should call callback with error when mergePolygons throws", async () => {
    const testError = new Error("Failed to merge polygons");
    vi.mocked(mergePolygonsModule.mergePolygons).mockImplementation(() => {
      throw testError;
    });

    mergePolygonsHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(testError, null));
    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledOnce();
  });
});
