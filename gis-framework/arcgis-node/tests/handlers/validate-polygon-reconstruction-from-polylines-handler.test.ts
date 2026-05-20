import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as validatePolygonReconstructionFromPolylinesModule
  from "../../src/geometric-operators/validate-polygon-reconstruction-from-polylines";
import {
  validatePolygonReconstructionFromPolylinesHandler,
} from "../../src/handlers/validate-polygon-reconstruction-from-polylines-handler";
import { esriJsonToPolyline } from "../../src/util/esrijson-util";
import { makePolygonEsriJson, makePolylineEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/geometric-operators/validate-polygon-reconstruction-from-polylines");
vi.mock("../../src/util/esrijson-util");
vi.mock("../../src/config/logger", () => ({
  logger: {
    error: vi.fn(),
  },
}));

describe("validatePolygonReconstructionFromPolylinesHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        lines: [],
        originalPolygonEsriJson: null,
      },
    };
  });

  it("should return a successful callback when polygon reconstruction is valid", () => {
    const firstLineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [0, 10],
      ],
    ]);
    const secondLineEsriJson = makePolylineEsriJson([
      [
        [0, 10],
        [10, 10],
      ],
    ]);
    const originalPolygonEsriJson = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);
    mockCall.request.lines = [
      { polylineEsriJson: firstLineEsriJson, ringNumber: 0, connectionOrder: 1 },
      { polylineEsriJson: secondLineEsriJson, ringNumber: 0, connectionOrder: 2 },
    ];
    mockCall.request.originalPolygonEsriJson = originalPolygonEsriJson;

    vi.mocked(validatePolygonReconstructionFromPolylinesModule.validatePolygonReconstructionFromPolylines).mockReturnValue(true);

    validatePolygonReconstructionFromPolylinesHandler(mockCall, mockCallback as any);

    expect(validatePolygonReconstructionFromPolylinesModule.validatePolygonReconstructionFromPolylines).toHaveBeenCalledWith(
      [
        { polyline: esriJsonToPolyline(firstLineEsriJson), ringNumber: 0, connectionOrder: 1 },
        { polyline: esriJsonToPolyline(secondLineEsriJson), ringNumber: 0, connectionOrder: 2 },
      ],
      originalPolygonEsriJson,
    );
    expect(mockCallback).toHaveBeenCalledWith(null, { isValid: true });
  });

  it("should return false when polygon reconstruction is invalid", () => {
    const lineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [0, 10],
      ],
    ]);
    const originalPolygonEsriJson = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);
    mockCall.request.lines = [{ polylineEsriJson: lineEsriJson, ringNumber: 0, connectionOrder: 1 }];
    mockCall.request.originalPolygonEsriJson = originalPolygonEsriJson;
    vi.mocked(validatePolygonReconstructionFromPolylinesModule.validatePolygonReconstructionFromPolylines).mockReturnValue(false);

    validatePolygonReconstructionFromPolylinesHandler(mockCall, mockCallback as any);

    expect(mockCallback).toHaveBeenCalledWith(null, { isValid: false });
  });

  it("should call callback with error when validatePolygonReconstructionFromPolylines throws", () => {
    const lineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [0, 10],
      ],
    ]);
    mockCall.request.lines = [{ polylineEsriJson: lineEsriJson, ringNumber: 0, connectionOrder: 1 }];
    mockCall.request.originalPolygonEsriJson = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);
    const testError = new Error("Failed to validate polygon reconstruction");

    vi.mocked(validatePolygonReconstructionFromPolylinesModule.validatePolygonReconstructionFromPolylines).mockImplementation(
      () => {
        throw testError;
      },
    );

    validatePolygonReconstructionFromPolylinesHandler(mockCall, mockCallback as any);

    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.message).toBe("Failed to validate polygon reconstruction");
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
  });
});
