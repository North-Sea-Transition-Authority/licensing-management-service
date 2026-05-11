import { describe, expect, it, vi } from 'vitest';
import {
  type OrderedPolyline,
  validatePolygonReconstructionFromPolylines,
} from '../../src/geometric-operators/validate-polygon-reconstruction-from-polylines';
import { makePolygonEsriJson, makePolyline } from '../test-utils/esrijson-test-util';

vi.mock('../../src/config/logger', () => ({
  logger: {
    error: vi.fn(),
  },
}));

const spatialReferenceWkid = 4326;

function makeOrderedPolyline(paths: number[][][], ringNumber: number, connectionOrder: number): OrderedPolyline {
  return {
    polyline: makePolyline(paths, spatialReferenceWkid),
    ringNumber,
    connectionOrder,
  };
}

describe('validatePolygonReconstructionFromPolylines', () => {
  it('should return true when ordered polylines reconstruct the original polygon', () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);

    const orderedLines = [
      makeOrderedPolyline(
        [
          [
            [0, 0],
            [10, 0],
          ],
        ],
        0,
        1,
      ),
      makeOrderedPolyline(
        [
          [
            [10, 0],
            [10, 10],
          ],
        ],
        0,
        2,
      ),
      makeOrderedPolyline(
        [
          [
            [10, 10],
            [0, 10],
          ],
        ],
        0,
        3,
      ),
      makeOrderedPolyline(
        [
          [
            [0, 10],
            [0, 0],
          ],
        ],
        0,
        4,
      ),
    ];

    const result = validatePolygonReconstructionFromPolylines(orderedLines, originalPolygon);

    expect(result).toBe(true);
  });

  it('should return false when lines are in wrong order', () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);

    const orderedLines = [
      makeOrderedPolyline(
        [
          [
            [0, 0],
            [10, 0],
          ],
        ],
        0,
        2,
      ),
      makeOrderedPolyline(
        [
          [
            [10, 0],
            [10, 10],
          ],
        ],
        0,
        1,
      ),
      makeOrderedPolyline(
        [
          [
            [10, 10],
            [0, 10],
          ],
        ],
        0,
        3,
      ),
      makeOrderedPolyline(
        [
          [
            [0, 10],
            [0, 0],
          ],
        ],
        0,
        4,
      ),
    ];

    const result = validatePolygonReconstructionFromPolylines(orderedLines, originalPolygon);

    expect(result).toBe(false);
  });

  it('should return true when ordered polylines reconstruct a polygon with a hole', () => {
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

    const orderedLines = [
      makeOrderedPolyline(
        [
          [
            [0, 0],
            [0, 10],
            [10, 10],
          ],
        ],
        0,
        1,
      ),
      makeOrderedPolyline(
        [
          [
            [10, 10],
            [10, 0],
            [0, 0],
          ],
        ],
        0,
        2,
      ),
      makeOrderedPolyline(
        [
          [
            [2, 2],
            [5, 2],
            [5, 5],
          ],
        ],
        1,
        1,
      ),
      makeOrderedPolyline(
        [
          [
            [5, 5],
            [2, 5],
            [2, 2],
          ],
        ],
        1,
        2,
      ),
    ];

    const result = validatePolygonReconstructionFromPolylines(orderedLines, originalPolygon);

    expect(result).toBe(true);
  });

  it('should return false when a ring has a gap between ordered polylines', () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);

    const orderedLines = [
      makeOrderedPolyline(
        [
          [
            [0, 0],
            [0, 10],
          ],
        ],
        0,
        1,
      ),
      makeOrderedPolyline(
        [
          [
            [10, 10],
            [10, 0],
            [0, 0],
          ],
        ],
        0,
        2,
      ),
    ];

    const result = validatePolygonReconstructionFromPolylines(orderedLines, originalPolygon);

    expect(result).toBe(false);
  });

  it('should return false when reconstructed polygon is not spatially equal to the original polygon', () => {
    const originalPolygon = makePolygonEsriJson([
      [
        [0, 0],
        [0, 10],
        [10, 10],
        [10, 0],
        [0, 0],
      ],
    ]);

    const orderedLines = [
      makeOrderedPolyline(
        [
          [
            [0, 0],
            [0, 8],
            [10, 8],
          ],
        ],
        0,
        1,
      ),
      makeOrderedPolyline(
        [
          [
            [10, 8],
            [10, 0],
            [0, 0],
          ],
        ],
        0,
        2,
      ),
    ];

    const result = validatePolygonReconstructionFromPolylines(orderedLines, originalPolygon);

    expect(result).toBe(false);
  });
});
