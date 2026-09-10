import { describe, expect, it } from "vitest";
import { parentContainsChild } from "../../src/geometric-operators/polygon-contains-operator";
import { unionPolygonsOperator } from "../../src/geometric-operators/union-polygons-operator";
import { makePolygon, makePolygonEsriJson } from "../test-utils/esrijson-test-util";

const parent = makePolygonEsriJson([
  [
    [0, 0],
    [10, 0],
    [10, 10],
    [0, 10],
    [0, 0],
  ],
]);
describe("polygon-contains-operator", () => {
  describe("parentContainsChild", () => {
    it("should return true when parent fully contains child", () => {
      const child = makePolygonEsriJson([
        [
          [2, 2],
          [8, 2],
          [8, 8],
          [2, 8],
          [2, 2],
        ],
      ]);

      expect(parentContainsChild(parent, child)).toBe(true);
    });

    it("should return false when parent partially contains child", () => {
      const child = makePolygonEsriJson([
        [
          [5, 5],
          [15, 5],
          [15, 15],
          [5, 15],
          [5, 5],
        ],
      ]);

      expect(parentContainsChild(parent, child)).toBe(false);
    });

    it("should return false when child is completely outside parent", () => {
      const child = makePolygonEsriJson([
        [
          [20, 20],
          [30, 20],
          [30, 30],
          [20, 30],
          [20, 20],
        ],
      ]);

      expect(parentContainsChild(parent, child)).toBe(false);
    });

    it("should return true when a multipart parent contains the child in one of its parts", () => {
      const multipartParent = makePolygonEsriJson([
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
        [
          [20, 20],
          [30, 20],
          [30, 30],
          [20, 30],
          [20, 20],
        ],
      ]);
      const child = makePolygonEsriJson([
        [
          [22, 22],
          [28, 22],
          [28, 28],
          [22, 28],
          [22, 22],
        ],
      ]);

      expect(parentContainsChild(multipartParent, child)).toBe(true);
    });

    it("should return false when a child straddles the gap between a multipart parent's parts", () => {
      const multipartParent = makePolygonEsriJson([
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
        [
          [20, 20],
          [30, 20],
          [30, 30],
          [20, 30],
          [20, 20],
        ],
      ]);
      const child = makePolygonEsriJson([
        [
          [5, 5],
          [25, 5],
          [25, 25],
          [5, 25],
          [5, 5],
        ],
      ]);

      expect(parentContainsChild(multipartParent, child)).toBe(false);
    });

    it("should return true when the union of a touching 2x2 quilt contains a child straddling the shared corner", () => {
      const quilt = [
        makePolygon(
          [
            [
              [0, 0],
              [5, 0],
              [5, 5],
              [0, 5],
              [0, 0],
            ],
          ],
          4326,
        ),
        makePolygon(
          [
            [
              [5, 0],
              [10, 0],
              [10, 5],
              [5, 5],
              [5, 0],
            ],
          ],
          4326,
        ),
        makePolygon(
          [
            [
              [0, 5],
              [5, 5],
              [5, 10],
              [0, 10],
              [0, 5],
            ],
          ],
          4326,
        ),
        makePolygon(
          [
            [
              [5, 5],
              [10, 5],
              [10, 10],
              [5, 10],
              [5, 5],
            ],
          ],
          4326,
        ),
      ];

      const unionedPolygon = unionPolygonsOperator(quilt);
      const unionedEsriJson = JSON.stringify(unionedPolygon.toJSON());

      const child = makePolygonEsriJson([
        [
          [3, 3],
          [7, 3],
          [7, 7],
          [3, 7],
          [3, 3],
        ],
      ]);

      expect(parentContainsChild(unionedEsriJson, child)).toBe(true);
    });
  });
});
