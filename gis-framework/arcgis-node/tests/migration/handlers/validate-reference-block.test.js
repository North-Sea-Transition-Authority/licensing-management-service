import { status } from "@grpc/grpc-js";
import { describe, expect, it, vi } from "vitest";
import { CoordinateSystem } from "../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem.ts";
import { LineNavigationType } from "../../../generated/uk/co/fivium/grpc/gis/LineNavigationType.ts";
import { validateReferenceBlock } from "../../../src/migration/handlers/validate-reference-block";
import { makeRectanglePolygonWrapper } from "../../test-utils/esrijson-test-util.ts";

describe("validateReferenceBlock", () => {
  it("valid", async () => {
    const refBlockWrapper = makeRectanglePolygonWrapper(0, 0, 20, 20);
    const licenceBlockWrapper = makeRectanglePolygonWrapper(5, 5, 15, 15);

    const call = {
      request: {
        refBlockPolygonLineWrappersList: [refBlockWrapper],
        licenceBlockPolygonLineWrappersList: [licenceBlockWrapper],
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateReferenceBlock(call, callback);

    expect(callback).toHaveBeenCalledWith(null, { isValid: true });
  });

  it("not valid when licence block is not contained by reference block", async () => {
    const refBlockWrapper = makeRectanglePolygonWrapper(0, 0, 5, 5);
    const licenceBlockWrapper = makeRectanglePolygonWrapper(0, 0, 20, 20);

    const call = {
      request: {
        refBlockPolygonLineWrappersList: [refBlockWrapper],
        licenceBlockPolygonLineWrappersList: [licenceBlockWrapper],
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateReferenceBlock(call, callback);

    expect(callback).toHaveBeenCalledWith(null, {
      isValid: false,
      message: "Reference block does not contain all of its licence blocks.",
    });
  });

  it("not valid when licence block geodesic lines do not overlap with reference block geodesic lines", async () => {
    const refBlockWrapper = makeRectanglePolygonWrapper(0, 0, 20, 20, LineNavigationType.GEODESIC);

    const licenceBlockWrapper = makeRectanglePolygonWrapper(5, 5, 15, 15, LineNavigationType.GEODESIC);

    const call = {
      request: {
        refBlockPolygonLineWrappersList: [refBlockWrapper],
        licenceBlockPolygonLineWrappersList: [licenceBlockWrapper],
        coordinateSystem: CoordinateSystem.ED50,
      },
    };

    const callback = vi.fn();

    await validateReferenceBlock(call, callback);

    expect(callback).toHaveBeenCalledWith(null, {
      isValid: false,
      message: "License block geodesic lines do not overlap reference block geodesic lines.",
    });
  });

  it("returns internal error when validation throws", async () => {
    const refBlockWrapper = makeRectanglePolygonWrapper(0, 0, 20, 20);
    const licenceBlockWrapper = makeRectanglePolygonWrapper(5, 5, 15, 15);

    const call = {
      request: {
        refBlockPolygonLineWrappersList: [refBlockWrapper],
        licenceBlockPolygonLineWrappersList: [licenceBlockWrapper],
        coordinateSystem: CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED,
      },
    };

    const callback = vi.fn();

    await validateReferenceBlock(call, callback);

    const callbackError = callback.mock.calls[0][0];
    expect(callbackError.message).toBe(`Could not determine wkid for ${CoordinateSystem.COORDINATE_SYSTEM_UNSPECIFIED}`);
    expect(callbackError.code).toBe(status.INTERNAL);
    expect(callback).toHaveBeenCalledWith(callbackError, null);
    expect(callback).toHaveBeenCalledOnce();
  });
});
