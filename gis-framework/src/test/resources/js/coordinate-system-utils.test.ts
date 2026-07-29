import { describe, expect, it } from "vitest";
import {
  bngToWgs84,
  ed50ToWgs84,
  etrs89ToWgs84,
  fromWgs84,
  isOffshore,
  SupportedWkid,
  toWgs84,
  wgs84ToBng,
  wgs84ToEd50,
  wgs84ToEtrs89,
} from "../../../main/resources/js/coordinate-system-utils";

describe("coordinateSystemUtils", () => {
  it("exports the supported spatial reference WKIDs", () => {
    expect(SupportedWkid.ED50_WKID).toBe(4230);
    expect(SupportedWkid.ETRS89_WKID).toBe(4258);
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

  it("converts WGS84 coordinates to ETRS89", () => {
    const [longitude, latitude] = wgs84ToEtrs89(-1.5, 52);

    expect(longitude).toBeCloseTo(-1.5, 5);
    expect(latitude).toBeCloseTo(52, 5);
  });

  it("converts ETRS89 coordinates to WGS84", () => {
    const [longitude, latitude] = etrs89ToWgs84(...wgs84ToEtrs89(-1.5, 52));

    expect(longitude).toBeCloseTo(-1.5, 6);
    expect(latitude).toBeCloseTo(52, 6);
  });

  describe("isOffshore", () => {
    it.each([
      { name: "ED50", wkid: SupportedWkid.ED50_WKID, expected: true },
      { name: "ETRS89", wkid: SupportedWkid.ETRS89_WKID, expected: true },
      { name: "British National Grid", wkid: SupportedWkid.BNG_WKID, expected: false },
    ])("classifies $name as offshore=$expected", ({ wkid, expected }) => {
      expect(isOffshore(wkid)).toBe(expected);
    });
  });

  describe("toWgs84 / fromWgs84 dispatchers", () => {
    it("dispatches toWgs84 to the matching converter", () => {
      expect(toWgs84(SupportedWkid.ED50_WKID, -1.4985404112135283, 52.000869121915954))
        .toEqual(ed50ToWgs84(-1.4985404112135283, 52.000869121915954));
      expect(toWgs84(SupportedWkid.BNG_WKID, 434423.0072384819, 233624.37405498995))
        .toEqual(bngToWgs84(434423.0072384819, 233624.37405498995));
    });

    it("dispatches fromWgs84 to the matching converter", () => {
      expect(fromWgs84(SupportedWkid.ED50_WKID, -1.5, 52)).toEqual(wgs84ToEd50(-1.5, 52));
      expect(fromWgs84(SupportedWkid.BNG_WKID, -1.5, 52)).toEqual(wgs84ToBng(-1.5, 52));
    });

    it("throws for an unsupported WKID", () => {
      // @ts-expect-error deliberately passing an unsupported WKID
      expect(() => toWgs84(9999, 0, 0)).toThrow("Unsupported SRS WKID: 9999");
      // @ts-expect-error deliberately passing an unsupported WKID
      expect(() => fromWgs84(9999, 0, 0)).toThrow("Unsupported SRS WKID: 9999");
    });
  });
});
