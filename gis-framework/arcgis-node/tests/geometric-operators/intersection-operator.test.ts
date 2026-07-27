import type Polygon from "@arcgis/core/geometry/Polygon.js";
import { describe, expect, it } from "vitest";
import { intersectPolygons } from "../../src/geometric-operators/intersection-operator";
import { polygonsAreTopologicallyEqual } from "../../src/geometric-operators/polygon-equality-operator";

const BNG_WKID = 27700;

/**
 * Builds an EsriJSON polygon string for an axis-aligned rectangle, in the British National Grid coordinate system.
 */
function rectangleEsriJson(minX: number, minY: number, maxX: number, maxY: number): string {
  const rings = [[
    [minX, minY],
    [minX, maxY],
    [maxX, maxY],
    [maxX, minY],
    [minX, minY],
  ]];

  return JSON.stringify({ rings, spatialReference: { wkid: BNG_WKID } });
}

function toEsriJson(polygon: Polygon): string {
  return JSON.stringify(polygon.toJSON());
}

describe("intersectPolygons", () => {
  const boundary = rectangleEsriJson(0, 0, 10, 10);

  it("intersectPolygons_whenPolygonFullyInsideOtherPolygon_assertPolygonUnchanged", () => {
    const polygon = rectangleEsriJson(2, 2, 4, 4);

    const result = intersectPolygons(polygon, boundary);

    expect(result).not.toBeNull();
    expect(polygonsAreTopologicallyEqual(toEsriJson(result as Polygon), polygon)).toBe(true);
  });

  it("intersectPolygons_whenPolygonFullyOutsideOtherPolygon_assertNullReturned", () => {
    const polygon = rectangleEsriJson(20, 20, 24, 24);

    const result = intersectPolygons(polygon, boundary);

    expect(result).toBeNull();
  });

  it("intersectPolygons_whenPolygonPartiallyInsideOtherPolygon_assertOnlyOverlappingPortionReturned", () => {
    // Straddles the boundary's right edge (x = 10): half inside, half outside.
    const polygon = rectangleEsriJson(8, 2, 14, 6);
    const expectedIntersection = rectangleEsriJson(8, 2, 10, 6);

    const result = intersectPolygons(polygon, boundary);

    expect(result).not.toBeNull();
    expect(polygonsAreTopologicallyEqual(toEsriJson(result as Polygon), expectedIntersection)).toBe(true);
  });

  it("intersectPolygons_whenPolygonOutsideButEdgeTouchesOtherPolygon_assertNullReturned", () => {
    // Sits entirely outside the boundary (x >= 10), but its left edge runs exactly along the boundary's right
    // edge (x = 10, y 2-6). The shared boundary is a zero-area line, so there is no area to retain.
    const polygon = rectangleEsriJson(10, 2, 14, 6);

    const result = intersectPolygons(polygon, boundary);

    expect(result).toBeNull();
  });

  it("intersectPolygons_whenPolygonOutsideButCornerTouchesOtherPolygon_assertNullReturned", () => {
    // Touches the boundary at a single point (10, 10) only.
    const polygon = rectangleEsriJson(10, 10, 14, 14);

    const result = intersectPolygons(polygon, boundary);

    expect(result).toBeNull();
  });

  it("intersectPolygons_whenPolygonFullyInsideButEdgeTouchesOtherPolygon_assertPolygonUnchanged", () => {
    // Fully inside the boundary, but drawn flush against it: shares the boundary's right edge (x = 10, y 2-6).
    // Touching the edge from the inside must not cause any part of the polygon to be excluded.
    const polygon = rectangleEsriJson(6, 2, 10, 6);

    const result = intersectPolygons(polygon, boundary);

    expect(result).not.toBeNull();
    expect(polygonsAreTopologicallyEqual(toEsriJson(result as Polygon), polygon)).toBe(true);
  });
});
