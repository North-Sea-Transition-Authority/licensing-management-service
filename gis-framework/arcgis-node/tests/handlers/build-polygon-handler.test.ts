import Polygon from "@arcgis/core/geometry/Polygon.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as buildPolygonModule from "../../src/geometric-operators/build-polygon";
import * as projectPolygon from "../../src/geometric-operators/project-polygon";
import { buildPolygonHandler } from "../../src/handlers/build-polygon-handler";
import * as esriJsonUtil from "../../src/util/esrijson-util";
import { makePolylineEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/util/esrijson-util");
vi.mock("../../src/geometric-operators/build-polygon");
vi.mock("../../src/geometric-operators/project-polygon", () => ({
  projectToWgs84: vi.fn(),
}));

describe("buildPolygonHandler", () => {
  let mockCallback: any;
  let mockCall: any;
  const testWkid = 4326;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        esriJsonPolylines: [],
        coordinateSystemWkid: testWkid,
        projectToWgs84: false,
      },
    };
  });

  it("should return a successful callback when no errors", async () => {
    const polylineEsriJson1 = makePolylineEsriJson([
      [
        [0, 0],
        [1, 0],
      ],
    ]);
    const polylineEsriJson2 = makePolylineEsriJson([
      [
        [1, 0],
        [1, 1],
      ],
    ]);

    mockCall.request.esriJsonPolylines = [polylineEsriJson1, polylineEsriJson2];

    const mockPolyline1 = new Polyline({
      paths: [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    const mockPolyline2 = new Polyline({
      paths: [
        [
          [1, 0],
          [1, 1],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    const mockPolygon = new Polygon({
      rings: [
        [
          [0, 0],
          [1, 0],
          [1, 1],
          [0, 1],
          [0, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValueOnce(mockPolyline1).mockReturnValueOnce(mockPolyline2);
    vi.mocked(buildPolygonModule.buildPolygon).mockReturnValue(mockPolygon);

    buildPolygonHandler(mockCall, mockCallback as any);

    await vi.waitFor(() =>
      expect(mockCallback).toHaveBeenCalledWith(null, { polygonEsriJson: JSON.stringify(mockPolygon.toJSON()) }),
    );
    expect(esriJsonUtil.esriJsonToPolyline).toHaveBeenCalledTimes(2);
    expect(buildPolygonModule.buildPolygon).toHaveBeenCalledWith([mockPolyline1, mockPolyline2], testWkid);
    expect(projectPolygon.projectToWgs84).toHaveBeenCalledTimes(0);
  });

  it("should call callback with error when buildPolygon returns undefined", async () => {
    const polylineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [1, 0],
      ],
    ]);
    mockCall.request.esriJsonPolylines = [polylineEsriJson];

    const mockPolyline = new Polyline({
      paths: [
        [
          [0, 0],
          [1, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValue(mockPolyline);
    vi.mocked(buildPolygonModule.buildPolygon).mockReturnValue(undefined);

    buildPolygonHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledOnce());
    const callArgs = mockCallback.mock.calls[0];
    expect(callArgs[0]).toBeInstanceOf(Error);
    expect(callArgs[0].message).toBe("No polygons could be built from the provided polylines");
    expect(callArgs[1]).toBeNull();
    expect(projectPolygon.projectToWgs84).toHaveBeenCalledTimes(0);
  });

  it("should call callback with error when buildPolygon throws", async () => {
    const polylineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [1, 0],
        [1, 1],
        [0, 1],
        [0, 0],
      ],
    ]);
    mockCall.request.esriJsonPolylines = [polylineEsriJson];

    const mockPolyline = new Polyline({
      paths: [
        [
          [0, 0],
          [1, 0],
          [1, 1],
          [0, 1],
          [0, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    const testError = new Error("Failed to build polygon");
    vi.mocked(esriJsonUtil.esriJsonToPolyline).mockReturnValue(mockPolyline);
    vi.mocked(buildPolygonModule.buildPolygon).mockImplementation(() => {
      throw testError;
    });

    buildPolygonHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(testError, null));
    expect(mockCallback).toHaveBeenCalledOnce();
    expect(projectPolygon.projectToWgs84).toHaveBeenCalledTimes(0);
  });

  it("should project polygon to WGS84 when flag is true", async () => {
    const polylineEsriJson = makePolylineEsriJson([
      [
        [0, 0],
        [1, 0],
      ],
    ]);
    mockCall.request.esriJsonPolylines = [polylineEsriJson];
    mockCall.request.projectToWgs84 = true;

    const mockPolygon = new Polygon({
      rings: [
        [
          [0, 0],
          [1, 0],
          [1, 1],
          [0, 1],
          [0, 0],
        ],
      ],
      spatialReference: { wkid: testWkid },
    });

    vi.mocked(buildPolygonModule.buildPolygon).mockReturnValue(mockPolygon);

    const mockPolygonProjected = new Polygon({
      rings: [
        [
          [0, 0],
          [1, 0],
          [1, 1],
          [0, 1],
          [0, 0],
        ],
      ],
      spatialReference: { wkid: 4230 },
    });
    vi.mocked(projectPolygon.projectToWgs84).mockResolvedValue(mockPolygonProjected);

    buildPolygonHandler(mockCall, mockCallback as any);

    const expectedResponse = { polygonEsriJson: JSON.stringify(mockPolygonProjected.toJSON()) };
    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, expectedResponse));
  });
});
