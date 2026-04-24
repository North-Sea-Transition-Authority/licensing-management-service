import { describe, expect, test } from 'vitest';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import SpatialReference from '@arcgis/core/geometry/SpatialReference';
import { unionPolygonsOperator } from '../../src/geometric-operators/union-polygons-operator';
import { CoordinateSystem } from '../../generated/uk/co/fivium/grpc/gis/CoordinateSystem.ts';
import { getCoordinateSystemWkid } from '../../src/util/coordinate-system-utils.ts';

const ED50_WKID = getCoordinateSystemWkid(CoordinateSystem.ED50);

describe('unionPolygonsOperator', () => {
  const srs = new SpatialReference({ wkid: ED50_WKID });

  test('returns the single polygon when only one is provided', () => {
    const polygon = new Polygon({
      rings: [
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
      ],
      spatialReference: srs,
    });

    const result = unionPolygonsOperator([polygon]);

    const expected = new Polygon({
      rings: [
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
      ],
      spatialReference: srs,
    });

    expect(result).toEqual(expected);
  });

  test('returns the union of two overlapping polygons', () => {
    const polygon1 = new Polygon({
      rings: [
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
      ],
      spatialReference: srs,
    });

    const polygon2 = new Polygon({
      rings: [
        [
          [5, 0],
          [15, 0],
          [15, 10],
          [5, 10],
          [5, 0],
        ],
      ],
      spatialReference: srs,
    });

    const result = unionPolygonsOperator([polygon1, polygon2]);

    const expected = new Polygon({
      rings: [
        [
          [0, 0],
          [15, 0],
          [15, 10],
          [0, 10],
          [0, 0],
        ],
      ],
      spatialReference: srs,
    });

    expect(result).toEqual(expected);
  });
});
