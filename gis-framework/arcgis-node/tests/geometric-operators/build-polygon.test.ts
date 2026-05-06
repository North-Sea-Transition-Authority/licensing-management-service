import { describe, expect, it } from 'vitest';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import { buildPolygon } from '../../src/geometric-operators/build-polygon';
import * as equalsOperator from '@arcgis/core/geometry/operators/equalsOperator';

const spatialReference = { wkid: 27700 };

describe('buildPolygon', () => {
  it('should build a polygon from a single closed polyline', () => {
    const polyline = new Polyline({
      paths: [
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

    const expectedPolygon = new Polygon({
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

    const result = buildPolygon([polyline], spatialReference.wkid);
    expect(equalsOperator.execute(result, expectedPolygon)).toBe(true);
  });

  it('should build a polygon from multiple polylines forming a closed shape', () => {
    const polyline1 = new Polyline({
      paths: [
        [
          [0, 0],
          [0, 10],
          [10, 10],
        ],
      ],
      spatialReference,
    });

    const polyline2 = new Polyline({
      paths: [
        [
          [10, 10],
          [10, 0],
          [0, 0],
        ],
      ],
      spatialReference,
    });

    const expectedPolygon = new Polygon({
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

    const result = buildPolygon([polyline1, polyline2], spatialReference.wkid);
    expect(equalsOperator.execute(result, expectedPolygon)).toBe(true);
  });

  it('should build a polygon with a hole', () => {
    const polyline1 = new Polyline({
      paths: [
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

    const polyline2 = new Polyline({
      paths: [
        [
          [2, 2],
          [2, 7],
          [7, 7],
          [7, 2],
          [2, 2],
        ],
      ],
      spatialReference,
    });

    const expectedPolygon = new Polygon({
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
          [7, 2],
          [7, 7],
          [2, 7],
          [2, 2],
        ],
      ],
      spatialReference,
    });

    const result = buildPolygon([polyline1, polyline2], spatialReference.wkid);
    expect(equalsOperator.execute(result, expectedPolygon)).toBe(true);
  });

  it('should return undefined when no polygon can be reconstructed from the lines', () => {
    const polyline1 = new Polyline({
      paths: [
        [
          [0, 0],
          [0, 10],
        ],
      ],
      spatialReference,
    });

    const polyline2 = new Polyline({
      paths: [
        [
          [5, 0],
          [5, 10],
        ],
      ],
      spatialReference,
    });

    expect(buildPolygon([polyline1, polyline2], spatialReference.wkid)).toBeUndefined();
  });
});
