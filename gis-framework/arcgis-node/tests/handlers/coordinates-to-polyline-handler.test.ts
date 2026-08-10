import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as coordinatesToPolylineModule from "../../src/geometric-operators/coordinates-to-polyline";
import { coordinatesToPolylineHandler } from "../../src/handlers/coordinates-to-polyline-handler";

vi.mock("../../src/geometric-operators/coordinates-to-polyline");

describe("coordinatesToPolylineHandler", () => {
  let mockCallback: any;
  let mockCall: any;
  const testWkid = 4230;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        coordinates: [],
        srsWkid: testWkid,
      },
    };
  });

  it("should return a successful callback with the built polyline EsriJSON", () => {
    const coordinates = [
      { x: 0, y: 0 },
      { x: 10, y: 0 },
    ];
    mockCall.request.coordinates = coordinates;

    const polylineEsriJson = JSON.stringify({ paths: [[[0, 0], [10, 0]]] });
    vi.mocked(coordinatesToPolylineModule.coordinatesToPolyline).mockReturnValue(polylineEsriJson);

    coordinatesToPolylineHandler(mockCall, mockCallback as any);

    expect(coordinatesToPolylineModule.coordinatesToPolyline).toHaveBeenCalledWith(coordinates, testWkid);
    expect(mockCallback).toHaveBeenCalledWith(null, { polylineEsriJson });
    expect(mockCallback).toHaveBeenCalledOnce();
  });

  it("should call callback with error when coordinatesToPolyline throws", () => {
    mockCall.request.coordinates = [{ x: 0, y: 0 }];

    const testError = new Error("Failed to build polyline");
    vi.mocked(coordinatesToPolylineModule.coordinatesToPolyline).mockImplementation(() => {
      throw testError;
    });

    coordinatesToPolylineHandler(mockCall, mockCallback as any);

    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.message).toBe("Failed to build polyline");
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
    expect(mockCallback).toHaveBeenCalledOnce();
  });
});
