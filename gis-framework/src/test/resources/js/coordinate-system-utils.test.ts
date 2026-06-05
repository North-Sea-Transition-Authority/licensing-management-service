import { describe, expect, it } from "vitest";
import {
  bngToWgs84,
  ed50ToWgs84,
  SupportedWkid,
  wgs84ToBng,
  wgs84ToEd50,
} from "../../../main/resources/js/coordinate-system-utils";

describe("coordinateSystemUtils", () => {
  it("exports the supported spatial reference WKIDs", () => {
    expect(SupportedWkid.ED50_WKID).toBe(4230);
    expect(SupportedWkid.BNG_WKID).toBe(27700);
  });

  it("converts WGS84 coordinates to ED50", () => {
    const [longitude, latitude] = wgs84ToEd50(-1.5, 52);

    expect(longitude).toBeCloseTo(-1.498540, 6);
    expect(latitude).toBeCloseTo(52.000869, 6);
  });

  it("converts ED50 coordinates to WGS84", () => {
    const [longitude, latitude] = ed50ToWgs84(-1.4985404112135283, 52.000869121915954);

    expect(longitude).toBeCloseTo(-1.5, 6);
    expect(latitude).toBeCloseTo(52, 6);
  });

  it("converts WGS84 coordinates to British National Grid", () => {
    const [easting, northing] = wgs84ToBng(-1.5, 52);

    expect(easting).toBeCloseTo(434423.007, 3);
    expect(northing).toBeCloseTo(233624.374, 3);
  });

  it("converts British National Grid coordinates to WGS84", () => {
    const [longitude, latitude] = bngToWgs84(434423.0072384819, 233624.37405498995);

    expect(longitude).toBeCloseTo(-1.5, 6);
    expect(latitude).toBeCloseTo(52, 6);
  });
});
