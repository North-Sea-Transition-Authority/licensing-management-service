import { status } from "@grpc/grpc-js";
import { describe, expect, it, vi } from "vitest";
import { CoordinateSystem } from "../../../generated/uk/co/fivium/grpc/gis/CoordinateSystem.ts";
import { LineNavigationType } from "../../../generated/uk/co/fivium/grpc/gis/LineNavigationType.ts";
import { processPolygons, validateReferenceBlock } from "../../../src/migration/handlers/validate-reference-block";
import { getCoordinateSystemWkid } from "../../../src/util/coordinate-system-utils.ts";
import { makePolygon, makeRectanglePolygonWrapper } from "../../test-utils/esrijson-test-util.ts";

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

  describe("processPolygons", () => {
    const offshoreWkid = getCoordinateSystemWkid(CoordinateSystem.ED50);
    const bngWkid = getCoordinateSystemWkid(CoordinateSystem.BRITISH_NATIONAL_GRID);

    it.each([
      { region: "offshore (non-BNG)", wkid: offshoreWkid, navigationType: LineNavigationType.CARTESIAN, expectedGeodesic: true },
      { region: "offshore (non-BNG)", wkid: offshoreWkid, navigationType: LineNavigationType.GEODESIC, expectedGeodesic: true },
      { region: "offshore (non-BNG)", wkid: offshoreWkid, navigationType: LineNavigationType.LOXODROME, expectedGeodesic: false },
      { region: "onshore (BNG)", wkid: bngWkid, navigationType: LineNavigationType.CARTESIAN, expectedGeodesic: false },
      { region: "onshore (BNG)", wkid: bngWkid, navigationType: LineNavigationType.GEODESIC, expectedGeodesic: true },
      { region: "onshore (BNG)", wkid: bngWkid, navigationType: LineNavigationType.LOXODROME, expectedGeodesic: false },
    ])("$region $navigationType lines have isGeodesic=$expectedGeodesic", ({ wkid, navigationType, expectedGeodesic }) => {
      const wrapper = makeRectanglePolygonWrapper(0, 0, 10, 10, navigationType);

      const result = processPolygons([wrapper], wkid);

      expect(result).toEqual({
        unionedPolygon: makePolygon(
          [
            [
              [0, 0],
              [0, 10],
              [10, 10],
              [10, 0],
              [0, 0],
            ],
          ],
          wkid,
        ),
        lines: wrapper.lineWrappers.map(lineWrapper => ({
          esriJsonPolyline: lineWrapper.esriJsonString,
          isGeodesic: expectedGeodesic,
        })),
      });
    });
  });
});
