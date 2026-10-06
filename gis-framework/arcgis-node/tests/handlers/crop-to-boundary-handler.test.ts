import { status } from "@grpc/grpc-js";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as cropToBoundaryModule from "../../src/geometric-operators/crop-to-boundary-operator";
import { cropToBoundaryHandler } from "../../src/handlers/crop-to-boundary-handler";
import { makePolygonEsriJson } from "../test-utils/esrijson-test-util";

vi.mock("../../src/geometric-operators/crop-to-boundary-operator");

const boundaryEsriJson = makePolygonEsriJson([
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

const featuresToBeCropped = [{ featureId: "feature", polygons: [{ id: "polygon", esriJsonPolygon: featurePolygonEsriJson }] }];

describe("cropToBoundaryHandler", () => {
  let mockCallback: any;
  let mockCall: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCallback = vi.fn() as any;
    mockCall = {
      request: {
        esriJsonBoundaryPolygons: [boundaryEsriJson],
        featuresToBeCropped,
      },
    };
  });

  it("should return the crop result for each feature", async () => {
    const cropResults = [{
      originalFeatureId: "feature",
      intersectionStatus: "CROPPED" as const,
      polygonCropResults: [{ polygonId: "polygon", status: "CROPPED" as const, esriJsonCroppedPolygon: "cropped" }],
    }];
    vi.mocked(cropToBoundaryModule.cropToBoundary).mockReturnValue(cropResults);

    cropToBoundaryHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledWith(null, { featureCropResults: cropResults }));
    expect(cropToBoundaryModule.cropToBoundary).toHaveBeenCalledWith([boundaryEsriJson], featuresToBeCropped);
  });

  it("should call callback with error when cropToBoundary throws", async () => {
    const testError = new Error("Failed to crop to boundary");
    vi.mocked(cropToBoundaryModule.cropToBoundary).mockImplementation(() => {
      throw testError;
    });

    cropToBoundaryHandler(mockCall, mockCallback as any);

    await vi.waitFor(() => expect(mockCallback).toHaveBeenCalledOnce());
    const callbackError = mockCallback.mock.calls[0][0];
    expect(callbackError).toBe(testError);
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(mockCallback).toHaveBeenCalledWith(callbackError, null);
  });
});
