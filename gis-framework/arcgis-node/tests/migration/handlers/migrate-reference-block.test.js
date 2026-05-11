import { describe, expect, test, vi } from 'vitest';
import { status } from '@grpc/grpc-js';
import {
  findIntersectionPoint,
  mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers,
  migrateReferenceBlockHandler,
  replaceSegment,
  updateGeodesicReferenceBlockLine,
} from '../../../src/migration/handlers/migrate-reference-block';
import { makeLineWithNavigationAndId, makePoint, makePolyline } from '../../test-utils/esrijson-test-util.ts';
import { getCoordinateSystemWkid } from '../../../src/util/coordinate-system-utils.ts';
import { CoordinateSystem } from '../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem.ts';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType.ts';

const ED50_WKID = getCoordinateSystemWkid(CoordinateSystem.ED50);

function makeLoxodromeLineWithSetBearing(point) {
  return makeLineWithNavigationAndId(
    makePolyline(
      [
        [
          [point.x, point.y],
          [point.x, point.y + 1],
        ],
      ],
      ED50_WKID,
    ),
    LineNavigationType.LOXODROME,
    1,
  );
}

describe('migrate-reference-block', () => {
  test('returns internal error when migration throws', async () => {
    const call = {
      request: {
        geoJsonLineWrappers: [],
        licenseBlockLines: [],
        coordinateSystem: CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED,
      },
    };

    const callback = vi.fn();

    await migrateReferenceBlockHandler(call, callback);

    const callbackError = callback.mock.calls[0][0];
    expect(callbackError.message).toBe(`Could not determine wkid for ${CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED}`);
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(callback).toHaveBeenCalledWith(callbackError, null);
    expect(callback).toHaveBeenCalledOnce();
  });

  describe('mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers', () => {
    test('returns all lines when there is one geodesic line', async () => {
      const geodesicLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        1,
      );
      const loxodromeLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [1, 0],
              [1, 1],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.LOXODROME,
        2,
      );

      const result = await mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers(
        new Map([
          [geodesicLine.id, geodesicLine],
          [loxodromeLine.id, loxodromeLine],
        ]),
        new Map([
          [geodesicLine.id, 1],
          [loxodromeLine.id, 2],
        ]),
      );

      expect(result).toEqual([geodesicLine, loxodromeLine]);
    });

    test('merges adjacent geodesic lines', async () => {
      const firstGeodesicLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        1,
      );
      const secondGeodesicLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [1, 0],
              [2, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        2,
      );

      const result = await mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers(
        new Map([
          [firstGeodesicLine.id, firstGeodesicLine],
          [secondGeodesicLine.id, secondGeodesicLine],
        ]),
        new Map([
          [firstGeodesicLine.id, 1],
          [secondGeodesicLine.id, 2],
        ]),
      );

      const expected = [
        makeLineWithNavigationAndId(
          makePolyline(
            [
              [
                [0, 0],
                [1, 0],
                [2, 0],
              ],
            ],
            ED50_WKID,
          ),
          LineNavigationType.GEODESIC,
          firstGeodesicLine.id,
        ),
      ];
      expect(result).toEqual(expected);
    });

    test('merges the first and last geodesic lines in the connection order', async () => {
      const firstGeodesicLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        1,
      );
      const middleLoxodromeLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [1, 0],
              [1, 1],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.LOXODROME,
        2,
      );
      const lastGeodesicLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 1],
              [0, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        3,
      );

      const result = await mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers(
        new Map([
          [firstGeodesicLine.id, firstGeodesicLine],
          [middleLoxodromeLine.id, middleLoxodromeLine],
          [lastGeodesicLine.id, lastGeodesicLine],
        ]),
        new Map([
          [firstGeodesicLine.id, 1],
          [middleLoxodromeLine.id, 2],
          [lastGeodesicLine.id, 3],
        ]),
      );

      expect(result.map((lineWrapper) => lineWrapper.id)).toEqual([firstGeodesicLine.id, middleLoxodromeLine.id]);
    });

    test('keeps non-adjacent geodesic lines separate', async () => {
      const firstGeodesicLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        2,
      );
      const secondGeodesicLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [2, 0],
              [3, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        4,
      );
      const loxodromeLine = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [1, 0],
              [2, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.LOXODROME,
        5,
      );

      const result = await mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers(
        new Map([
          [firstGeodesicLine.id, firstGeodesicLine],
          [secondGeodesicLine.id, secondGeodesicLine],
          [loxodromeLine.id, loxodromeLine],
        ]),
        new Map([
          [loxodromeLine.id, 1],
          [firstGeodesicLine.id, 2],
          [secondGeodesicLine.id, 4],
        ]),
      );

      expect(result.map((lineWrapper) => lineWrapper.id)).toEqual([
        firstGeodesicLine.id,
        secondGeodesicLine.id,
        loxodromeLine.id,
      ]);
    });
  });
  describe('findIntersectionPoint', () => {
    test('returns the bearing intersection from the ref block point when it intersects the license line', () => {
      const refBlockPoint = makePoint(1, 50, ED50_WKID);
      const licensePoint = makePoint(5, 50, ED50_WKID);
      const licenseLine = makePolyline(
        [
          [
            [0, 50],
            [2, 50],
          ],
        ],
        ED50_WKID,
      );
      const refBlockLine = makePolyline(
        [
          [
            [4, 50],
            [6, 50],
          ],
        ],
        ED50_WKID,
      );

      const result = findIntersectionPoint(refBlockPoint, licensePoint, licenseLine, refBlockLine, [
        makeLoxodromeLineWithSetBearing(refBlockPoint),
      ]);

      expect(result).toEqual(makePoint(1, 50, ED50_WKID));
    });

    test('returns the bearing intersection from the license point when the ref block point does not intersect the license line', () => {
      const refBlockPoint = makePoint(1, 50, ED50_WKID);
      const licensePoint = makePoint(5, 50, ED50_WKID);
      const licenseLine = makePolyline(
        [
          [
            [20, 20],
            [30, 20],
          ],
        ],
        ED50_WKID,
      );
      const refBlockLine = makePolyline(
        [
          [
            [4, 50],
            [6, 50],
          ],
        ],
        ED50_WKID,
      );

      const result = findIntersectionPoint(refBlockPoint, licensePoint, licenseLine, refBlockLine, [
        makeLoxodromeLineWithSetBearing(refBlockPoint),
      ]);

      expect(result).toEqual(makePoint(5, 50, ED50_WKID));
    });

    test('returns the nearest point when there is no connected line on a set bearing', () => {
      const refBlockPoint = makePoint(1, 50, ED50_WKID);
      const licensePoint = makePoint(100, 100, ED50_WKID);
      const licenseLine = makePolyline(
        [
          [
            [0, 50],
            [2, 50],
          ],
        ],
        ED50_WKID,
      );
      const refBlockLine = makePolyline(
        [
          [
            [99, 99],
            [101, 99],
          ],
        ],
        ED50_WKID,
      );

      const result = findIntersectionPoint(refBlockPoint, licensePoint, licenseLine, refBlockLine, []);

      expect(result).toEqual(makePoint(1, 50, ED50_WKID));
    });

    test('returns undefined when there is no bearing intersection and the nearest point is too far away', () => {
      const refBlockPoint = makePoint(1, 50, ED50_WKID);
      const licensePoint = makePoint(100, 100, ED50_WKID);
      const licenseLine = makePolyline(
        [
          [
            [20, 20],
            [30, 20],
          ],
        ],
        ED50_WKID,
      );
      const refBlockLine = makePolyline(
        [
          [
            [120, 120],
            [130, 120],
          ],
        ],
        ED50_WKID,
      );

      const result = findIntersectionPoint(refBlockPoint, licensePoint, licenseLine, refBlockLine, []);

      expect(result).toBeUndefined();
    });
  });
  describe('updateGeodesicReferenceBlockLine', () => {
    test('replaces the reference block line segment when both intersections are found', () => {
      const refBlockLineWrapper = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 0],
              [2, 0],
              [3, 0],
              [4, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        1,
      );
      const licenseLine = makePolyline(
        [
          [
            [0, 0],
            [1, 1],
            [2, 1],
            [3, 1],
            [4, 0],
          ],
        ],
        ED50_WKID,
      );

      updateGeodesicReferenceBlockLine(
        [licenseLine],
        makePoint(0, 0, ED50_WKID),
        makePoint(4, 0, ED50_WKID),
        refBlockLineWrapper,
        [refBlockLineWrapper],
      );

      const expected = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 1],
              [2, 1],
              [3, 1],
              [4, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        refBlockLineWrapper.id,
      );
      expect(refBlockLineWrapper).toEqual(expected);
    });

    test('does not replace the segment when the start intersection is not found', () => {
      const refBlockLineWrapper = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 0],
              [2, 0],
              [3, 0],
              [4, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        1,
      );
      const licenseLine = makePolyline(
        [
          [
            [20, 20],
            [5, 5],
            [4, 0],
          ],
        ],
        ED50_WKID,
      );

      updateGeodesicReferenceBlockLine(
        [licenseLine],
        makePoint(0, 0, ED50_WKID),
        makePoint(4, 0, ED50_WKID),
        refBlockLineWrapper,
        [refBlockLineWrapper],
      );

      const expected = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 0],
              [2, 0],
              [3, 0],
              [4, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        refBlockLineWrapper.id,
      );
      expect(refBlockLineWrapper).toEqual(expected);
    });

    test('does not replace the segment when the end intersection is not found', () => {
      const refBlockLineWrapper = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 0],
              [2, 0],
              [3, 0],
              [4, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        1,
      );
      const licenseLine = makePolyline(
        [
          [
            [0, 0],
            [5, 5],
            [20, 20],
          ],
        ],
        ED50_WKID,
      );

      updateGeodesicReferenceBlockLine(
        [licenseLine],
        makePoint(0, 0, ED50_WKID),
        makePoint(4, 0, ED50_WKID),
        refBlockLineWrapper,
        [refBlockLineWrapper],
      );

      const expected = makeLineWithNavigationAndId(
        makePolyline(
          [
            [
              [0, 0],
              [1, 0],
              [2, 0],
              [3, 0],
              [4, 0],
            ],
          ],
          ED50_WKID,
        ),
        LineNavigationType.GEODESIC,
        refBlockLineWrapper.id,
      );
      expect(refBlockLineWrapper).toEqual(expected);
    });
  });
  describe('replaceSegment', () => {
    test('replaces a same-direction segment with the matching section of the license line', () => {
      const refBlockLine = makePolyline(
        [
          [
            [0, 0],
            [1, 0],
            [2, 0],
            [3, 0],
            [4, 0],
          ],
        ],
        ED50_WKID,
      );
      const licenseLine = makePolyline(
        [
          [
            [10, 10],
            [1, 0],
            [2, 1],
            [3, 0],
            [20, 20],
          ],
        ],
        ED50_WKID,
      );

      const result = replaceSegment(refBlockLine, licenseLine, makePoint(1, 0, ED50_WKID), makePoint(3, 0, ED50_WKID), true);

      const expected = makePolyline(
        [
          [
            [0, 0],
            [1, 0],
            [2, 1],
            [3, 0],
            [4, 0],
          ],
        ],
        ED50_WKID,
      );
      expect(result).toEqual(expected);
    });

    test('reverses the license segment when the lines go in opposite directions', () => {
      const refBlockLine = makePolyline(
        [
          [
            [0, 0],
            [1, 0],
            [2, 0],
            [3, 0],
            [4, 0],
          ],
        ],
        ED50_WKID,
      );
      const licenseLine = makePolyline(
        [
          [
            [10, 10],
            [3, 0],
            [2, 1],
            [1, 0],
            [20, 20],
          ],
        ],
        ED50_WKID,
      );

      const result = replaceSegment(refBlockLine, licenseLine, makePoint(1, 0, ED50_WKID), makePoint(3, 0, ED50_WKID), false);

      const expected = makePolyline(
        [
          [
            [0, 0],
            [1, 0],
            [2, 1],
            [3, 0],
            [4, 0],
          ],
        ],
        ED50_WKID,
      );
      expect(result).toEqual(expected);
    });

    test('uses the lower ref block index when the start and end points are passed in reverse order', () => {
      const refBlockLine = makePolyline(
        [
          [
            [0, 0],
            [1, 0],
            [2, 0],
            [3, 0],
            [4, 0],
          ],
        ],
        ED50_WKID,
      );
      const licenseLine = makePolyline(
        [
          [
            [10, 10],
            [3, 0],
            [2, 1],
            [1, 0],
            [20, 20],
          ],
        ],
        ED50_WKID,
      );

      const result = replaceSegment(refBlockLine, licenseLine, makePoint(3, 0, ED50_WKID), makePoint(1, 0, ED50_WKID), true);

      const expected = makePolyline(
        [
          [
            [0, 0],
            [3, 0],
            [2, 1],
            [1, 0],
            [4, 0],
          ],
        ],
        ED50_WKID,
      );
      expect(result).toEqual(expected);
    });
  });
});
