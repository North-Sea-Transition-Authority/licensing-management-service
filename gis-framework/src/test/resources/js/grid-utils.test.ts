import { describe, expect, it } from "vitest";
import { SupportedWkid } from "@/coordinate-system-utils";
import { generateSnapPoints, getSpacingForZoom } from "@/grid-utils";

describe("generateSnapPoints", () => {
  it("generates snap points for a ED50 grid", () => {
    const points = generateSnapPoints(0.998, 51.998, 1.008, 52.007, SupportedWkid.ED50_WKID, 30);

    // Regex used for displayName because the geodesy Dms library separates parts with U+202F which is fragile as editors silently normalise the character on save.
    expect(points).toEqual([
      { id: "3600,187200", coordinates: [expect.closeTo(0.9986, 2), expect.closeTo(51.9992, 2)], originalSrsCoordinates: [expect.closeTo(1.0, 4), expect.closeTo(52.0, 4)], displayName: expect.stringMatching(/52[^\d\n]+00[^\d\n]+00[^\d\n]+N\n1[^\d\n]+00[^\d\n]+00[^\d\n]+E/) },
      { id: "3600,187230", coordinates: [expect.closeTo(0.9986, 2), expect.closeTo(52.0075, 2)], originalSrsCoordinates: [expect.closeTo(1.0, 4), expect.closeTo(52.0083, 4)], displayName: expect.stringMatching(/52[^\d\n]+00[^\d\n]+30[^\d\n]+N\n1[^\d\n]+00[^\d\n]+00[^\d\n]+E/) },
      { id: "3630,187200", coordinates: [expect.closeTo(1.0069, 2), expect.closeTo(51.9992, 2)], originalSrsCoordinates: [expect.closeTo(1.0083, 4), expect.closeTo(52.0, 4)], displayName: expect.stringMatching(/52[^\d\n]+00[^\d\n]+00[^\d\n]+N\n1[^\d\n]+00[^\d\n]+30[^\d\n]+E/) },
      { id: "3630,187230", coordinates: [expect.closeTo(1.0069, 2), expect.closeTo(52.0075, 2)], originalSrsCoordinates: [expect.closeTo(1.0083, 4), expect.closeTo(52.0083, 4)], displayName: expect.stringMatching(/52[^\d\n]+00[^\d\n]+30[^\d\n]+N\n1[^\d\n]+00[^\d\n]+30[^\d\n]+E/) },
    ]);
  });

  it("generates snap points for a BNG grid", () => {
    const points = generateSnapPoints(0.998, 51.998, 1.008, 52.007, SupportedWkid.BNG_WKID, 500);

    expect(points).toEqual([
      { id: "12120,4750", coordinates: [expect.closeTo(0.9993, 2), expect.closeTo(51.9977, 2)], originalSrsCoordinates: [606000, 237500], displayName: "TM 0600 3750" },
      { id: "12120,4760", coordinates: [expect.closeTo(0.9996, 2), expect.closeTo(52.0022, 2)], originalSrsCoordinates: [606000, 238000], displayName: "TM 0600 3800" },
      { id: "12120,4770", coordinates: [expect.closeTo(0.9999, 2), expect.closeTo(52.0066, 2)], originalSrsCoordinates: [606000, 238500], displayName: "TM 0600 3850" },
      { id: "12130,4750", coordinates: [expect.closeTo(1.0066, 2), expect.closeTo(51.9975, 2)], originalSrsCoordinates: [606500, 237500], displayName: "TM 0650 3750" },
      { id: "12130,4760", coordinates: [expect.closeTo(1.00695, 2), expect.closeTo(52.0020, 2)], originalSrsCoordinates: [606500, 238000], displayName: "TM 0650 3800" },
      { id: "12130,4770", coordinates: [expect.closeTo(1.0072, 2), expect.closeTo(52.0065, 2)], originalSrsCoordinates: [606500, 238500], displayName: "TM 0650 3850" },
    ]);
  });

  it("generates fewer snap points with a larger ED50 spacing", () => {
    const finerPoints = generateSnapPoints(0.0, 50.0, 1.0, 51.0, SupportedWkid.ED50_WKID, 30);
    const coarserPoints = generateSnapPoints(0.0, 50.0, 1.0, 51.0, SupportedWkid.ED50_WKID, 3600);
    expect(finerPoints.length).toBeGreaterThan(coarserPoints.length);
  });

  it("generates fewer snap points with a larger BNG spacing", () => {
    const finerPoints = generateSnapPoints(-2.1, 53.9, -1.9, 54.1, SupportedWkid.BNG_WKID, 500);
    const coarserPoints = generateSnapPoints(-2.1, 53.9, -1.9, 54.1, SupportedWkid.BNG_WKID, 1000);
    expect(finerPoints.length).toBeGreaterThan(coarserPoints.length);
  });

  it.for([0, -1])("rejects non-positive custom spacing: %d", (spacing) => {
    expect(() => generateSnapPoints(0, 50, 1, 51, SupportedWkid.ED50_WKID, spacing))
      .toThrow(`Grid spacing must be greater than zero: ${spacing}`);
  });

  it("produces stable IDs for ED50 across different spacing tiers", () => {
    const coarsePoints = generateSnapPoints(0.998, 51.998, 1.008, 52.007, SupportedWkid.ED50_WKID, 30);
    const finePoints = generateSnapPoints(0.998, 51.998, 1.008, 52.007, SupportedWkid.ED50_WKID, 5);

    const fineIds = new Set(finePoints.map(p => p.id));
    coarsePoints.forEach(p => expect(fineIds.has(p.id)).toBe(true));
  });

  it("produces stable IDs for BNG across different spacing tiers", () => {
    const coarsePoints = generateSnapPoints(0.998, 51.998, 1.008, 52.007, SupportedWkid.BNG_WKID, 500);
    const finePoints = generateSnapPoints(0.998, 51.998, 1.008, 52.007, SupportedWkid.BNG_WKID, 100);

    const fineIds = new Set(finePoints.map(p => p.id));
    coarsePoints.forEach(p => expect(fineIds.has(p.id)).toBe(true));
  });

  describe("bng valid bounds filtering", () => {
    it.for([
      { label: "south (central France — large negative northings)", args: [0, 43, 5, 45] },
      { label: "west (mid-Atlantic — large negative eastings)", args: [-15, 52, -10, 55] },
      { label: "east (continental Europe — eastings beyond 700,000 m)", args: [10, 50, 15, 55] },
    ] as { label: string, args: [number, number, number, number] }[])(
      "returns empty array when extent is entirely $label of BNG valid area",
      ({ args }) => {
        const points = generateSnapPoints(...args, SupportedWkid.BNG_WKID, 500);
        expect(points).toEqual([]);
      },
    );

    it("generates snap points only within valid BNG bounds when extent in between the southern boundary", () => {
      // Covers southern England + northern France; southern portion maps to negative northing
      const points = generateSnapPoints(-2, 49.5, 1, 51, SupportedWkid.BNG_WKID, 500);
      expect(points.length).toBeGreaterThan(0);
      points.forEach((p) => {
        expect(p.originalSrsCoordinates[1]).toBeGreaterThanOrEqual(0);
        expect(p.originalSrsCoordinates[1]).toBeLessThanOrEqual(1300000);
      });
    });

    it("does not apply bounds filtering for ED50 extents that would be outside BNG", () => {
      // Same southern-France extent — ED50 has no validBounds and should still produce points
      const points = generateSnapPoints(0, 43, 5, 45, SupportedWkid.ED50_WKID, 3600);
      expect(points.length).toBeGreaterThan(0);
    });
  });
});

describe("getSpacingForZoom", () => {
  it.for([
    [11, 60],
    [12, 30],
    [13, 15],
    [14, 10],
    [15, 5],
  ] as [number, number][])("ed50 zoom %d → %d arc-seconds", ([zoom, expected]) => {
    expect(getSpacingForZoom(zoom, SupportedWkid.ED50_WKID)).toBe(expected);
  });

  it.for([
    [12, 1000],
    [13, 500],
    [14, 250],
    [15, 100],
  ] as [number, number][])("bng zoom %d → %d metres", ([zoom, expected]) => {
    expect(getSpacingForZoom(zoom, SupportedWkid.BNG_WKID)).toBe(expected);
  });
});
