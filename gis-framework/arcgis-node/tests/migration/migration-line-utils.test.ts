import { describe, expect, it } from "vitest";
import {
  findParentLine,
  FIVE_CM_IN_DEGREES_AT_48N_ED50,
  getLineStartAndEndPoints,
} from "../../src/migration/utils/migration-line-utils";
import { esriJsonToPolyline } from "../../src/util/esrijson-util";
import { makePolylineEsriJson } from "../test-utils/esrijson-test-util";

describe("migration-line-utils", () => {
  describe("getLineStartAndEndPoints", () => {
    it("should return the first and last points of the polyline", () => {
      const polyline = esriJsonToPolyline(
        makePolylineEsriJson([
          [
            [0, 0],
            [5, 5],
            [10, 10],
          ],
        ]),
      );

      const { startPoint, endPoint } = getLineStartAndEndPoints(polyline);

      expect(startPoint.x).toBe(0);
      expect(startPoint.y).toBe(0);
      expect(endPoint.x).toBe(10);
      expect(endPoint.y).toBe(10);
    });

    it("should throw when polyline has 0 paths", () => {
      const polyline = esriJsonToPolyline(makePolylineEsriJson([]));

      expect(() => getLineStartAndEndPoints(polyline)).toThrow("Polyline must have exactly one path");
    });
    it("should throw when polyline has more than 1 path", () => {
      const polyline = esriJsonToPolyline(
        makePolylineEsriJson([
          [
            [0, 0],
            [5, 5],
          ],
          [
            [10, 10],
            [15, 15],
          ],
        ]),
      );

      expect(() => getLineStartAndEndPoints(polyline)).toThrow("Polyline must have exactly one path");
    });
  });

  describe("findParentLine", () => {
    it("should find the closest parent line when child points are on the parent", () => {
      const parent = makePolylineEsriJson([
        [
          [0, 0],
          [10, 0],
        ],
      ]);
      const polyline = esriJsonToPolyline(
        makePolylineEsriJson([
          [
            [0, 0],
            [10, 0],
          ],
        ]),
      );
      const { startPoint, endPoint } = getLineStartAndEndPoints(polyline);

      const result = findParentLine([parent], startPoint, endPoint);

      expect(result).toBeDefined();
      expect(result).toStrictEqual(polyline);
    });

    it("should return undefined when child points are far from all parent lines", () => {
      const parent = makePolylineEsriJson([
        [
          [0, 0],
          [10, 0],
        ],
      ]);
      const polyline = esriJsonToPolyline(
        makePolylineEsriJson([
          [
            [50, 50],
            [60, 60],
          ],
        ]),
      );
      const { startPoint, endPoint } = getLineStartAndEndPoints(polyline);

      const result = findParentLine([parent], startPoint, endPoint);

      expect(result).toBeUndefined();
    });

    it("should return the closest parent when multiple parents are provided", () => {
      const farParent = makePolylineEsriJson([
        [
          [100, 100],
          [110, 100],
        ],
      ]);
      const closeParent = makePolylineEsriJson([
        [
          [0, 0],
          [5, 0],
        ],
      ]);
      const polyline = esriJsonToPolyline(
        makePolylineEsriJson([
          [
            [0, 0],
            [5, 0],
          ],
        ]),
      );
      const { startPoint, endPoint } = getLineStartAndEndPoints(polyline);

      const result = findParentLine([farParent, closeParent], startPoint, endPoint);

      expect(result).toStrictEqual(esriJsonToPolyline(closeParent));
    });

    it("should return undefined when no parent lines are provided", () => {
      const polyline = esriJsonToPolyline(
        makePolylineEsriJson([
          [
            [0, 0],
            [5, 0],
          ],
        ]),
      );
      const { startPoint, endPoint } = getLineStartAndEndPoints(polyline);

      const result = findParentLine([], startPoint, endPoint);

      expect(result).toBeUndefined();
    });
  });

  it("should return parent when parent is less than 5cm away from child line", () => {
    const offset = FIVE_CM_IN_DEGREES_AT_48N_ED50 - FIVE_CM_IN_DEGREES_AT_48N_ED50 * 0.1;
    const parent = makePolylineEsriJson([
      [
        [0, 0],
        [1, 0],
        [9, 0],
        [10, 0],
      ],
    ]);
    const polyline = esriJsonToPolyline(
      makePolylineEsriJson([
        [
          [1 + offset, 0],
          [9 - offset, 0],
        ],
      ]),
    );
    const { startPoint, endPoint } = getLineStartAndEndPoints(polyline);

    const result = findParentLine([parent], startPoint, endPoint);

    expect(result).toStrictEqual(esriJsonToPolyline(parent));
  });
});
