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
        linesWithNavigationType: [],
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
    mockCall.request.linesWithNavigationType = [
      { esriJsonPolyline: firstLineEsriJson, lineNavigationType: LineNavigationType.LOXODROME },
      { esriJsonPolyline: secondLineEsriJson, lineNavigationType: LineNavigationType.GEODESIC },
    ];

    vi.mocked(calculateAreaModule.densifyLoxodromesAndCalculateArea).mockResolvedValue(123.45);

    await calculateAreaHandler(mockCall, mockCallback as any);

    expect(calculateAreaModule.densifyLoxodromesAndCalculateArea).toHaveBeenCalledWith(
      [
        { line: esriJsonToPolyline(firstLineEsriJson), navigationType: LineNavigationType.LOXODROME },
        { line: esriJsonToPolyline(secondLineEsriJson), navigationType: LineNavigationType.GEODESIC },
      ],
      CoordinateSystem.ED50,
    );
    expect(mockCallback).toHaveBeenCalledWith(null, { area: 123.45 });
  });

  it("should call callback with error when densifyLoxodromesAndCalculateArea throws", async () => {
    const lineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [0, 10],
      ],
    ]);
    mockCall.request.linesWithNavigationType = [
      { esriJsonPolyline: lineEsriJson, lineNavigationType: LineNavigationType.LOXODROME },
    ];
    const testError = new Error("Failed to calculate area");

    vi.mocked(calculateAreaModule.densifyLoxodromesAndCalculateArea).mockRejectedValue(testError);

    await calculateAreaHandler(mockCall, mockCallback as any);

    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.message).toBe("Failed to calculate area");
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
  });
});
