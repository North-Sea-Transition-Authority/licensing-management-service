import { describe, expect, it } from "vitest";
import { CoordinateSystem } from "../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";
import { LineNavigationType } from "../../generated/uk/co/fivium/grpc/gis/LineNavigationType";
import { calculateArea, densifyLoxodromesUnionAndCalculateArea } from "../../src/geometric-operators/calculate-area-operator";
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

function ringLines(ring: number[][], wkid = BNG_WKID, navigationType: LineNavigationType = LineNavigationType.CARTESIAN) {
  return ring.slice(0, -1).map((point, index) =>
    makeLineWithNavigationAndId(makePolyline([[point, ring[index + 1]]], wkid), navigationType, index + 1));
}

function squareLines(
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  wkid = BNG_WKID,
  navigationType: LineNavigationType = LineNavigationType.CARTESIAN,
) {
  return ringLines([[x1, y1], [x2, y1], [x2, y2], [x1, y2], [x1, y1]], wkid, navigationType);
}

function donutLines() {
  return [...ringLines(SQUARE_RING), ...ringLines(DONUT_HOLE_RING)];
}

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

  describe("densifyLoxodromesUnionAndCalculateArea", () => {
    it.each([
      { description: "a single footprint", footprints: [squareLines(0, 0, 10, 10)], expected: 100 },
      { description: "two identical footprints stacked at different depths", footprints: [squareLines(0, 0, 10, 10), squareLines(0, 0, 10, 10)], expected: 100 },
      { description: "three identical footprints stacked at different depths", footprints: [squareLines(0, 0, 10, 10), squareLines(0, 0, 10, 10), squareLines(0, 0, 10, 10)], expected: 100 },
      { description: "two footprints that partly overlap, counting the overlap once", footprints: [squareLines(0, 0, 10, 10), squareLines(5, 0, 15, 10)], expected: 150 },
      { description: "a footprint fully inside another", footprints: [squareLines(0, 0, 10, 10), squareLines(3, 3, 7, 7)], expected: 100 },
      { description: "two disjoint footprints", footprints: [squareLines(0, 0, 10, 10), squareLines(20, 0, 30, 10)], expected: 200 },
      { description: "an overlapping pair plus a disjoint footprint", footprints: [squareLines(0, 0, 10, 10), squareLines(5, 0, 15, 10), squareLines(20, 0, 30, 10)], expected: 250 },
      { description: "a footprint with a hole", footprints: [donutLines()], expected: 84 },
      { description: "a holed footprint plus a solid one with the same outline", footprints: [donutLines(), squareLines(0, 0, 10, 10)], expected: 100 },
    ])("should return the plan area of $description in BNG", async ({ footprints, expected }) => {
      const area = await densifyLoxodromesUnionAndCalculateArea(footprints, CoordinateSystem.BRITISH_NATIONAL_GRID);

      expect(area).toBe(expected);
    });

    it("should not count a footprint stacked at a different depth twice in ED50", async () => {
      const footprint = () => squareLines(0, 0, 1, 1, ED50_WKID, LineNavigationType.LOXODROME);
      const singleArea = await densifyLoxodromesUnionAndCalculateArea([footprint()], CoordinateSystem.ED50);

      const stackedArea = await densifyLoxodromesUnionAndCalculateArea([footprint(), footprint()], CoordinateSystem.ED50);

      expect(stackedArea).toBe(singleArea);
    });

    it("should calculate the plan area of a footprint with mixed navigation type lines in ED50", async () => {
      const area = await densifyLoxodromesUnionAndCalculateArea([ED50_MIXED_POLYLINES], CoordinateSystem.ED50);

      expect(area).toBe(ED50_MIXED_POLYGON_AREA);
    });
  });
});
