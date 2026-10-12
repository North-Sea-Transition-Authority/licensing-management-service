import Polygon from "@arcgis/core/geometry/Polygon.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as buildPolygonModule from "../../src/geometric-operators/build-polygon";
import { buildFeaturePolygonsHandler } from "../../src/handlers/build-feature-polygons-handler";
import * as esriJsonUtil from "../../src/util/esrijson-util";
import { makePolylineEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/util/esrijson-util");
vi.mock("../../src/geometric-operators/build-polygon");

const ed50Wkid = 4230;
const britishNationalGridWkid = 27700;

const firstPolylineEsriJson = makePolylineEsriJson([[[0, 0], [1, 0]]]);
const secondPolylineEsriJson = makePolylineEsriJson([[[1, 0], [1, 1]]]);
const thirdPolylineEsriJson = makePolylineEsriJson([[[2, 2], [3, 2]]]);
const fourthPolylineEsriJson = makePolylineEsriJson([[[5, 5], [6, 5]]]);

const firstPolyline = new Polyline({ paths: [[[0, 0], [1, 0]]], spatialReference: { wkid: ed50Wkid } });
const secondPolyline = new Polyline({ paths: [[[1, 0], [1, 1]]], spatialReference: { wkid: ed50Wkid } });
const thirdPolyline = new Polyline({ paths: [[[2, 2], [3, 2]]], spatialReference: { wkid: ed50Wkid } });
const fourthPolyline = new Polyline({ paths: [[[5, 5], [6, 5]]], spatialReference: { wkid: britishNationalGridWkid } });

const firstPolygon = new Polygon({ rings: [[[0, 0], [1, 0], [1, 1], [0, 0]]], spatialReference: { wkid: ed50Wkid } });
const secondPolygon = new Polygon({ rings: [[[2, 2], [3, 2], [3, 3], [2, 2]]], spatialReference: { wkid: ed50Wkid } });
const thirdPolygon = new Polygon({
  rings: [[[5, 5], [6, 5], [6, 6], [5, 5]]],
  spatialReference: { wkid: britishNationalGridWkid },
});

describe("buildFeaturePolygonsHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        features: [
          {
            featureId: "first-feature",
            coordinateSystemWkid: ed50Wkid,
            polygons: [
              { polygonId: "first-polygon", esriJsonPolylines: [firstPolylineEsriJson, secondPolylineEsriJson] },
              { polygonId: "second-polygon", esriJsonPolylines: [thirdPolylineEsriJson] },
            ],
          },
          {
            featureId: "second-feature",
            coordinateSystemWkid: britishNationalGridWkid,
            polygons: [
              { polygonId: "third-polygon", esriJsonPolylines: [fourthPolylineEsriJson] },
            ],
          },
        ],
      },
    };
    const esriJsonToPolyline = new Map([
      [firstPolylineEsriJson, firstPolyline],
      [secondPolylineEsriJson, secondPolyline],
      [thirdPolylineEsriJson, thirdPolyline],
      [fourthPolylineEsriJson, fourthPolyline],
    ]);
    vi.mocked(esriJsonUtil.esriJsonToPolyline).mockImplementation(esriJson => esriJsonToPolyline.get(esriJson)!);
  });

  it("should return the built polygons of every feature", () => {
    vi.mocked(buildPolygonModule.buildPolygon)
      .mockReturnValueOnce(firstPolygon)
      .mockReturnValueOnce(secondPolygon)
      .mockReturnValueOnce(thirdPolygon);

    buildFeaturePolygonsHandler(mockCall, mockCallback as any);

    expect(mockCallback).toHaveBeenCalledWith(null, {
      features: [
        {
          featureId: "first-feature",
          polygons: [
            { id: "first-polygon", esriJsonPolygon: JSON.stringify(firstPolygon.toJSON()) },
            { id: "second-polygon", esriJsonPolygon: JSON.stringify(secondPolygon.toJSON()) },
          ],
        },
        {
          featureId: "second-feature",
          polygons: [
            { id: "third-polygon", esriJsonPolygon: JSON.stringify(thirdPolygon.toJSON()) },
          ],
        },
      ],
    });
    expect(vi.mocked(buildPolygonModule.buildPolygon).mock.calls).toEqual([
      [[firstPolyline, secondPolyline], ed50Wkid],
      [[thirdPolyline], ed50Wkid],
      [[fourthPolyline], britishNationalGridWkid],
    ]);
  });

  it("should call callback with error when a polygon cannot be built", () => {
    vi.mocked(buildPolygonModule.buildPolygon)
      .mockReturnValueOnce(firstPolygon)
      .mockReturnValueOnce(undefined);

    buildFeaturePolygonsHandler(mockCall, mockCallback as any);

    expect(mockCallback).toHaveBeenCalledOnce();
    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError.message).toBe("No polygon could be built from the polylines of polygon second-polygon");
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
  });
});
