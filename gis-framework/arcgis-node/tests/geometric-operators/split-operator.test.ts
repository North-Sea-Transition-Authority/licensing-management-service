import * as equalsOperator from "@arcgis/core/geometry/operators/equalsOperator.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import { describe, expect, it } from "vitest";
import { splitPolygon } from "../../src/geometric-operators/split-operator.js";

const spatialReference = { wkid: 27700 };

const targetPolygon = new Polygon({
  rings: [
    [
      [0, 0],
      [0, 10],
      [10, 10],
      [10, 0],
      [0, 0],
    ],
  ],
  spatialReference,
});
describe("splitPolygon", () => {
  it("should split a polygon into two when cut by a line", () => {
    const cutter = new Polyline({
      paths: [
        [
          [0, 0],
          [10, 10],
        ],
      ],
      spatialReference,
    });

    const polygons = splitPolygon(targetPolygon, cutter);
    expect(polygons).toHaveLength(2);
    const mergedResult = unionOperator.execute(polygons[0], polygons[1]) as Polygon;
    expect(equalsOperator.execute(mergedResult, targetPolygon)).toBe(true);
  });

  it("should return empty when cutter does not intersect the polygon", () => {
    const cutter = new Polyline({
      paths: [
        [
          [20, 20],
          [30, 30],
        ],
      ],
      spatialReference,
    });

    const polygons = splitPolygon(targetPolygon, cutter);
    expect(polygons).toHaveLength(0);
  });

  it("should handle a cutter with overlapping/backtracking segments", () => {
    const cutter = new Polyline({
      paths: [
        [
          [0, 5],
          [10, 5],
          [1, 5], // Line backtracks
        ],
      ],
      spatialReference,
    });

    const polygons = splitPolygon(targetPolygon, cutter);
    expect(polygons).toHaveLength(2);
    const mergedResult = unionOperator.execute(polygons[0], polygons[1]) as Polygon;
    expect(equalsOperator.execute(mergedResult, targetPolygon)).toBe(true);
    polygons.forEach((poly) => {
      expect(poly.rings[0].length).toBe(5);
    });
  });
});
