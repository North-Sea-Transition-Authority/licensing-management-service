import { status } from "@grpc/grpc-js";
import { describe, expect, it, vi } from "vitest";
import { CoordinateSystem } from "../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";
import { validateTopologicallyEqual } from "../../../src/migration/handlers/validate-topologically-equal";
import { makePolylineEsriJson } from "../../test-utils/esrijson-test-util";

function makeGrpcInput(x1, y1, x2, y2) {
  return [
    {
      esriJsonPolyline: [
        makePolylineEsriJson([
          [
            [x1, y1],
            [x2, y1],
          ],
        ]),
        makePolylineEsriJson([
          [
            [x2, y1],
            [x2, y2],
          ],
        ]),
        makePolylineEsriJson([
          [
            [x2, y2],
            [x1, y2],
          ],
        ]),
        makePolylineEsriJson([
          [
            [x1, y2],
            [x1, y1],
          ],
        ]),
      ],
    },
  ];
}

describe("validateTopologicallyEqual", () => {
  it("should return valid when child and parent polygons are topologically equal", async () => {
    const polygonAsLines = makeGrpcInput(0, 0, 10, 10);

    const call = {
      request: {
        childPolygons: polygonAsLines,
        parentPolygons: polygonAsLines,
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateTopologicallyEqual(call, callback);

    expect(callback).toHaveBeenCalledWith(null, { isValid: true });
  });

  it("should return not valid when polygons are not topologically equal", async () => {
    const childPolygons = makeGrpcInput(0, 0, 10, 10);
    const parentPolygons = makeGrpcInput(0, 0, 20, 20);

    const call = {
      request: {
        childPolygons,
        parentPolygons,
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateTopologicallyEqual(call, callback);

    expect(callback).toHaveBeenCalledWith(null, {
      isValid: false,
      message: "Polygons are not topologically equal",
    });
  });

  it("returns internal error when validation throws", async () => {
    const polygonAsLines = makeGrpcInput(0, 0, 10, 10);

    const call = {
      request: {
        childPolygons: polygonAsLines,
        parentPolygons: polygonAsLines,
        coordinateSystem: CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED,
      },
    };

    const callback = vi.fn();

    await validateTopologicallyEqual(call, callback);

    const callbackError = callback.mock.calls[0][0];
    expect(callbackError.message).toBe(`Could not determine wkid for ${CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED}`);
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(callback).toHaveBeenCalledWith(callbackError, null);
    expect(callback).toHaveBeenCalledOnce();
  });
});
