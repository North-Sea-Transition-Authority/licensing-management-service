import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as generalizePolygonModule from "../../src/geometric-operators/generalize-polygon";
import { generalizePolygonHandler } from "../../src/handlers/generalize-polygon-handler";

vi.mock("../../src/geometric-operators/generalize-polygon");

describe("generalizePolygonHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        esriPolygon: "input polygon",
      },
    };
  });

  it("should generalize the input polygon and return the result", async () => {
    vi.mocked(generalizePolygonModule.generalizePolygon).mockReturnValue("generalized polygon");

    generalizePolygonHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, { esriPolygon: "generalized polygon" }));
    expect(generalizePolygonModule.generalizePolygon).toHaveBeenCalledWith("input polygon");
  });

  it("should call callback with error when generalizePolygon throws", async () => {
    const testError = new Error("Failed to generalize polygon");
    vi.mocked(generalizePolygonModule.generalizePolygon).mockImplementation(() => {
      throw testError;
    });

    generalizePolygonHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(testError, null));
    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledOnce();
  });
});
