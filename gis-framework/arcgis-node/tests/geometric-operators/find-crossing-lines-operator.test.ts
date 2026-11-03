import { describe, expect, it } from "vitest";
import { findCrossingLines } from "../../src/geometric-operators/find-crossing-lines-operator";

const BNG_WKID = 27700;
const ED50_WKID = 4230;
const ETRS89_WKID = 4258;

function rectangleEsriJson(
  minX: number,
  minY: number,
  maxX: number,
  maxY: number,
  wkid: number = BNG_WKID,
): string {
  const rings = [[
    [minX, minY],
    [minX, maxY],
    [maxX, maxY],
    [maxX, minY],
    [minX, minY],
  ]];

  return JSON.stringify({ rings, spatialReference: { wkid } });
}

function line(
  id: string,
  path: number[][],
  wkid: number = BNG_WKID,
) {
  return { id, polyLineEsriJson: JSON.stringify({ paths: [path], spatialReference: { wkid } }) };
}

describe("findCrossingLines", () => {
  const polygon = rectangleEsriJson(0, 0, 10, 10);

  it.each([
    ["LinePartlyInsidePolygon", [[5, 5], [15, 5]]],
    ["LinePassesThroughPolygon", [[-5, 5], [15, 5]]],
    ["LineEntersAndLeavesPolygon", [[-5, 2], [5, 2], [5, -5]]],
  ])("findCrossingLines_when%s_thenLineReturned", (_scenario, path) => {
    const result = findCrossingLines([polygon], [line("line", path)]);

    expect(result).toEqual(["line"]);
  });

  it.each([
    ["LineInsidePolygon", [[2, 2], [8, 8]]],
    ["LineOutsidePolygon", [[20, 20], [30, 30]]],
    ["LineAlongEdgeOfPolygon", [[10, 2], [10, 8]]],
    ["LineTouchesPolygonAtPoint", [[10, 5], [15, 5]]],
    ["LineRunsFromEdgeToEdgeOfPolygon", [[0, 5], [10, 5]]],
  ])("findCrossingLines_when%s_thenLineNotReturned", (_scenario, path) => {
    const result = findCrossingLines([polygon], [line("line", path)]);

    expect(result).toEqual([]);
  });

  it.each([
    ["BritishNationalGrid", BNG_WKID],
    ["Ed50", ED50_WKID],
    ["Etrs89", ETRS89_WKID],
  ])("findCrossingLines_whenLineCrossesPolygonIn%s_thenLineReturned", (_coordinateSystem, wkid) => {
    const result = findCrossingLines([rectangleEsriJson(1, 1, 2, 2, wkid)], [line("line", [[1.5, 1.5], [3, 1.5]], wkid)]);

    expect(result).toEqual(["line"]);
  });

  it("findCrossingLines_whenLineCrossesOnlySecondPolygon_thenLineReturned", () => {
    const polygons = [polygon, rectangleEsriJson(20, 0, 30, 10)];

    const result = findCrossingLines(polygons, [line("line", [[25, 5], [35, 5]])]);

    expect(result).toEqual(["line"]);
  });

  it("findCrossingLines_whenLineCrossesIntoHoleOfPolygon_thenLineReturned", () => {
    const polygonWithHole = JSON.stringify({
      rings: [
        [[0, 0], [0, 10], [10, 10], [10, 0], [0, 0]],
        [[3, 3], [7, 3], [7, 7], [3, 7], [3, 3]],
      ],
      spatialReference: { wkid: BNG_WKID },
    });

    const result = findCrossingLines([polygonWithHole], [line("line", [[5, 5], [5, 9]])]);

    expect(result).toEqual(["line"]);
  });

  it("findCrossingLines_whenSeveralLines_thenOnlyCrossingLinesReturnedInOrder", () => {
    const lines = [
      line("crossing", [[5, 5], [15, 5]]),
      line("inside", [[2, 2], [8, 8]]),
      line("passingThrough", [[-5, 5], [15, 5]]),
    ];

    const result = findCrossingLines([polygon], lines);

    expect(result).toEqual(["crossing", "passingThrough"]);
  });
});
