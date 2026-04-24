import { beforeEach, describe, expect, it, vi } from 'vitest';
import Polygon from '@arcgis/core/geometry/Polygon.js';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import { splitPolygonHandler } from '../../src/handlers/split-polygon-handler';
import * as esriJsonUtil from '../../src/util/esrijson-util';
import * as splitPolygonModule from '../../src/geometric-operators/split-operator';
import { makePolygonEsriJson, makePolylineEsriJson } from '../test-utils/esrijson-test-util';

vi.mock('../../src/util/esrijson-util');
vi.mock('../../src/geometric-operators/split-operator');

describe('splitPolygonHandler', () => {
  let mockCallback: any;
  let mockCall: any;
  const testWkid = 4326;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        esriJsonPolygonTarget: null,
        esriJsonLineCutter: null,
      },
    };
  });

  describe('splitPolygonHandler', () => {
    it('should return a successful callback with split polygons', () => {
      // Arrange
      const polygonEsriJson = makePolygonEsriJson([
        [
          [0, 0],
          [2, 0],
          [2, 2],
          [0, 2],
          [0, 0],
        ],
      ]);
      const polylineEsriJson = makePolylineEsriJson([
        [
          [1, 0],
          [1, 2],
        ],
      ]);

      mockCall.request.esriJsonPolygonTarget = polygonEsriJson;
      mockCall.request.esriJsonLineCutter = polylineEsriJson;

      const mockTargetPolygon = new Polygon({
        rings: [
          [
            [0, 0],
            [2, 0],
            [2, 2],
            [0, 2],
            [0, 0],
          ],
        ],
        spatialReference: { wkid: testWkid },
      });

      const mockCutterLine = new Polyline({
        paths: [
          [
            [1, 0],
            [1, 2],
          ],
        ],
        spatialReference: { wkid: testWkid },
      });

      const mockPolygon1 = new Polygon({
        rings: [
          [
            [0, 0],
            [1, 0],
            [1, 2],
            [0, 2],
            [0, 0],
          ],
        ],
        spatialReference: { wkid: testWkid },
      });

      const mockPolygon2 = new Polygon({
        rings: [
          [
            [1, 0],
            [2, 0],
            [2, 2],
            [1, 2],
            [1, 0],
          ],
        ],
        spatialReference: { wkid: testWkid },
      });

      vi.mocked(esriJsonUtil.esriJsonToPolygon).mockReturnValue(mockTargetPolygon);
      vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValue(mockCutterLine);
      vi.mocked(splitPolygonModule.splitPolygon).mockReturnValue([mockPolygon1, mockPolygon2]);

      // Act
      splitPolygonHandler(mockCall, mockCallback as any);

      // Assert
      expect(esriJsonUtil.esriJsonToPolygon).toHaveBeenCalledWith(polygonEsriJson);
      expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenCalledWith(polylineEsriJson);
      expect(splitPolygonModule.splitPolygon).toHaveBeenCalledWith(mockTargetPolygon, mockCutterLine);
      expect(mockCallback).toHaveBeenCalledWith(null, {
        outputPolygonEsriJsons: [JSON.stringify(mockPolygon1.toJSON()), JSON.stringify(mockPolygon2.toJSON())],
      });
    });

    it('should return empty array when splitPolygon returns empty array', () => {
      // Arrange
      const polygonEsriJson = makePolygonEsriJson([
        [
          [0, 0],
          [2, 0],
          [2, 2],
          [0, 2],
          [0, 0],
        ],
      ]);
      const polylineEsriJson = makePolylineEsriJson([
        [
          [1, 0],
          [1, 2],
        ],
      ]);

      mockCall.request.esriJsonPolygonTarget = polygonEsriJson;
      mockCall.request.esriJsonLineCutter = polylineEsriJson;

      const mockTargetPolygon = new Polygon({
        rings: [
          [
            [0, 0],
            [2, 0],
            [2, 2],
            [0, 2],
            [0, 0],
          ],
        ],
        spatialReference: { wkid: testWkid },
      });

      const mockCutterLine = new Polyline({
        paths: [
          [
            [1, 0],
            [1, 2],
          ],
        ],
        spatialReference: { wkid: testWkid },
      });

      vi.mocked(esriJsonUtil.esriJsonToPolygon).mockReturnValue(mockTargetPolygon);
      vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValue(mockCutterLine);
      vi.mocked(splitPolygonModule.splitPolygon).mockReturnValue([]);

      // Act
      splitPolygonHandler(mockCall, mockCallback as any);

      // Assert
      expect(mockCallback).toHaveBeenCalledWith(null, { outputPolygonEsriJsons: [] });
    });

    it('should call callback with error when splitPolygon throws', () => {
      // Arrange
      const polygonEsriJson = makePolygonEsriJson([
        [
          [0, 0],
          [2, 0],
          [2, 2],
          [0, 2],
          [0, 0],
        ],
      ]);
      const polylineEsriJson = makePolylineEsriJson([
        [
          [1, 0],
          [1, 2],
        ],
      ]);

      mockCall.request.esriJsonPolygonTarget = polygonEsriJson;
      mockCall.request.esriJsonLineCutter = polylineEsriJson;

      const mockTargetPolygon = new Polygon({
        rings: [
          [
            [0, 0],
            [2, 0],
            [2, 2],
            [0, 2],
            [0, 0],
          ],
        ],
        spatialReference: { wkid: testWkid },
      });

      const mockCutterLine = new Polyline({
        paths: [
          [
            [1, 0],
            [1, 2],
          ],
        ],
        spatialReference: { wkid: testWkid },
      });

      const testError = new Error('Failed to split polygon');

      vi.mocked(esriJsonUtil.esriJsonToPolygon).mockReturnValue(mockTargetPolygon);
      vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValue(mockCutterLine);
      vi.mocked(splitPolygonModule.splitPolygon).mockImplementation(() => {
        throw testError;
      });

      // Act
      splitPolygonHandler(mockCall, mockCallback as any);

      // Assert
      expect(mockCallback).toHaveBeenCalledWith(testError, null);
      expect(mockCallback).toHaveBeenCalledOnce();
    });
  });
});
