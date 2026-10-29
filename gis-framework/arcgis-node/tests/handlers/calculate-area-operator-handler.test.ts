import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CoordinateSystem } from "../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";
import { LineNavigationType } from "../../generated/uk/co/fivium/grpc/gis/LineNavigationType";
import * as calculateAreaModule from "../../src/geometric-operators/calculate-area-operator";
import { calculateAreaHandler } from "../../src/handlers/calculate-area-operator-handler";
import { esriJsonToPolyline } from "../../src/util/esrijson-util";
import { makePolylineEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/geometric-operators/calculate-area-operator");
vi.mock("../../src/util/esrijson-util");
vi.mock("../../src/config/logger", () => ({
  logger: {
    error: vi.fn(),
  },
}));

describe("calculateAreaHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        polygons: [],
        coordinateSystem: CoordinateSystem.ED50,
      },
    };
  });

  it("should return a successful callback with the calculated area", async () => {
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
    mockCall.request.polygons = [
      { lines: [{ esriJsonPolyline: firstLineEsriJson, lineNavigationType: LineNavigationType.LOXODROME }] },
      { lines: [{ esriJsonPolyline: secondLineEsriJson, lineNavigationType: LineNavigationType.GEODESIC }] },
    ];

    vi.mocked(calculateAreaModule.densifyLoxodromesUnionAndCalculateArea).mockResolvedValue(123.45);

    calculateAreaHandler(mockCall, mockCallback as any);
    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, { area: 123.45 }));

    expect(calculateAreaModule.densifyLoxodromesUnionAndCalculateArea).toHaveBeenCalledWith(
      [
        [{ line: esriJsonToPolyline(firstLineEsriJson), navigationType: LineNavigationType.LOXODROME }],
        [{ line: esriJsonToPolyline(secondLineEsriJson), navigationType: LineNavigationType.GEODESIC }],
      ],
      CoordinateSystem.ED50,
    );
  });

  it("should call callback with error when densifyLoxodromesUnionAndCalculateArea throws", async () => {
    const lineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [0, 10],
      ],
    ]);
    mockCall.request.polygons = [
      { lines: [{ esriJsonPolyline: lineEsriJson, lineNavigationType: LineNavigationType.LOXODROME }] },
    ];
    const testError = new Error("Failed to calculate area");

    vi.mocked(calculateAreaModule.densifyLoxodromesUnionAndCalculateArea).mockRejectedValue(testError);

    calculateAreaHandler(mockCall, mockCallback as any);
    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(testError, null));

    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError.message).toBe("Failed to calculate area");
    expect(callbackError.code).toBe(status.INTERNAL);
  });
});
