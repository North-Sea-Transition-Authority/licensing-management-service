import { describe, expect, it } from 'vitest';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import * as unionOperator from '@arcgis/core/geometry/operators/unionOperator.js';
import { findParentLines } from '../../src/geometric-operators/find-parent-lines';

const spatialReference = { wkid: 27700 };

function makePolyline(paths: number[][][]): Polyline {
  return new Polyline({
    paths,
    spatialReference,
  });
}

describe('findParentLines', () => {
  it('should group child lines by containing parent and return orphaned children', () => {
    const parent1 = makePolyline([
      [
        [0, 0],
        [10, 0],
      ],
    ]);
    const parent2 = makePolyline([
      [
        [0, 10],
        [10, 10],
      ],
    ]);
    const child1 = makePolyline([
      [
        [0, 0],
        [4, 0],
      ],
    ]);
    const child2 = makePolyline([
      [
        [6, 0],
        [10, 0],
      ],
    ]);
    const child3 = makePolyline([
      [
        [2, 10],
        [8, 10],
      ],
    ]);
    const orphanedChild = makePolyline([
      [
        [0, 20],
        [5, 20],
      ],
    ]);

    const result = findParentLines(
      [
        { id: 'parent-1', polyline: parent1 },
        { id: 'parent-2', polyline: parent2 },
      ],
      [child1, child2, child3, orphanedChild],
    );

    const expectedResult = {
      lines: [
        { id: 'parent-1', polyline: child1 },
        { id: 'parent-1', polyline: child2 },
        { id: 'parent-2', polyline: child3 },
      ],
      orphanedLines: [orphanedChild],
    };

    expect(result).toEqual(expectedResult);
  });

  it('should merge contiguous child lines for the same parent', () => {
    const parent = makePolyline([
      [
        [0, 0],
        [10, 0],
      ],
    ]);
    const child1 = makePolyline([
      [
        [0, 0],
        [5, 0],
      ],
    ]);
    const child2 = makePolyline([
      [
        [5, 0],
        [10, 0],
      ],
    ]);

    const result = findParentLines([{ id: 'parent-1', polyline: parent }], [child1, child2]);
    const expectedResult = {
      lines: [{ id: 'parent-1', polyline: unionOperator.execute(child1, child2) as Polyline }],
      orphanedLines: [],
    };

    expect(result).toEqual(expectedResult);
  });

  it('should return all child lines as orphaned when no parent contains them', () => {
    const child = makePolyline([
      [
        [0, 0],
        [10, 0],
      ],
    ]);

    const result = findParentLines([], [child]);

    const expectedResult = {
      lines: [],
      orphanedLines: [child],
    };
    expect(result).toEqual(expectedResult);
  });
});
