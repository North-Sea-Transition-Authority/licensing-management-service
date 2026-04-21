import { describe, expect, test } from 'vitest';
import { CoordinateSystem } from '../../generated/arcgisjs/CoordinateSystem';
import { getCoordinateSystemWkid } from '../../src/util/coordinate-system-utils.ts';

describe('coordinate-system-utils', () => {
  test.each([
    [CoordinateSystem.ED50, 4230],
    [CoordinateSystem.BRITISH_NATIONAL_GRID, 27700],
    [CoordinateSystem.WGS84, 4326],
  ])('getCoordinateSystemWkid(%s) === %s', (coordinateSystem, wkid) => {
    expect(getCoordinateSystemWkid(coordinateSystem)).toEqual(wkid);
  });

  test('unknown coordinate system throws error', () => {
    expect(() => getCoordinateSystemWkid(CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED)).toThrow(
      new Error(`Could not determine wkid for ${CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED}`),
    );
  });
});
