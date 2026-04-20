import { describe, expect, test } from 'vitest';
import { makePolygonEsriJson } from '../testUtils/esrijson-test-util';
import { polygonsAreTopologicallyEqual } from '../../src/geometric-operators/polygon-equality-operator';

describe('polygon-equality-operator', () => {
  describe('polygonsAreTopologicallyEqual', () => {
    test('should return true for identical polygons', () => {
      const polygon = makePolygonEsriJson([
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
      ]);

      expect(polygonsAreTopologicallyEqual(polygon, polygon)).toBe(true);
    });

    test('should return true for same polygon with different vertex order (rotated ring)', () => {
      const polygon1 = makePolygonEsriJson([
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
      ]);
      const polygon2 = makePolygonEsriJson([
        [
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
          [10, 0],
        ],
      ]);

      expect(polygonsAreTopologicallyEqual(polygon1, polygon2)).toBe(true);
    });

    test('should return false for different polygons', () => {
      const polygon1 = makePolygonEsriJson([
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
      ]);
      const polygon2 = makePolygonEsriJson([
        [
          [20, 20],
          [30, 20],
          [30, 30],
          [20, 30],
          [20, 20],
        ],
      ]);

      expect(polygonsAreTopologicallyEqual(polygon1, polygon2)).toBe(false);
    });

    test('should return false for polygon contained within another polygon', () => {
      const polygon1 = makePolygonEsriJson([
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
      ]);
      const polygon2 = makePolygonEsriJson([
        [
          [0, 0],
          [5, 0],
          [5, 5],
          [0, 5],
          [0, 0],
        ],
      ]);

      expect(polygonsAreTopologicallyEqual(polygon1, polygon2)).toBe(false);
    });

    test('should return false for overlapping but non-equal polygons', () => {
      const polygon1 = makePolygonEsriJson([
        [
          [0, 0],
          [10, 0],
          [10, 10],
          [0, 10],
          [0, 0],
        ],
      ]);
      const polygon2 = makePolygonEsriJson([
        [
          [5, 5],
          [15, 5],
          [15, 15],
          [5, 15],
          [5, 5],
        ],
      ]);

      expect(polygonsAreTopologicallyEqual(polygon1, polygon2)).toBe(false);
    });
  });
});
