import { describe, expect, it } from "vitest";
import { SupportedWkid } from "../../../main/resources/js/coordinate-system-utils";
import { generateSnapPoints } from "../../../main/resources/js/grid-utils";

describe("generateSnapPoints", () => {
  it("generates the expected snap points for a ED50 grid", () => {
    // Extent spans ED50 indices 120-121 in X (1.0°-1.0083°) and 6240-6241 in Y
    // (52.0°-52.0083°), producing 4 points in X-outer, Y-inner order.
    const points = generateSnapPoints(0.998, 51.998, 1.008, 52.007, SupportedWkid.ED50_WKID);

    expect(points).toEqual([
      { id: "120,6240", coordinates: [expect.closeTo(0.9986, 2), expect.closeTo(51.9992, 2)], originalSrsCoordinates: [expect.closeTo(1.0, 4), expect.closeTo(52.0, 4)] },
      { id: "120,6241", coordinates: [expect.closeTo(0.9986, 2), expect.closeTo(52.0075, 2)], originalSrsCoordinates: [expect.closeTo(1.0, 4), expect.closeTo(52.0083, 4)] },
      { id: "121,6240", coordinates: [expect.closeTo(1.0069, 2), expect.closeTo(51.9992, 2)], originalSrsCoordinates: [expect.closeTo(1.0083, 4), expect.closeTo(52.0, 4)] },
      { id: "121,6241", coordinates: [expect.closeTo(1.0069, 2), expect.closeTo(52.0075, 2)], originalSrsCoordinates: [expect.closeTo(1.0083, 4), expect.closeTo(52.0083, 4)] },
    ]);
  });

  it("generates the expected snap points for a BNG grid", () => {
    // Extent spans ED50 indices 120-121 in X (1.0°-1.0083°) and 6240-6241 in Y
    // (52.0°-52.0083°), producing 4 points in X-outer, Y-inner order.
    const points = generateSnapPoints(0.998, 51.998, 1.008, 52.007, SupportedWkid.BNG_WKID);

    expect(points).toEqual([
      { id: "1212,475", coordinates: [expect.closeTo(0.9993, 2), expect.closeTo(51.9977, 2)], originalSrsCoordinates: [606000, 237500] },
      { id: "1212,476", coordinates: [expect.closeTo(0.9996, 2), expect.closeTo(52.0022, 2)], originalSrsCoordinates: [606000, 238000] },
      { id: "1212,477", coordinates: [expect.closeTo(0.9999, 2), expect.closeTo(52.0066, 2)], originalSrsCoordinates: [606000, 238500] },
      { id: "1213,475", coordinates: [expect.closeTo(1.0066, 2), expect.closeTo(51.9975, 2)], originalSrsCoordinates: [606500, 237500] },
      { id: "1213,476", coordinates: [expect.closeTo(1.00695, 2), expect.closeTo(52.0020, 2)], originalSrsCoordinates: [606500, 238000] },
      { id: "1213,477", coordinates: [expect.closeTo(1.0072, 2), expect.closeTo(52.0065, 2)], originalSrsCoordinates: [606500, 238500] },
    ]);
  });

  it("generates fewer snap points with a larger custom ED50 spacing", () => {
    const defaultPoints = generateSnapPoints(0.0, 50.0, 1.0, 51.0, SupportedWkid.ED50_WKID);
    const largerSpacingPoints = generateSnapPoints(0.0, 50.0, 1.0, 51.0, SupportedWkid.ED50_WKID, 3600);
    expect(defaultPoints.length).toBeGreaterThan(largerSpacingPoints.length);
  });

  it("generates fewer snap points with a larger custom BNG spacing", () => {
    const defaultPoints = generateSnapPoints(-2.1, 53.9, -1.9, 54.1, SupportedWkid.BNG_WKID);
    const largerSpacingPoints = generateSnapPoints(-2.1, 53.9, -1.9, 54.1, SupportedWkid.BNG_WKID, 1000);
    expect(defaultPoints.length).toBeGreaterThan(largerSpacingPoints.length);
  });

  it.for([0, -1])("rejects non-positive custom spacing: %d", (spacing) => {
    expect(() => generateSnapPoints(0, 50, 1, 51, SupportedWkid.ED50_WKID, spacing))
      .toThrow(`Grid spacing must be greater than zero: ${spacing}`);
  });
});
