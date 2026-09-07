import Polyline from "@arcgis/core/geometry/Polyline.js";
import { describe, expect, it } from "vitest";
import { mergeAndGeneralizeLines } from "../../src/geometric-operators/merge-and-generalize-lines";
import { makePolylineEsriJson } from "../test-utils/esrijson-test-util";

function pathContains(path: number[][], point: number[]): boolean {
  return path.some(([x, y]) => x === point[0] && y === point[1]);
}

describe("mergeAndGeneralizeLines", () => {
  it("should join two collinear segments into a single straightened path", () => {
    const a = makePolylineEsriJson([[[0, 0], [1, 0]]]);
    const b = makePolylineEsriJson([[[1, 0], [2, 0]]]);

    const result = Polyline.fromJSON(JSON.parse(mergeAndGeneralizeLines([a, b])));

    expect(result.paths).toHaveLength(1);
    // shared midpoint [1,0] is collinear -> generalised away
    expect(pathContains(result.paths[0], [1, 0])).toBe(false);
    // endpoints preserved
    expect(pathContains(result.paths[0], [0, 0])).toBe(true);
    expect(pathContains(result.paths[0], [2, 0])).toBe(true);
  });

  it("should keep a corner vertex where two non-collinear segments meet", () => {
    const a = makePolylineEsriJson([[[0, 0], [1, 0]]]);
    const b = makePolylineEsriJson([[[1, 0], [1, 1]]]);

    const result = Polyline.fromJSON(JSON.parse(mergeAndGeneralizeLines([a, b])));

    expect(result.paths).toHaveLength(1);
    // real corner, not collinear -> retained
    expect(pathContains(result.paths[0], [1, 0])).toBe(true);
  });

  it("should merge more than two segments", () => {
    const segments = [
      makePolylineEsriJson([[[0, 0], [1, 0]]]),
      makePolylineEsriJson([[[1, 0], [2, 0]]]),
      makePolylineEsriJson([[[2, 0], [3, 0]]]),
    ];

    const result = Polyline.fromJSON(JSON.parse(mergeAndGeneralizeLines(segments)));

    expect(result.paths).toHaveLength(1);
    expect(pathContains(result.paths[0], [0, 0])).toBe(true);
    expect(pathContains(result.paths[0], [3, 0])).toBe(true);
  });

  it("should keep disjoint segments as separate paths", () => {
    const a = makePolylineEsriJson([[[0, 0], [1, 0]]]);
    const b = makePolylineEsriJson([[[5, 5], [6, 5]]]);

    const result = Polyline.fromJSON(JSON.parse(mergeAndGeneralizeLines([a, b])));

    // no shared vertex -> union cannot join them
    expect(result.paths).toHaveLength(2);
  });
});
