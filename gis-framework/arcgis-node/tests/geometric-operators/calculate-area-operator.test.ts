import type { LineWithNavigationTypeAndId } from "../../src/migration/types/line-with-navigation-wrapper";
import { describe, expect, it } from "vitest";
import { CoordinateSystem } from "../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";
import { LineNavigationType } from "../../generated/uk/co/fivium/grpc/gis/LineNavigationType";
import { calculateArea, densifyLoxodromesAndCalculateArea } from "../../src/geometric-operators/calculate-area-operator";
import { getCoordinateSystemWkid } from "../../src/util/coordinate-system-utils";
import { makeLineWithNavigationAndId, makePolygon, makePolyline } from "../test-utils/esrijson-test-util";
import { ED50_MIXED_POLYGON_AREA, ED50_MIXED_POLYLINES } from "../test-utils/known-test-shapes";

const SQUARE_RING = [
  [0, 0],
  [10, 0],
  [10, 10],
  [0, 10],
  [0, 0],
];

const DONUT_HOLE_RING = [
  [3, 3],
  [3, 7],
  [7, 7],
  [7, 3],
  [3, 3],
];

const ED50_SQUARE_RING = [
  [0, 0],
  [1, 0],
  [1, 1],
  [0, 1],
  [0, 0],
];

const ED50_DONUT_HOLE_RING = [
  [0.3, 0.3],
  [0.3, 0.7],
  [0.7, 0.7],
  [0.7, 0.3],
  [0.3, 0.3],
];

const BNG_WKID = getCoordinateSystemWkid(CoordinateSystem.BRITISH_NATIONAL_GRID);
const ED50_WKID = getCoordinateSystemWkid(CoordinateSystem.ED50);

describe("calculate-area-operator", () => {
  describe("calculateArea", () => {
    it("should calculate area for a polygon in BNG", async () => {
      const polygon = makePolygon([SQUARE_RING], BNG_WKID);

      const area = await calculateArea(polygon, CoordinateSystem.BRITISH_NATIONAL_GRID);

      expect(area).toBe(100);
    });

    it("should calculate area for a donut polygon in BNG", async () => {
      const polygon = makePolygon([SQUARE_RING, DONUT_HOLE_RING], BNG_WKID);

      const area = await calculateArea(polygon, CoordinateSystem.BRITISH_NATIONAL_GRID);

      expect(area).toBe(84);
    });

    it("should calculate area for a polygon in ED50", async () => {
      const polygon = makePolygon([ED50_SQUARE_RING], ED50_WKID);

      const area = await calculateArea(polygon, CoordinateSystem.ED50);

      expect(area).toBe(12309396644.519192);
    });

    it("should calculate area for a donut polygon in ED50", async () => {
      const polygon = makePolygon([ED50_SQUARE_RING, ED50_DONUT_HOLE_RING], ED50_WKID);

      const area = await calculateArea(polygon, CoordinateSystem.ED50);

      expect(area).toBe(10339915015.231005);
    });
  });

  describe("densifyLoxodromesAndCalculateArea", () => {
    it("should calculate area for a polygon in BNG", async () => {
      const lines: LineWithNavigationTypeAndId[] = [
        makeLineWithNavigationAndId(
          makePolyline(
            [
              [
                [0, 0],
                [10, 0],
              ],
            ],
            BNG_WKID,
          ),
          LineNavigationType.CARTESIAN,
          1,
        ),
        makeLineWithNavigationAndId(
          makePolyline(
            [
              [
                [10, 0],
                [10, 10],
              ],
            ],
            BNG_WKID,
          ),
          LineNavigationType.CARTESIAN,
          2,
        ),
        makeLineWithNavigationAndId(
          makePolyline(
            [
              [
                [10, 10],
                [0, 10],
              ],
            ],
            BNG_WKID,
          ),
          LineNavigationType.CARTESIAN,
          3,
        ),
        makeLineWithNavigationAndId(
          makePolyline(
            [
              [
                [0, 10],
                [0, 0],
              ],
            ],
            BNG_WKID,
          ),
          LineNavigationType.CARTESIAN,
          4,
        ),
      ];

      const area = await densifyLoxodromesAndCalculateArea(lines, CoordinateSystem.BRITISH_NATIONAL_GRID);

      expect(area).toBe(100);
    });

    it("should calculate area for a polygon with lines with mixed navigation type in ED50", async () => {
      const area = await densifyLoxodromesAndCalculateArea(ED50_MIXED_POLYLINES, CoordinateSystem.ED50);

      expect(area).toBe(ED50_MIXED_POLYGON_AREA);
    });
  });
});
