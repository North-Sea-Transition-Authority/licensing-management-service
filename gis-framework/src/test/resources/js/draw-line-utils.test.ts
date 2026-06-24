import type { LinePoint } from "../../../main/resources/js/grid-utils";
import { describe, expect, it } from "vitest";
import { isOrthogonalSegment } from "../../../main/resources/js/draw-line-utils";

describe("isOrthogonalSegment", () => {
  it("returns true for points on the same X axis", () => {
    const point1: LinePoint = { coordinates: [1, 2], originalSrsCoordinates: [1, 2] };
    const point2: LinePoint = { coordinates: [1, 3], originalSrsCoordinates: [1, 3] };
    expect(isOrthogonalSegment(point1, point2)).toBe(true);
  });

  it("returns true for points on the same Y axis", () => {
    const point1: LinePoint = { coordinates: [2, 2], originalSrsCoordinates: [2, 2] };
    const point2: LinePoint = { coordinates: [1, 2], originalSrsCoordinates: [1, 2] };
    expect(isOrthogonalSegment(point1, point2)).toBe(true);
  });

  it("returns false for points on different axes", () => {
    const point1: LinePoint = { coordinates: [1, 2], originalSrsCoordinates: [1, 2] };
    const point2: LinePoint = { coordinates: [3, 4], originalSrsCoordinates: [3, 4] };
    expect(isOrthogonalSegment(point1, point2)).toBe(false);
  });
});
