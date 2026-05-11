import { beforeEach, describe, expect, it, vi } from 'vitest';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import { status } from '@grpc/grpc-js';
import { findNorthwestMostLineHandler } from '../../src/handlers/find-northwest-most-line-handler';
import * as esriJsonUtil from '../../src/util/esrijson-util';
import * as findNorthwestMostLineModule from '../../src/geometric-operators/find-northwest-most-line';
import { makePolylineEsriJson } from '../test-utils/esrijson-test-util';

vi.mock('../../src/util/esrijson-util');
vi.mock('../../src/geometric-operators/find-northwest-most-line');

describe('findNorthwestMostLineHandler', () => {
  let mockCallback: any;
  let mockCall: any;
  const testWkid = 4326;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        lines: [],
      },
    };
  });

  it('should return a successful callback with the northwest-most line ID', () => {
    const firstLineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [1, 0],
      ],
    ]);
    const secondLineEsriJson = makePolylineEsriJson([
      [
        [1, 9],
        [2, 9],
      ],
    ]);

    mockCall.request.lines = [
      { id: 'line-1', polyLineEsriJson: firstLineEsriJson },
      { id: 'line-2', polyLineEsriJson: secondLineEsriJson },
    ];

    const mockFirstLine = new Polyline({
      paths: [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });
    const mockSecondLine = new Polyline({
      paths: [
        [
          [1, 9],
          [2, 9],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValueOnce(mockFirstLine).mockReturnValueOnce(mockSecondLine);
    vi.mocked(findNorthwestMostLineModule.findNorthwestMostLine).mockReturnValue('line-2');

    findNorthwestMostLineHandler(mockCall, mockCallback as any);

    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenCalledTimes(2);
    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenNthCalledWith(1, firstLineEsriJson);
    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenNthCalledWith(2, secondLineEsriJson);
    expect(findNorthwestMostLineModule.findNorthwestMostLine).toHaveBeenCalledWith([
      { id: 'line-1', polyline: mockFirstLine },
      { id: 'line-2', polyline: mockSecondLine },
    ]);
    expect(mockCallback).toHaveBeenCalledWith(null, { lineId: 'line-2' });
  });

  it('should call callback with error when findNorthwestMostLine throws', () => {
    const lineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [1, 0],
      ],
    ]);
    mockCall.request.lines = [{ id: 'line-1', polyLineEsriJson: lineEsriJson }];

    const mockLine = new Polyline({
      paths: [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    const testError = new Error('Failed to find northwest-most line');
    vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValue(mockLine);
    vi.mocked(findNorthwestMostLineModule.findNorthwestMostLine).mockImplementation(() => {
      throw testError;
    });

    findNorthwestMostLineHandler(mockCall, mockCallback as any);

    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.message).toBe('Failed to find northwest-most line');
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
    expect(mockCallback).toHaveBeenCalledOnce();
  });
});
