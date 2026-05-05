import { describe, expect, it, vi } from 'vitest';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import { splitPolygon } from '../../src/geometric-operators/split-operator.js';
import * as unionOperator from '@arcgis/core/geometry/operators/unionOperator.js';
import * as equalsOperator from '@arcgis/core/geometry/operators/equalsOperator.js';
import { esriJsonToPolygon } from '../../src/util/esrijson-util';

const spatialReference = { wkid: 27700 };

const targetPolygon = new Polygon({
  rings: [
    [
      [0, 0],
      [0, 10],
      [10, 10],
      [10, 0],
      [0, 0],
    ],
  ],
  spatialReference,
});
describe('splitPolygon', () => {
  it('should split a polygon into two when cut by a line', () => {
    const cutter = new Polyline({
      paths: [
        [
          [0, 0],
          [10, 10],
        ],
      ],
      spatialReference,
    });

    const call = {
      request: {
        esriJsonPolygonTarget: JSON.stringify(targetPolygon.toJSON()),
        esriJsonLineCutter: JSON.stringify(cutter.toJSON()),
      },
    };

    const callback = vi.fn();
    splitPolygon(call as any, callback);

    expect(callback.mock.calls[0][0]).toBeNull();
    const polygonsJson = callback.mock.calls[0][1].outputPolygonEsriJsons;
    expect(polygonsJson).toHaveLength(2);

    const polygons: Polygon[] = polygonsJson.map((json: string) => esriJsonToPolygon(json));
    const mergedResult = unionOperator.execute(polygons[0], polygons[1]);
    expect(equalsOperator.execute(mergedResult, targetPolygon)).toBe(true);
  });

  it('should return empty when cutter does not intersect the polygon', () => {
    const cutter = new Polyline({
      paths: [
        [
          [20, 20],
          [30, 30],
        ],
      ],
      spatialReference,
    });

    const call = {
      request: {
        esriJsonPolygonTarget: JSON.stringify(targetPolygon.toJSON()),
        esriJsonLineCutter: JSON.stringify(cutter.toJSON()),
      },
    };

    const callback = vi.fn();
    splitPolygon(call as any, callback);

    expect(callback).toHaveBeenCalledWith(null, { outputPolygonEsriJsons: [] });
  });

  it('should handle a cutter with overlapping/backtracking segments', () => {
    const cutter = new Polyline({
      paths: [
        [
          [0, 5],
          [10, 5],
          [1, 5], // Line backtracks
        ],
      ],
      spatialReference,
    });

    const call = {
      request: {
        esriJsonPolygonTarget: JSON.stringify(targetPolygon.toJSON()),
        esriJsonLineCutter: JSON.stringify(cutter.toJSON()),
      },
    };

    const callback = vi.fn();
    splitPolygon(call as any, callback);

    expect(callback.mock.calls[0][0]).toBeNull();
    const polygonsJson = callback.mock.calls[0][1].outputPolygonEsriJsons;
    expect(polygonsJson).toHaveLength(2);

    const polygons: Polygon[] = polygonsJson.map((json: string) => esriJsonToPolygon(json));
    const mergedResult = unionOperator.execute(polygons[0], polygons[1]);
    expect(equalsOperator.execute(mergedResult, targetPolygon)).toBe(true);
    polygons.forEach((poly) => {
      expect(poly.rings[0].length).toBe(5);
    });
  });
});
