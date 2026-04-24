import { describe, expect, test } from 'vitest';
import { makePolygonEsriJson } from '../test-utils/esrijson-test-util';
import { parentContainsChild } from '../../src/geometric-operators/polygon-contains-operator';

const parent = makePolygonEsriJson([
  [
    [0, 0],
    [10, 0],
    [10, 10],
    [0, 10],
    [0, 0],
  ],
]);
describe('polygon-contains-operator', () => {
  describe('parentContainsChild', () => {
    test('should return true when parent fully contains child', () => {
      const child = makePolygonEsriJson([
        [
          [2, 2],
          [8, 2],
          [8, 8],
          [2, 8],
          [2, 2],
        ],
      ]);

      expect(parentContainsChild(parent, child)).toBe(true);
    });

    test('should return false when parent partially contains child', () => {
      const child = makePolygonEsriJson([
        [
          [5, 5],
          [15, 5],
          [15, 15],
          [5, 15],
          [5, 5],
        ],
      ]);

      expect(parentContainsChild(parent, child)).toBe(false);
    });

    test('should return false when child is completely outside parent', () => {
      const child = makePolygonEsriJson([
        [
          [20, 20],
          [30, 20],
          [30, 30],
          [20, 30],
          [20, 20],
        ],
      ]);

      expect(parentContainsChild(parent, child)).toBe(false);
    });
  });
});
