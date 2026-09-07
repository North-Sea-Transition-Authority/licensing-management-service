import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as mergeAndGeneralizeLinesModule from "../../src/geometric-operators/merge-and-generalize-lines";
import { mergeAndGeneralizeLinesHandler } from "../../src/handlers/merge-and-generalize-lines-handler";

vi.mock("../../src/geometric-operators/merge-and-generalize-lines");

describe("mergeAndGeneralizeLinesHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        esriPolylines: ["polyline 1", "polyline 2"],
      },
    };
  });

  it("should merge and generalize the input polylines and return the result", async () => {
    vi.mocked(mergeAndGeneralizeLinesModule.mergeAndGeneralizeLines).mockReturnValue("merged polyline");

    mergeAndGeneralizeLinesHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, { esriPolyline: "merged polyline" }));
    expect(mergeAndGeneralizeLinesModule.mergeAndGeneralizeLines).toHaveBeenCalledWith(["polyline 1", "polyline 2"]);
  });

  it("should call callback with error when mergeAndGeneralizeLines throws", async () => {
    const testError = new Error("Failed to merge lines");
    vi.mocked(mergeAndGeneralizeLinesModule.mergeAndGeneralizeLines).mockImplementation(() => {
      throw testError;
    });

    mergeAndGeneralizeLinesHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(testError, null));
    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledOnce();
  });
});
