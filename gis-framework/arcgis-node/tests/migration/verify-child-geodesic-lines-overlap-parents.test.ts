import { describe, expect, test } from 'vitest';
import { childGeodesicLinesOverlapParents } from '../../src/migration/verify-child-geodesic-lines-overlap-parents';
import { makePolylineEsriJson } from '../test-utils/esrijson-test-util';

describe('verify-child-geodesic-lines-overlap-parents', () => {
  describe('childGeodesicLinesOverlapParents', () => {
    test('should return true when child geodesic line overlaps parent', () => {
      const line = makePolylineEsriJson([
        [
          [0, 0],
          [10, 0],
        ],
      ]);

      const result = childGeodesicLinesOverlapParents(
        [{ esriJsonPolyline: line, isGeodesic: true }],
        [{ esriJsonPolyline: line, isGeodesic: true }],
      );

      expect(result).toBe(true);
    });

    test('should return true when child is a subset of parent', () => {
      const parent = makePolylineEsriJson([
        [
          [0, 0],
          [5, 0],
          [10, 0],
        ],
      ]);
      const child = makePolylineEsriJson([
        [
          [0, 0],
          [5, 0],
        ],
      ]);

      const result = childGeodesicLinesOverlapParents(
        [{ esriJsonPolyline: parent, isGeodesic: true }],
        [{ esriJsonPolyline: child, isGeodesic: true }],
      );

      expect(result).toBe(true);
    });

    test('should return false when child geodesic line has no matching parent', () => {
      const parent = makePolylineEsriJson([
        [
          [0, 0],
          [10, 0],
        ],
      ]);
      const child = makePolylineEsriJson([
        [
          [50, 50],
          [60, 60],
        ],
      ]);

      const result = childGeodesicLinesOverlapParents(
        [{ esriJsonPolyline: parent, isGeodesic: true }],
        [{ esriJsonPolyline: child, isGeodesic: true }],
      );

      expect(result).toBe(false);
    });

    test('should ignore non-geodesic child lines', () => {
      const parent = makePolylineEsriJson([
        [
          [0, 0],
          [10, 0],
        ],
      ]);
      const child = makePolylineEsriJson([
        [
          [50, 50],
          [60, 60],
        ],
      ]);

      const result = childGeodesicLinesOverlapParents(
        [{ esriJsonPolyline: parent, isGeodesic: true }],
        [{ esriJsonPolyline: child, isGeodesic: false }],
      );

      expect(result).toBe(true);
    });

    test('should return false when no geodesic parents available for geodesic child', () => {
      const parent = makePolylineEsriJson([
        [
          [0, 0],
          [10, 0],
        ],
      ]);
      const child = makePolylineEsriJson([
        [
          [0, 0],
          [10, 0],
        ],
      ]);

      const result = childGeodesicLinesOverlapParents(
        [{ esriJsonPolyline: parent, isGeodesic: false }],
        [{ esriJsonPolyline: child, isGeodesic: true }],
      );

      expect(result).toBe(false);
    });

    test('should return true when there are no child lines', () => {
      const parent = makePolylineEsriJson([
        [
          [0, 0],
          [10, 0],
        ],
      ]);

      const result = childGeodesicLinesOverlapParents([{ esriJsonPolyline: parent, isGeodesic: true }], []);

      expect(result).toBe(true);
    });

    test('should return true when there are no child or parent geodesic lines', () => {
      const line = makePolylineEsriJson([
        [
          [0, 0],
          [10, 0],
        ],
      ]);

      const result = childGeodesicLinesOverlapParents(
        [{ esriJsonPolyline: line, isGeodesic: false }],
        [{ esriJsonPolyline: line, isGeodesic: false }],
      );

      expect(result).toBe(true);
    });
  });
});
