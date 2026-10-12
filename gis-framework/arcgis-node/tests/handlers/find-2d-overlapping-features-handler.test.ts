import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as find2dOverlappingFeaturesModule from "../../src/geometric-operators/find-2d-overlapping-features-operator";
import { find2dOverlappingFeaturesHandler } from "../../src/handlers/find-2d-overlapping-features-handler";
import { makePolygonEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/geometric-operators/find-2d-overlapping-features-operator");

const shapeEsriJson = makePolygonEsriJson([
  [
    [0, 0],
    [10, 0],
    [10, 10],
    [0, 10],
    [0, 0],
  ],
]);

const featurePolygonEsriJson = makePolygonEsriJson([
  [
    [8, 2],
    [14, 2],
    [14, 6],
    [8, 6],
    [8, 2],
  ],
]);

const featuresToCompare = [{ featureId: "feature", polygons: [{ id: "polygon", esriJsonPolygon: featurePolygonEsriJson }] }];

describe("find2dOverlappingFeaturesHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        esriJsonPolygons: [shapeEsriJson],
        featuresToCompare,
      },
    };
  });

  it("should return the ids of the overlapping features", () => {
    vi.mocked(find2dOverlappingFeaturesModule.find2dOverlappingFeatures).mockReturnValue(["feature"]);

    find2dOverlappingFeaturesHandler(mockCall, mockCallback as any);

    expect(mockCallback).toHaveBeenCalledWith(null, { overlappingFeatureIds: ["feature"] });
    expect(find2dOverlappingFeaturesModule.find2dOverlappingFeatures).toHaveBeenCalledWith([shapeEsriJson], featuresToCompare);
  });

  it("should call callback with error when find2dOverlappingFeatures throws", () => {
    const testError = new Error("Failed to find overlapping features");
    vi.mocked(find2dOverlappingFeaturesModule.find2dOverlappingFeatures).mockImplementation(() => {
      throw testError;
    });

    find2dOverlappingFeaturesHandler(mockCall, mockCallback as any);

    expect(mockCallback).toHaveBeenCalledOnce();
    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
  });
});
