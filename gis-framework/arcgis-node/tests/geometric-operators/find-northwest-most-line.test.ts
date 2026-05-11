import { describe, expect, it } from 'vitest';
import { findNorthwestMostLine } from '../../src/geometric-operators/find-northwest-most-line';
import { makePolyline } from '../test-utils/esrijson-test-util';

const spatialReferenceWkid = 27700;

describe('findNorthwestMostLine', () => {
  it('should return the line id with the start point closest to the northwest reference point', () => {
    const southWestLine = makePolyline(
      [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      spatialReferenceWkid,
    );
    const northEastLine = makePolyline(
      [
        [
          [10, 10],
          [11, 10],
        ],
      ],
      spatialReferenceWkid,
    );
    const northWestLine = makePolyline(
      [
        [
          [1, 9],
          [2, 9],
        ],
      ],
      spatialReferenceWkid,
    );

    const result = findNorthwestMostLine([
      { id: 'south-west-line', polyline: southWestLine },
      { id: 'north-east-line', polyline: northEastLine },
      { id: 'north-west-line', polyline: northWestLine },
    ]);

    expect(result).toBe('north-west-line');
  });

  it('should only use start points when finding the northwest-most line', () => {
    const lineWithNorthWestEndPoint = makePolyline(
      [
        [
          [10, 0],
          [0, 10],
        ],
      ],
      spatialReferenceWkid,
    );
    const lineWithNorthWestStartPoint = makePolyline(
      [
        [
          [1, 9],
          [2, 9],
        ],
      ],
      spatialReferenceWkid,
    );

    const result = findNorthwestMostLine([
      { id: 'line-with-north-west-end-point', polyline: lineWithNorthWestEndPoint },
      { id: 'line-with-north-west-start-point', polyline: lineWithNorthWestStartPoint },
    ]);

    expect(result).toBe('line-with-north-west-start-point');
  });

  it('should throw when no lines are provided', () => {
    expect(() => findNorthwestMostLine([])).toThrow('No lines provided');
  });

  it('should throw when a line does not have a start point', () => {
    const emptyLine = makePolyline([], spatialReferenceWkid);

    expect(() => findNorthwestMostLine([{ id: 'empty-line', polyline: emptyLine }])).toThrow(
      'Could not get start point for line empty-line',
    );
  });
});
