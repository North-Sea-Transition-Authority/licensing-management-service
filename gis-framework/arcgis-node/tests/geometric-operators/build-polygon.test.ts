import { describe, expect, it, vi } from 'vitest';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import { buildPolygon } from '../../src/geometric-operators/build-polygon';

const spatialReference = { wkid: 27700 };

describe('buildPolygon', () => {
  it('should build a polygon from a single closed polyline', () => {
    const polyline = new Polyline({
      paths: [
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

    const expectedPolygon = new Polygon({
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

    const call = {
      request: {
        esriJsonPolylines: [JSON.stringify(polyline.toJSON())],
        coordinateSystemWkid: spatialReference.wkid,
      },
    };

    const callback = vi.fn();
    buildPolygon(call as any, callback);

    expect(callback).toHaveBeenCalledWith(null, { polygonEsriJson: JSON.stringify(expectedPolygon.toJSON()) });
  });

  it('should build a polygon from multiple polylines forming a closed shape', () => {
    const polyline1 = new Polyline({
      paths: [
        [
          [0, 0],
          [0, 10],
          [10, 10],
        ],
      ],
      spatialReference,
    });

    const polyline2 = new Polyline({
      paths: [
        [
          [10, 10],
          [10, 0],
          [0, 0],
        ],
      ],
      spatialReference,
    });

    const expectedPolygon = new Polygon({
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

    const call = {
      request: {
        esriJsonPolylines: [JSON.stringify(polyline1.toJSON()), JSON.stringify(polyline2.toJSON())],
        coordinateSystemWkid: spatialReference.wkid,
      },
    };

    const callback = vi.fn();
    buildPolygon(call as any, callback);

    expect(callback).toHaveBeenCalledWith(null, { polygonEsriJson: JSON.stringify(expectedPolygon.toJSON()) });
  });

  it('should build a polygon with a hole', () => {
    const polyline1 = new Polyline({
      paths: [
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

    const polyline2 = new Polyline({
      paths: [
        [
          [2, 2],
          [2, 7],
          [7, 7],
          [7, 2],
          [2, 2],
        ],
      ],
      spatialReference,
    });

    const expectedPolygon = new Polygon({
      rings: [
        [
          [0, 0],
          [0, 10],
          [10, 10],
          [10, 0],
          [0, 0],
        ],
        [
          [2, 2],
          [7, 2],
          [7, 7],
          [2, 7],
          [2, 2],
        ],
      ],
      spatialReference,
    });

    const call = {
      request: {
        esriJsonPolylines: [JSON.stringify(polyline1.toJSON()), JSON.stringify(polyline2.toJSON())],
        coordinateSystemWkid: spatialReference.wkid,
      },
    };

    const callback = vi.fn();
    buildPolygon(call as any, callback);

    expect(callback).toHaveBeenCalledWith(null, { polygonEsriJson: JSON.stringify(expectedPolygon.toJSON()) });
  });

  it('should return an error when no polygon can be reconstructed from the lines', () => {
    const polyline1 = new Polyline({
      paths: [
        [
          [0, 0],
          [0, 10],
        ],
      ],
      spatialReference,
    });

    const polyline2 = new Polyline({
      paths: [
        [
          [5, 0],
          [5, 10],
        ],
      ],
      spatialReference,
    });

    const call = {
      request: {
        esriJsonPolylines: [JSON.stringify(polyline1.toJSON()), JSON.stringify(polyline2.toJSON())],
        coordinateSystemWkid: spatialReference.wkid,
      },
    };

    const callback = vi.fn();
    buildPolygon(call as any, callback);

    expect(callback.mock.calls[0][0]).toEqual(new Error('No polygons could be built from the provided polylines'));
  });
});
