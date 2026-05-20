import { describe, expect, it } from "vitest";
import { LineNavigationType } from "../../generated/uk/co/fivium/grpc/gis/LineNavigationType";
import {
  findLoxodromeThatConnectsToPointOnSetBearing,
  SetBearing,
} from "../../src/migration/types/line-with-bearing-wrapper";
import { esriJsonToPolyline } from "../../src/util/esrijson-util";
import { makePolylineEsriJson } from "../test-utils/esrijson-test-util";

function makeLine(paths, navigationType, id) {
  return {
    line: esriJsonToPolyline(makePolylineEsriJson(paths)),
    navigationType,
    id,
  };
}

describe("line-with-bearing-wrapper", () => {
  describe("findLoxodromeThatConnectsToPointOnSetBearing", () => {
    it("should return undefined when no lines are provided", () => {
      const polyline = esriJsonToPolyline(
        makePolylineEsriJson([
          [
            [0, 0],
            [5, 0],
          ],
        ]),
      );
      const point = polyline.getPoint(0, 0);

      const result = findLoxodromeThatConnectsToPointOnSetBearing(point, []);

      expect(result).toBeUndefined();
    });

    it("should return undefined when line is geodesic, not loxodrome", () => {
      const line = makeLine(
        [
          [
            [0, 0],
            [0, 5],
          ],
        ],
        LineNavigationType.GEODESIC,
        1,
      );
      const point = line.line.getPoint(0, 0);

      const result = findLoxodromeThatConnectsToPointOnSetBearing(point, [line]);

      expect(result).toBeUndefined();
    });

    it("should return undefined when point does not connect to any line", () => {
      const line = makeLine(
        [
          [
            [0, 0],
            [0, 5],
          ],
        ],
        LineNavigationType.LOXODROME,
        1,
      );
      const disconnectedPolyline = esriJsonToPolyline(
        makePolylineEsriJson([
          [
            [50, 50],
            [60, 60],
          ],
        ]),
      );
      const point = disconnectedPolyline.getPoint(0, 0);

      const result = findLoxodromeThatConnectsToPointOnSetBearing(point, [line]);

      expect(result).toBeUndefined();
    });

    it("should return LATITUDE set bearing when loxodrome has same x for first two points and point connects to start", () => {
      const line = makeLine(
        [
          [
            [5, 0],
            [5, 10],
          ],
        ],
        LineNavigationType.LOXODROME,
        1,
      );
      const point = line.line.getPoint(0, 0);

      const result = findLoxodromeThatConnectsToPointOnSetBearing(point, [line]);

      const expected = { line: line.line, setBearing: SetBearing.LATITUDE, id: 1 };
      expect(result).toStrictEqual(expected);
    });

    it("should return LONGITUDE set bearing when loxodrome has same y for first two points and point connects to start", () => {
      const line = makeLine(
        [
          [
            [0, 5],
            [10, 5],
          ],
        ],
        LineNavigationType.LOXODROME,
        2,
      );
      const point = line.line.getPoint(0, 0);

      const result = findLoxodromeThatConnectsToPointOnSetBearing(point, [line]);

      const expected = { line: line.line, setBearing: SetBearing.LONGITUDE, id: 2 };
      expect(result).toStrictEqual(expected);
    });

    it("should return LATITUDE set bearing when point connects to end of loxodrome", () => {
      const line = makeLine(
        [
          [
            [5, 0],
            [5, 5],
            [5, 10],
          ],
        ],
        LineNavigationType.LOXODROME,
        3,
      );
      const point = line.line.getPoint(0, 2);

      const result = findLoxodromeThatConnectsToPointOnSetBearing(point, [line]);

      const expected = { line: line.line, setBearing: SetBearing.LATITUDE, id: 3 };
      expect(result).toStrictEqual(expected);
    });

    it("should return LONGITUDE set bearing when point connects to end of loxodrome", () => {
      const line = makeLine(
        [
          [
            [0, 5],
            [5, 5],
            [10, 5],
          ],
        ],
        LineNavigationType.LOXODROME,
        4,
      );
      const point = line.line.getPoint(0, 2);

      const result = findLoxodromeThatConnectsToPointOnSetBearing(point, [line]);

      const expected = { line: line.line, setBearing: SetBearing.LONGITUDE, id: 4 };
      expect(result).toStrictEqual(expected);
    });

    it("should return undefined when loxodrome has neither same x nor same y for adjacent points", () => {
      const line = makeLine(
        [
          [
            [0, 0],
            [5, 5],
          ],
        ],
        LineNavigationType.LOXODROME,
        5,
      );
      const point = line.line.getPoint(0, 0);

      const result = findLoxodromeThatConnectsToPointOnSetBearing(point, [line]);

      expect(result).toBeUndefined();
    });
  });
});
