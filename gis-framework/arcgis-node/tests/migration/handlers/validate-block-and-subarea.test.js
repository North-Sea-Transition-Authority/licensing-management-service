import { describe, expect, test, vi } from 'vitest';
import { validateBlockAndSubarea } from '../../../src/migration/handlers/validate-block-and-subarea';
import { CoordinateSystem } from '../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem.ts';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType.ts';
import { makeRectanglePolygonWrapper } from '../../test-utils/esrijson-test-util.ts';

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
