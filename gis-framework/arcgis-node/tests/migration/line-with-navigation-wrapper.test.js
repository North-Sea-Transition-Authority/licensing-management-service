import { describe, expect, test } from 'vitest';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import * as Terraformer from '@terraformer/arcgis';
import { LineNavigationType } from '../../generated/uk/co/fivium/grpc/gis/LineNavigationType';
import { geoJsonLineInputToLinesWithNavigationTypeAndId } from '../../src/migration/types/line-with-navigation-wrapper';

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

    const geoJsonLineWrappers = [
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

    const expectedMap = new Map([
      [1, { line: geodesicLine, navigationType: LineNavigationType.GEODESIC, id: 1 }],
      [2, { line: loxodromeLine, navigationType: LineNavigationType.LOXODROME, id: 2 }],
    ]);

    const result = geoJsonLineInputToLinesWithNavigationTypeAndId(geoJsonLineWrappers, wkid);

    expect(result).toEqual(expectedMap);
  });

  test('should throw when GeoJsonLineWrapper is missing oracleLineSsid', () => {
    const wkid = 4326;

    const geoJsonLineWrappers = [
      {
        geoJsonString: JSON.stringify({
          type: 'LineString',
          coordinates: [
            [0, 0],
            [10, 10],
          ],
        }),
        isGeodesic: true,
        oracleLineSsid: null,
        connectionOrder: 1,
        ringNumber: 1,
      },
    ];

    expect(() => geoJsonLineInputToLinesWithNavigationTypeAndId(geoJsonLineWrappers, wkid)).toThrow(
      'GeoJsonLineWrapper is missing required field: oracleLineSsid',
    );
  });

  test('should throw when GeoJsonLineWrapper is missing geoJsonString', () => {
    const wkid = 4326;
    const oracleLineSsid = 1;

    const geoJsonLineWrappers = [
      {
        geoJsonString: null,
        isGeodesic: true,
        oracleLineSsid: oracleLineSsid,
        connectionOrder: 1,
        ringNumber: 1,
      },
    ];

    expect(() => geoJsonLineInputToLinesWithNavigationTypeAndId(geoJsonLineWrappers, wkid)).toThrow(
      `GeoJsonLineWrapper with oracleLineSsid ${oracleLineSsid} is missing required field: geoJsonString`,
    );
  });
});
