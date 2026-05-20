import Polygon from "@arcgis/core/geometry/Polygon.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import { describe, expect, it } from "vitest";
import { esriJsonToPolygon, esriJsonToPolyline } from "../../src/util/esrijson-util";

describe("esrijson-util", () => {
  describe("esriJsonToPolygon", () => {
    it("should parse a valid esriJson string into a Polygon", () => {
      const esriJson = JSON.stringify({
        rings: [
          [
            [0, 0],
            [10, 0],
            [10, 10],
            [0, 10],
            [0, 0],
          ],
        ],
        spatialReference: { wkid: 4326 },
      });

      expect(Polygon.fromJSON(esriJson)).toStrictEqual(esriJsonToPolygon(esriJson));
    });

    it("should throw when given invalid esriJson", () => {
      expect(() => esriJsonToPolygon("not valid json")).toThrow();
    });
  });

  describe("esriJsonToPolyline", () => {
    it("should parse a valid esriJson string into a Polyline", () => {
      const esriJson = JSON.stringify({
        paths: [
          [
            [0, 0],
            [5, 5],
            [10, 0],
          ],
        ],
        spatialReference: { wkid: 4326 },
      });

      expect(Polyline.fromJSON(esriJson)).toStrictEqual(esriJsonToPolyline(esriJson));
    });

    it("should throw when given invalid esriJson", () => {
      expect(() => esriJsonToPolyline("not valid json")).toThrow();
    });
  });
});
