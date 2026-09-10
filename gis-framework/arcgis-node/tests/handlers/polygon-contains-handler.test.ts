import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as polygonContainsModule from "../../src/geometric-operators/polygon-contains-operator";
import { polygonContainsHandler } from "../../src/handlers/polygon-contains-handler";
import { makePolygonEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/geometric-operators/polygon-contains-operator");

const containerEsriJson = makePolygonEsriJson([
  [
    [0, 0],
    [10, 0],
    [10, 10],
    [0, 10],
    [0, 0],
  ],
]);

const containedEsriJson = makePolygonEsriJson([
  [
    [2, 2],
    [8, 2],
    [8, 8],
    [2, 8],
    [2, 2],
  ],
]);

describe("polygonContainsHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        esriJsonContainerPolygon: containerEsriJson,
        esriJsonContainedPolygon: containedEsriJson,
      },
    };
  });

  it("should return true when the container polygon contains the contained polygon", async () => {
    vi.mocked(polygonContainsModule.parentContainsChild).mockReturnValue(true);

    polygonContainsHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, { contains: true }));
    expect(polygonContainsModule.parentContainsChild).toHaveBeenCalledWith(containerEsriJson, containedEsriJson);
  });

  it("should return false when the container polygon does not contain the contained polygon", async () => {
    vi.mocked(polygonContainsModule.parentContainsChild).mockReturnValue(false);

    polygonContainsHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, { contains: false }));
  });

  it("should call callback with error when parentContainsChild throws", async () => {
    const testError = new Error("Failed to check polygon containment");
    vi.mocked(polygonContainsModule.parentContainsChild).mockImplementation(() => {
      throw testError;
    });

    polygonContainsHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledOnce());
    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.message).toBe("Failed to check polygon containment");
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
  });
});
