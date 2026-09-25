import Polyline from "@arcgis/core/geometry/Polyline.js";
import { describe, expect, it } from "vitest";
import { mergeAndGeneralizeLines } from "../../src/geometric-operators/merge-and-generalize-lines";
import { makePolylineEsriJson } from "../test-utils/esrijson-test-util";

describe("mergeAndGeneralizeLines", () => {
  it("should join two collinear segments into a single straightened path", () => {
    const a = makePolylineEsriJson([[[0, 0], [1, 0]]]);
    const b = makePolylineEsriJson([[[1, 0], [2, 0]]]);

    const result = Polyline.fromJSON(JSON.parse(mergeAndGeneralizeLines([a, b])));

    // shared midpoint [1,0] is collinear -> generalised away, endpoints preserved
    expect(result.paths).toEqual([
      [
        [0, 0],
        [2, 0],
      ],
    ]);
  });

  it("should keep a corner vertex where two non-collinear segments meet", () => {
    const a = makePolylineEsriJson([[[0, 0], [1, 0]]]);
    const b = makePolylineEsriJson([[[1, 0], [1, 1]]]);

    const result = Polyline.fromJSON(JSON.parse(mergeAndGeneralizeLines([a, b])));

    // real corner at [1,0], not collinear -> retained
    expect(result.paths).toEqual([
      [
        [0, 0],
        [1, 0],
        [1, 1],
      ],
    ]);
  });

  it("should merge more than two segments", () => {
    const segments = [
      makePolylineEsriJson([[[0, 0], [1, 0]]]),
      makePolylineEsriJson([[[1, 0], [2, 0]]]),
      makePolylineEsriJson([[[2, 0], [3, 0]]]),
    ];

    const result = Polyline.fromJSON(JSON.parse(mergeAndGeneralizeLines(segments)));

    // all collinear -> one straight path spanning the endpoints
    expect(result.paths).toEqual([
      [
        [0, 0],
        [3, 0],
      ],
    ]);
  });

  it("should keep disjoint segments as separate paths", () => {
    const a = makePolylineEsriJson([[[0, 0], [1, 0]]]);
    const b = makePolylineEsriJson([[[5, 5], [6, 5]]]);

    const result = Polyline.fromJSON(JSON.parse(mergeAndGeneralizeLines([a, b])));

    // no shared vertex -> union cannot join them
    expect(result.paths).toHaveLength(2);
    expect(result.paths).toEqual(
      expect.arrayContaining([
        [
          [0, 0],
          [1, 0],
        ],
        [
          [5, 5],
          [6, 5],
        ],
      ]),
    );
  });
});
