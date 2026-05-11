import { describe, expect, it } from 'vitest';
import { getLineStartAndEndPoints } from '../../src/geometric-operators/get-line-start-and-end-points';
import { makePolyline } from '../test-utils/esrijson-test-util';

const spatialReferenceWkid = 27700;

describe('getLineStartAndEndPoints', () => {
  it('should return the first and last points for each line', () => {
    const firstLine = makePolyline(
      [
        [
          [0, 0],
          [5, 5],
          [10, 10],
        ],
      ],
      spatialReferenceWkid,
    );
    const secondLine = makePolyline(
      [
        [
          [20, 20],
          [25, 25],
        ],
      ],
      spatialReferenceWkid,
    );

    const result = getLineStartAndEndPoints([
      { id: 'line-1', polyline: firstLine },
      { id: 'line-2', polyline: secondLine },
    ]);

    expect(result).toEqual([
      {
        lineId: 'line-1',
        startPoint: { x: 0, y: 0 },
        endPoint: { x: 10, y: 10 },
      },
      {
        lineId: 'line-2',
        startPoint: { x: 20, y: 20 },
        endPoint: { x: 25, y: 25 },
      },
    ]);
  });

  it('should return empty when no lines are provided', () => {
    expect(getLineStartAndEndPoints([])).toEqual([]);
  });

  it('should throw when a line does not have path', () => {
    const emptyLine = makePolyline([[]], spatialReferenceWkid);

    expect(() => getLineStartAndEndPoints([{ id: 'empty-line', polyline: emptyLine }])).toThrow('Line empty-line has no path');
  });
});
