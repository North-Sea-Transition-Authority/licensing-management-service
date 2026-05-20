import { describe, expect, it } from "vitest";
import { CoordinateSystem } from "../../generated/uk/co/fivium/grpc/gis/CoordinateSystem.ts";
import { getCoordinateSystemWkid } from "../../src/util/coordinate-system-utils.ts";

describe("coordinate-system-utils", () => {
  it.each([
    [CoordinateSystem.ED50, 4230],
    [CoordinateSystem.BRITISH_NATIONAL_GRID, 27700],
    [CoordinateSystem.WGS84, 4326],
  ])("getCoordinateSystemWkid(%s) === %s", (coordinateSystem, wkid) => {
    expect(getCoordinateSystemWkid(coordinateSystem)).toEqual(wkid);
  });

  it("unknown coordinate system throws error", () => {
    expect(() => getCoordinateSystemWkid(CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED)).toThrow(
      new Error(`Could not determine wkid for ${CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED}`),
    );
  });
});
