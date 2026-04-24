import { describe, expect, test } from 'vitest';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import * as Terraformer from '@terraformer/arcgis';
import { LineNavigationType } from '../../generated/uk/co/fivium/grpc/gis/LineNavigationType';
import {
  geoJsonLineInputToLinesWithNavigationTypeAndId,
  LineWithNavigationTypeAndId,
} from '../../src/migration/types/line-with-navigation-wrapper';
import { GeoJsonLineWrapper__Output } from '../../generated/uk/co/fivium/grpc/gis/GeoJsonLineWrapper';

describe('geoJsonLineInputToLinesWithNavigationTypeAndId', () => {
  test('should convert GeoJsonLineWrappers to a map of id to LineWithNavigationTypeAndId', () => {
    const wkid = 4326;

    const geodesicGeoJson = JSON.stringify({
      type: 'LineString',
      coordinates: [
        [0, 0],
        [10, 10],
      ],
    });

    const loxodromeGeoJson = JSON.stringify({
      type: 'LineString',
      coordinates: [
        [20, 20],
        [30, 30],
      ],
    });

    const geoJsonLineWrappers: GeoJsonLineWrapper__Output[] = [
      {
        geoJsonString: geodesicGeoJson,
        isGeodesic: true,
        oracleLineSsid: 1,
        connectionOrder: 1,
        ringNumber: 1,
      },
      {
        geoJsonString: loxodromeGeoJson,
        isGeodesic: false,
        oracleLineSsid: 2,
        connectionOrder: 2,
        ringNumber: 1,
      },
    ];

    const geodesicLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(JSON.parse(geodesicGeoJson)));
    geodesicLine.spatialReference = { wkid: wkid };

    const loxodromeLine = Polyline.fromJSON(Terraformer.geojsonToArcGIS(JSON.parse(loxodromeGeoJson)));
    loxodromeLine.spatialReference = { wkid: wkid };

    const expectedMap: Map<number, LineWithNavigationTypeAndId> = new Map([
      [1, { line: geodesicLine, navigationType: LineNavigationType.GEODESIC, id: 1 }],
      [2, { line: loxodromeLine, navigationType: LineNavigationType.LOXODROME, id: 2 }],
    ]);

    const result = geoJsonLineInputToLinesWithNavigationTypeAndId(geoJsonLineWrappers, wkid);

    expect(result).toEqual(expectedMap);
  });
});
