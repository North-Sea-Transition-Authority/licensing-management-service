import { describe, expect, test, vi } from 'vitest';
import { validateTopologicallyEqual } from '../../../src/migration/handlers/validate-topologically-equal';
import { CoordinateSystem } from '../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem';
import { makePolylineEsriJson } from '../../test-utils/esrijson-test-util';

function makeGrpcInput(x1, y1, x2, y2) {
  return [
    {
      esriJsonPolyline: [
        makePolylineEsriJson([
          [
            [x1, y1],
            [x2, y1],
          ],
        ]),
        makePolylineEsriJson([
          [
            [x2, y1],
            [x2, y2],
          ],
        ]),
        makePolylineEsriJson([
          [
            [x2, y2],
            [x1, y2],
          ],
        ]),
        makePolylineEsriJson([
          [
            [x1, y2],
            [x1, y1],
          ],
        ]),
      ],
    },
  ];
}

describe('validateTopologicallyEqual', () => {
  test('should return valid when child and parent polygons are topologically equal', async () => {
    const polygonAsLines = makeGrpcInput(0, 0, 10, 10);

    const call = {
      request: {
        childPolygons: polygonAsLines,
        parentPolygons: polygonAsLines,
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateTopologicallyEqual(call, callback);

    expect(callback).toHaveBeenCalledWith(null, { isValid: true });
  });

  test('should return not valid when polygons are not topologically equal', async () => {
    const childPolygons = makeGrpcInput(0, 0, 10, 10);
    const parentPolygons = makeGrpcInput(0, 0, 20, 20);

    const call = {
      request: {
        childPolygons,
        parentPolygons,
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateTopologicallyEqual(call, callback);

    expect(callback).toHaveBeenCalledWith(null, {
      isValid: false,
      message: 'Polygons are not topologically equal',
    });
  });
});
