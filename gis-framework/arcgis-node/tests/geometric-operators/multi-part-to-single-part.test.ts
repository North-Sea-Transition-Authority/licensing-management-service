import Polygon from "@arcgis/core/geometry/Polygon.js";
import { describe, expect, it } from "vitest";
import { multiPartToSinglePart } from "../../src/geometric-operators/multi-part-to-single-part";
import { makePolygonEsriJson } from "../test-utils/esrijson-test-util";

describe("multiPartToSinglePart", () => {
  it("should split a disjoint multipart polygon into one single-part polygon per part", () => {
    const disjointPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 2],
        [2, 2],
        [2, 0],
        [0, 0],
      ],
      [
        [10, 10],
        [10, 12],
        [12, 12],
        [12, 10],
        [10, 10],
      ],
    ]);

    const result = multiPartToSinglePart(disjointPolygon).map(json => Polygon.fromJSON(JSON.parse(json)).rings);

    expect(result).toHaveLength(2);
    expect(result).toEqual(
      expect.arrayContaining([
        [
          [
            [0, 0],
            [0, 2],
            [2, 2],
            [2, 0],
            [0, 0],
          ],
        ],
        [
          [
            [10, 10],
            [10, 12],
            [12, 12],
            [12, 10],
            [10, 10],
          ],
        ],
      ]),
    );
  });

  it("should return a single single-part polygon for a single-part polygon", () => {
    const square = makePolygonEsriJson([
      [
        [0, 0],
        [0, 2],
        [2, 2],
        [2, 0],
        [0, 0],
      ],
    ]);

    const result = multiPartToSinglePart(square).map(json => Polygon.fromJSON(JSON.parse(json)).rings);

    expect(result).toEqual([
      [
        [
          [0, 0],
          [0, 2],
          [2, 2],
          [2, 0],
          [0, 0],
        ],
      ],
    ]);
  });

  it("should keep a hole with its outer ring in the same single-part polygon", () => {
    const squareWithHole = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
      [
        [2, 2],
        [4, 2],
        [4, 4],
        [2, 4],
        [2, 2],
      ],
    ]);

    const result = multiPartToSinglePart(squareWithHole).map(json => Polygon.fromJSON(JSON.parse(json)).rings);

    expect(result).toEqual([
      [
        [
          [0, 0],
          [0, 10],
          [10, 10],
          [10, 0],
          [0, 0],
        ],
        [
          [2, 2],
          [4, 2],
          [4, 4],
          [2, 4],
          [2, 2],
        ],
      ],
    ]);
  });
});
