import Feature from "ol/Feature";
import { describe, expect, it } from "vitest";
import { toSortedFeatureOptions } from "@/feature-option-utils";

function feature(featureId: string, featureName: string, startDepth?: number, endDepth?: number): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  created.set("featureName", featureName);
  created.set("startDepth", startDepth);
  created.set("endDepth", endDepth);
  return created;
}

const allDepths = [{ startDepth: 0, endDepth: Number.NEGATIVE_INFINITY }];

describe("toSortedFeatureOptions", () => {
  it("returns one option per feature id", () => {
    const options = toSortedFeatureOptions([
      feature("feature-1", "Block A"),
      feature("feature-1", "Block A"),
      feature("feature-2", "Block B"),
    ]);

    expect(options).toEqual([
      { featureId: "feature-1", featureName: "Block A", depthRanges: allDepths },
      { featureId: "feature-2", featureName: "Block B", depthRanges: allDepths },
    ]);
  });

  it("collects the depth range of each of a feature's polygons", () => {
    const options = toSortedFeatureOptions([
      feature("feature-1", "Block A", -1000, -3000),
      feature("feature-1", "Block A", 0, -1000),
    ]);

    expect(options).toEqual([
      {
        featureId: "feature-1",
        featureName: "Block A",
        depthRanges: [
          { startDepth: 0, endDepth: -1000 },
          { startDepth: -1000, endDepth: -3000 },
        ],
      },
    ]);
  });

  it("sorts the options by feature name with numbers in numeric order", () => {
    const options = toSortedFeatureOptions([
      feature("feature-1", "Block_10"),
      feature("feature-2", "Charlie"),
      feature("feature-3", "Block_2"),
      feature("feature-4", "alpha"),
    ]);

    expect(options).toEqual([
      { featureId: "feature-4", featureName: "alpha", depthRanges: allDepths },
      { featureId: "feature-3", featureName: "Block_2", depthRanges: allDepths },
      { featureId: "feature-1", featureName: "Block_10", depthRanges: allDepths },
      { featureId: "feature-2", featureName: "Charlie", depthRanges: allDepths },
    ]);
  });

  it("returns no options when there are no features", () => {
    expect(toSortedFeatureOptions([])).toEqual([]);
  });
});
