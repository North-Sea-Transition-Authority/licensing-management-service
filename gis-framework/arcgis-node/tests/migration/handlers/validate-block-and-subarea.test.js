import { describe, expect, test, vi } from 'vitest';
import { validateBlockAndSubarea } from '../../../src/migration/handlers/validate-block-and-subarea';
import { CoordinateSystem } from '../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem.ts';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType.ts';
import { makePolylineEsriJson } from '../../test-utils/esrijson-test-util.ts';

function makeRectanglePolygonWrapper(x1, y1, x2, y2, navigationType = LineNavigationType.LOXODROME) {
  return {
    lineWrapper: [
      {
        esriJsonString: makePolylineEsriJson([
          [
            [x1, y1],
            [x2, y1],
          ],
        ]),
        oracleLineSsid: 1,
        navigationType,
      },
      {
        esriJsonString: makePolylineEsriJson([
          [
            [x2, y1],
            [x2, y2],
          ],
        ]),
        oracleLineSsid: 2,
        navigationType,
      },
      {
        esriJsonString: makePolylineEsriJson([
          [
            [x2, y2],
            [x1, y2],
          ],
        ]),
        oracleLineSsid: 3,
        navigationType,
      },
      {
        esriJsonString: makePolylineEsriJson([
          [
            [x1, y2],
            [x1, y1],
          ],
        ]),
        oracleLineSsid: 4,
        navigationType,
      },
    ],
  };
}

describe('validateBlockAndSubarea', () => {
  test('valid', async () => {
    const parentWrapper = makeRectanglePolygonWrapper(0, 0, 20, 20);
    const childWrapper = makeRectanglePolygonWrapper(5, 5, 15, 15);

    const call = {
      request: {
        childPolygonLineWrappersLists: [childWrapper],
        parentPolygonLineWrappersLists: [parentWrapper],
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateBlockAndSubarea(call, callback);

    expect(callback).toHaveBeenCalledWith(null, { isValid: true });
  });

  test('not valid when child is not contained by parent', async () => {
    const parentWrapper = makeRectanglePolygonWrapper(0, 0, 5, 5);
    const childWrapper = makeRectanglePolygonWrapper(0, 0, 20, 20);

    const call = {
      request: {
        childPolygonLineWrappersLists: [childWrapper],
        parentPolygonLineWrappersLists: [parentWrapper],
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateBlockAndSubarea(call, callback);

    expect(callback).toHaveBeenCalledWith(null, {
      isValid: false,
      message: 'Child is not contained by parent',
    });
  });

  test('not valid when  child geodesic lines do not overlap with parent geodesic lines', async () => {
    const parentWrapper = makeRectanglePolygonWrapper(0, 0, 20, 20, LineNavigationType.GEODESIC);

    const childWrapper = makeRectanglePolygonWrapper(5, 5, 15, 15, LineNavigationType.GEODESIC);

    const call = {
      request: {
        childPolygonLineWrappersLists: [childWrapper],
        parentPolygonLineWrappersLists: [parentWrapper],
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateBlockAndSubarea(call, callback);

    expect(callback).toHaveBeenCalledWith(null, {
      isValid: false,
      message: 'Child geodesic lines do not overlap with parent geodesic lines',
    });
  });
});
