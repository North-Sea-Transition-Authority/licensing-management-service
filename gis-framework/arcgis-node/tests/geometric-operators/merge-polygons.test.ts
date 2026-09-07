import Polygon from "@arcgis/core/geometry/Polygon.js";
import { describe, expect, it } from "vitest";
import { mergePolygons } from "../../src/geometric-operators/merge-polygons";
import { makePolygonEsriJson } from "../test-utils/esrijson-test-util";

function square(x1: number, y1: number, x2: number, y2: number): string {
  return makePolygonEsriJson([
    [
      [x1, y1],
      [x2, y1],
      [x2, y2],
      [x1, y2],
      [x1, y1],
    ],
  ]);
}

describe("mergePolygons", () => {
  it("should union two edge-adjacent squares into a single ring", () => {
    const left = square(0, 0, 1, 1);
    const right = square(1, 0, 2, 1);

    const result = Polygon.fromJSON(JSON.parse(mergePolygons(left, right)));

    // shared edge dissolved -> one outer ring spanning the full 0..2 width
    expect(result.rings).toHaveLength(1);
    expect(result.extent.xmin).toBe(0);
    expect(result.extent.xmax).toBe(2);
    expect(result.extent.ymin).toBe(0);
    expect(result.extent.ymax).toBe(1);
  });

  it("should union two overlapping squares into a single ring", () => {
    const a = square(0, 0, 2, 2);
    const b = square(1, 1, 3, 3);

    const result = Polygon.fromJSON(JSON.parse(mergePolygons(a, b)));

    expect(result.rings).toHaveLength(1);
    expect(result.extent.xmax).toBe(3);
    expect(result.extent.ymax).toBe(3);
  });

  it("should keep both parts when the squares touch only at a corner", () => {
    const a = square(0, 0, 1, 1);
    const b = square(1, 1, 2, 2);

    const result = Polygon.fromJSON(JSON.parse(mergePolygons(a, b)));

    // point-touch is not a shared edge -> multipart, two rings survive
    expect(result.rings).toHaveLength(2);
  });

  it("should preserve the input spatial reference", () => {
    const result = Polygon.fromJSON(JSON.parse(mergePolygons(square(0, 0, 1, 1), square(1, 0, 2, 1))));

    expect(result.spatialReference?.wkid).toBe(4326);
  });
});
