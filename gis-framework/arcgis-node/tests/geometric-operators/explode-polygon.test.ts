import { describe, expect, it } from 'vitest';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import { explodePolygon } from '../../src/geometric-operators/explode-polygon';

const spatialReference = { wkid: 27700 };

function makePolyline(start: number[], end: number[]): Polyline {
  return new Polyline({
    paths: [[start, end]],
    spatialReference: spatialReference,
  });
}

describe('explodePolygon', () => {
  it('should explode a single-ring polygon into one polyline per edge', () => {
    const polygon = new Polygon({
      rings: [
        [
          [0, 0],
          [0, 10],
          [10, 10],
          [10, 0],
          [0, 0],
        ],
      ],
      spatialReference,
    });

    const result = explodePolygon(polygon);

    const expectedLines = [
      makePolyline([0, 0], [0, 10]),
      makePolyline([0, 10], [10, 10]),
      makePolyline([10, 10], [10, 0]),
      makePolyline([10, 0], [0, 0]),
    ];
    expect(result).toEqual(expectedLines);
  });

  it('should explode every ring when a polygon has a hole', () => {
    const polygon = new Polygon({
      rings: [
        [
          [0, 0],
          [0, 10],
          [10, 10],
          [10, 0],
          [0, 0],
        ],
        [
          [2, 2],
          [2, 5],
          [5, 5],
          [5, 2],
          [2, 2],
        ],
      ],
      spatialReference,
    });

    const result = explodePolygon(polygon);

    const expectedLines = [
      makePolyline([0, 0], [0, 10]),
      makePolyline([0, 10], [10, 10]),
      makePolyline([10, 10], [10, 0]),
      makePolyline([10, 0], [0, 0]),
      makePolyline([2, 2], [2, 5]),
      makePolyline([2, 5], [5, 5]),
      makePolyline([5, 5], [5, 2]),
      makePolyline([5, 2], [2, 2]),
    ];
    expect(result).toEqual(expectedLines);
  });

  it('should return empty when the polygon has no rings', () => {
    const polygon = new Polygon({
      rings: [],
      spatialReference,
    });

    expect(explodePolygon(polygon)).toEqual([]);
  });
});
