import Polygon from "@arcgis/core/geometry/Polygon.js";
import { describe, expect, it } from "vitest";
import { generalizePolygon } from "../../src/geometric-operators/generalize-polygon";
import { makePolygonEsriJson } from "../test-utils/esrijson-test-util";

function ringContains(ring: number[][], point: number[]): boolean {
  return ring.some(([x, y]) => x === point[0] && y === point[1]);
}

describe("generalizePolygon", () => {
  it("should drop a collinear vertex lying on a straight edge", () => {
    // [1, 0] is the midpoint of the straight bottom edge [0,0] -> [2,0]
    const withMidpoint = makePolygonEsriJson([
      [
        [0, 0],
        [1, 0],
        [2, 0],
        [2, 2],
        [0, 2],
        [0, 0],
      ],
    ]);

    const result = Polygon.fromJSON(JSON.parse(generalizePolygon(withMidpoint)));

    expect(ringContains(result.rings[0], [1, 0])).toBe(false);
    // four corners + closing vertex remain
    expect(result.rings[0]).toHaveLength(5);
  });

  it("should leave a polygon with no collinear vertices unchanged", () => {
    const square = makePolygonEsriJson([
      [
        [0, 0],
        [2, 0],
        [2, 2],
        [0, 2],
        [0, 0],
      ],
    ]);

    const result = Polygon.fromJSON(JSON.parse(generalizePolygon(square)));

    expect(result.rings[0]).toHaveLength(5);
    expect(result.extent.xmax).toBe(2);
    expect(result.extent.ymax).toBe(2);
  });

  it("should generalize every ring including a hole", () => {
    const withHoleAndMidpoints = makePolygonEsriJson([
      [
        [0, 0],
        [5, 0],
        [10, 0],
        [10, 10],
        [0, 10],
        [0, 0],
      ],
      [
        [2, 2],
        [4, 2],
        [6, 2],
        [6, 6],
        [2, 6],
        [2, 2],
      ],
    ]);

    const result = Polygon.fromJSON(JSON.parse(generalizePolygon(withHoleAndMidpoints)));

    expect(result.rings).toHaveLength(2);
    // collinear midpoints [5,0] (outer) and [4,2] (inner) removed
    expect(ringContains(result.rings[0], [5, 0])).toBe(false);
    expect(ringContains(result.rings[1], [4, 2])).toBe(false);
  });
});
