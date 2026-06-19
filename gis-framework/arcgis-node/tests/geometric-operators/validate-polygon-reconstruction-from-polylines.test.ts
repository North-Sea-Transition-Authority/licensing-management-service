import type Polyline from "@arcgis/core/geometry/Polyline.js";
import { describe, expect, it, vi } from "vitest";
import {
  validatePolygonReconstructionFromPolylines,
} from "../../src/geometric-operators/validate-polygon-reconstruction-from-polylines";
import { makePolygonEsriJson, makePolyline } from "../test-utils/esrijson-test-util";

vi.mock("../../src/config/logger", () => ({
  logger: {
    error: vi.fn(),
  },
}));

const spatialReferenceWkid = 4326;

function makeLine(paths: number[][][]) {
  return makePolyline(paths, spatialReferenceWkid);
}

describe("validatePolygonReconstructionFromPolylines", () => {
  it("should return true when polylines reconstruct the original polygon", () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);

    const polylines = [
      makeLine([
        [
          [0, 0],
          [10, 0],
        ],
      ]),
      makeLine([
        [
          [10, 0],
          [10, 10],
        ],
      ]),
      makeLine([
        [
          [10, 10],
          [0, 10],
        ],
      ]),
      makeLine([
        [
          [0, 10],
          [0, 0],
        ],
      ]),
    ];

    const result = validatePolygonReconstructionFromPolylines(polylines, originalPolygon);

    expect(result).toBe(true);
  });

  it("should return true when polylines reconstruct a polygon with a hole", () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
      [
        [2, 2],
        [5, 2],
        [5, 5],
        [2, 5],
        [2, 2],
      ],
    ]);

    const polylines = [
      makeLine([
        [
          [0, 0],
          [0, 10],
          [10, 10],
        ],
      ]),
      makeLine([
        [
          [10, 10],
          [10, 0],
          [0, 0],
        ],
      ]),
      makeLine([
        [
          [2, 2],
          [5, 2],
          [5, 5],
        ],
      ]),
      makeLine([
        [
          [5, 5],
          [2, 5],
          [2, 2],
        ],
      ]),
    ];

    const result = validatePolygonReconstructionFromPolylines(polylines, originalPolygon);

    expect(result).toBe(true);
  });

  it("should return false when a polyline is missing so the ring cannot be closed", () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);

    const polylines = [
      makeLine([
        [
          [0, 0],
          [0, 10],
        ],
      ]),
      makeLine([
        [
          [10, 10],
          [10, 0],
          [0, 0],
        ],
      ]),
    ];

    const result = validatePolygonReconstructionFromPolylines(polylines, originalPolygon);

    expect(result).toBe(false);
  });

  it("should return false when reconstructed polygon is not spatially equal to the original polygon", () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);

    const polylines = [
      makeLine([
        [
          [0, 0],
          [0, 8],
          [10, 8],
        ],
      ]),
      makeLine([
        [
          [10, 8],
          [10, 0],
          [0, 0],
        ],
      ]),
    ];

    const result = validatePolygonReconstructionFromPolylines(polylines, originalPolygon);

    expect(result).toBe(false);
  });

  it("should return false when no polygon can be reconstructed from the lines", () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);

    const result = validatePolygonReconstructionFromPolylines([], originalPolygon);

    expect(result).toBe(false);
  });

  it("should return false when reconstructing the polygon throws", () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);

    const result = validatePolygonReconstructionFromPolylines([undefined as unknown as Polyline], originalPolygon);

    expect(result).toBe(false);
  });
});
