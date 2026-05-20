import Polyline from "@arcgis/core/geometry/Polyline.js";
import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as explodePolygonModule from "../../src/geometric-operators/explode-polygon";
import { explodePolygonHandler } from "../../src/handlers/explode-polygon-handler";
import { makePolygonEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/geometric-operators/explode-polygon");

describe("explodePolygonHandler", () => {
  let mockCallback: any;
  let mockCall: any;
  const testWkid = 4326;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        esriJsonPolygon: null,
      },
    };
  });

  it("should return a successful callback with exploded polylines", () => {
    const polygonEsriJson = makePolygonEsriJson([
      [
        [0, 0],
        [2, 0],
        [2, 2],
        [0, 2],
        [0, 0],
      ],
    ]);
    mockCall.request.esriJsonPolygon = polygonEsriJson;

    const mockPolyline1 = new Polyline({
      paths: [
        [
          [0, 0],
          [2, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    const mockPolyline2 = new Polyline({
      paths: [
        [
          [2, 0],
          [2, 2],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    vi.mocked(explodePolygonModule.explodePolygon).mockReturnValue([mockPolyline1, mockPolyline2]);

    explodePolygonHandler(mockCall, mockCallback as any);

    expect(explodePolygonModule.explodePolygon).toHaveBeenCalledOnce();
    expect(mockCallback).toHaveBeenCalledWith(null, {
      esriJsonLines: [JSON.stringify(mockPolyline1.toJSON()), JSON.stringify(mockPolyline2.toJSON())],
    });
  });

  it("should return empty array when explodePolygon returns empty array", () => {
    mockCall.request.esriJsonPolygon = makePolygonEsriJson([]);
    vi.mocked(explodePolygonModule.explodePolygon).mockReturnValue([]);

    explodePolygonHandler(mockCall, mockCallback as any);

    expect(mockCallback).toHaveBeenCalledWith(null, { esriJsonLines: [] });
  });

  it("should call callback with error when explodePolygon throws", () => {
    mockCall.request.esriJsonPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [2, 0],
        [2, 2],
        [0, 2],
        [0, 0],
      ],
    ]);

    const testError = new Error("Failed to explode polygon");
    vi.mocked(explodePolygonModule.explodePolygon).mockImplementation(() => {
      throw testError;
    });

    explodePolygonHandler(mockCall, mockCallback as any);

    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.message).toBe("Failed to explode polygon");
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
    expect(mockCallback).toHaveBeenCalledOnce();
  });
});
