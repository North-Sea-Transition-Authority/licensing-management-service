import Polyline from "@arcgis/core/geometry/Polyline.js";
import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as getLineStartAndEndPointsModule from "../../src/geometric-operators/get-line-start-and-end-points";
import { getLineStartAndEndPointsHandler } from "../../src/handlers/get-line-start-and-end-points-handler";
import * as esriJsonUtil from "../../src/util/esrijson-util";
import { makePolylineEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/util/esrijson-util");
vi.mock("../../src/geometric-operators/get-line-start-and-end-points");

describe("getLineStartAndEndPointsHandler", () => {
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

  it("should return a successful callback with line start and end points", () => {
    const firstLineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [10, 10],
      ],
    ]);
    const secondLineEsriJson = makePolylineEsriJson([
      [
        [20, 20],
        [25, 25],
      ],
    ]);

    mockCall.request.lines = [
      { id: "line-1", polyLineEsriJson: firstLineEsriJson },
      { id: "line-2", polyLineEsriJson: secondLineEsriJson },
    ];

    const mockFirstLine = new Polyline({
      paths: [
        [
          [0, 0],
          [10, 10],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });
    const mockSecondLine = new Polyline({
      paths: [
        [
          [20, 20],
          [25, 25],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });
    const expectedPoints = [
      {
        lineId: "line-1",
        startPoint: { x: 0, y: 0 },
        endPoint: { x: 10, y: 10 },
      },
      {
        lineId: "line-2",
        startPoint: { x: 20, y: 20 },
        endPoint: { x: 25, y: 25 },
      },
    ];

    vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValueOnce(mockFirstLine).mockReturnValueOnce(mockSecondLine);
    vi.mocked(getLineStartAndEndPointsModule.getLineStartAndEndPoints).mockReturnValue(expectedPoints);

    getLineStartAndEndPointsHandler(mockCall, mockCallback as any);

    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenCalledTimes(2);
    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenNthCalledWith(1, firstLineEsriJson);
    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenNthCalledWith(2, secondLineEsriJson);
    expect(getLineStartAndEndPointsModule.getLineStartAndEndPoints).toHaveBeenCalledWith([
      { id: "line-1", polyline: mockFirstLine },
      { id: "line-2", polyline: mockSecondLine },
    ]);
    expect(mockCallback).toHaveBeenCalledWith(null, { lines: expectedPoints });
  });

  it("should call callback with error when getLineStartAndEndPoints throws", () => {
    const lineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [10, 10],
      ],
    ]);
    mockCall.request.lines = [{ id: "line-1", polyLineEsriJson: lineEsriJson }];

    const mockLine = new Polyline({
      paths: [
        [
          [0, 0],
          [10, 10],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    const testError = new Error("Failed to get line start and end points");
    vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValue(mockLine);
    vi.mocked(getLineStartAndEndPointsModule.getLineStartAndEndPoints).mockImplementation(() => {
      throw testError;
    });

    getLineStartAndEndPointsHandler(mockCall, mockCallback as any);

    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.message).toBe("Failed to get line start and end points");
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
    expect(mockCallback).toHaveBeenCalledOnce();
  });
});
