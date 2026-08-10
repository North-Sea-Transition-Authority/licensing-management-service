import { describe, expect, it } from "vitest";
import { coordinatesToPolyline } from "../../src/geometric-operators/coordinates-to-polyline";

describe("coordinates-to-polyline", () => {
  describe("coordinatesToPolyline", () => {
    it("builds a polyline that passes through the given coordinates, in the given spatial reference", () => {
      const polylineEsriJson = coordinatesToPolyline(
        [
          { x: 0, y: 0 },
          { x: 10, y: 0 },
          { x: 10, y: 10 },
        ],
        4230,
      );

      const polyline = JSON.parse(polylineEsriJson);
      expect(polyline.paths).toEqual([[[0, 0], [10, 0], [10, 10]]]);
      expect(polyline.spatialReference.wkid).toBe(4230);
    });
  });
});
