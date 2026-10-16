import { describe, expect, it } from "vitest";
import { groupEqualPolygons } from "../../src/geometric-operators/group-equal-polygons";
import { makePolygonEsriJson } from "../test-utils/esrijson-test-util";

const square = makePolygonEsriJson([
  [
    [0, 0],
    [10, 0],
    [10, 10],
    [0, 10],
    [0, 0],
  ],
]);

const sameSquareStartingFromDifferentVertex = makePolygonEsriJson([
  [
    [10, 0],
    [10, 10],
    [0, 10],
    [0, 0],
    [10, 0],
  ],
]);

const differentSquare = makePolygonEsriJson([
  [
    [20, 20],
    [30, 20],
    [30, 30],
    [20, 30],
    [20, 20],
  ],
]);

describe("groupEqualPolygons", () => {
  it.each([
    ["identical squares", square, square],
    ["the same square starting from a different vertex", square, sameSquareStartingFromDifferentVertex],
  ])("groupEqualPolygons_whenPolygonsAreEqual_thenGroupedTogether (%s)", (_description, firstPolygon, secondPolygon) => {
    const polygons = [
      { id: "a", esriJsonPolygon: firstPolygon },
      { id: "b", esriJsonPolygon: secondPolygon },
    ];

    const groups = groupEqualPolygons(polygons);

    expect(groups).toEqual([["a", "b"]]);
  });

  it("groupEqualPolygons_whenPolygonsAreDifferent_thenSeparateGroups", () => {
    const polygons = [
      { id: "a", esriJsonPolygon: square },
      { id: "b", esriJsonPolygon: differentSquare },
    ];

    const groups = groupEqualPolygons(polygons);

    expect(groups).toEqual([["a"], ["b"]]);
  });

  it("groupEqualPolygons_whenMixOfEqualAndDifferentPolygons_thenGroupsInInputOrder", () => {
    const polygons = [
      { id: "a", esriJsonPolygon: square },
      { id: "c", esriJsonPolygon: differentSquare },
      { id: "b", esriJsonPolygon: sameSquareStartingFromDifferentVertex },
    ];

    const groups = groupEqualPolygons(polygons);

    expect(groups).toEqual([["a", "b"], ["c"]]);
  });

  it("groupEqualPolygons_whenSinglePolygon_thenOneGroup", () => {
    const polygons = [{ id: "a", esriJsonPolygon: square }];

    const groups = groupEqualPolygons(polygons);

    expect(groups).toEqual([["a"]]);
  });

  it("groupEqualPolygons_whenNoPolygons_thenNoGroups", () => {
    const groups = groupEqualPolygons([]);

    expect(groups).toEqual([]);
  });
});
