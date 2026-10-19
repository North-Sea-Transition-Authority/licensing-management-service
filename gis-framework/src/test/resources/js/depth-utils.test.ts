import Feature from "ol/Feature";
import { describe, expect, it } from "vitest";
import { canSplitAt, depthRangesOf, endDepthOf, formatDepthRanges, startDepthOf } from "@/depth-utils";

function feature(featureId: string, startDepth?: number, endDepth?: number): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  created.set("startDepth", startDepth);
  created.set("endDepth", endDepth);
  return created;
}

describe("startDepthOf", () => {
  it.each([
    { startDepth: undefined, expected: 0 },
    { startDepth: -1000, expected: -1000 },
  ])("returns $expected when the start depth is $startDepth", ({ startDepth, expected }) => {
    expect(startDepthOf(feature("feature-1", startDepth))).toBe(expected);
  });
});

describe("endDepthOf", () => {
  it.each([
    { endDepth: undefined, expected: Number.NEGATIVE_INFINITY },
    { endDepth: -1000, expected: -1000 },
  ])("returns $expected when the end depth is $endDepth", ({ endDepth, expected }) => {
    expect(endDepthOf(feature("feature-1", 0, endDepth))).toBe(expected);
  });
});

describe("canSplitAt", () => {
  const depthRanges = [
    { startDepth: 0, endDepth: -2500 },
    { startDepth: -3000, endDepth: -3500 },
  ];

  it.each([
    { depth: -1000, expected: true },
    { depth: -3250, expected: true },
    { depth: 0, expected: false },
    { depth: -2500, expected: false },
    { depth: -2750, expected: false },
    { depth: -3000, expected: false },
    { depth: -3500, expected: false },
    { depth: -4000, expected: false },
  ])("returns $expected at depth $depth", ({ depth, expected }) => {
    expect(canSplitAt(depthRanges, depth)).toBe(expected);
  });

  it("can split any depth below 0 when the range has no end", () => {
    expect(canSplitAt([{ startDepth: 0, endDepth: Number.NEGATIVE_INFINITY }], -100000)).toBe(true);
  });
});

describe("formatDepthRanges", () => {
  it("joins each range and shows a range with no end as -infinity", () => {
    const formatted = formatDepthRanges([
      { startDepth: 0, endDepth: -1000 },
      { startDepth: -1000, endDepth: Number.NEGATIVE_INFINITY },
    ]);

    expect(formatted).toBe("0 to -1000, -1000 to -infinity");
  });
});

describe("depthRangesOf", () => {
  it("returns the feature's ranges shallowest first, ignoring other features", () => {
    const depthRanges = depthRangesOf([
      feature("feature-1", -1000, -3000),
      feature("feature-2", 0, -500),
      feature("feature-1", 0, -1000),
    ], "feature-1");

    expect(depthRanges).toEqual([
      { startDepth: 0, endDepth: -1000 },
      { startDepth: -1000, endDepth: -3000 },
    ]);
  });

  it("returns one range for polygons at the same depths", () => {
    const depthRanges = depthRangesOf([
      feature("feature-1", 0, -1000),
      feature("feature-1", 0, -1000),
    ], "feature-1");

    expect(depthRanges).toEqual([{ startDepth: 0, endDepth: -1000 }]);
  });
});
