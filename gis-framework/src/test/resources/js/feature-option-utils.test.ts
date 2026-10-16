import Feature from "ol/Feature";
import { describe, expect, it } from "vitest";
import { toSortedFeatureOptions } from "@/feature-option-utils";

function feature(featureId: string, featureName: string): Feature {
  const created = new Feature();
  created.set("featureId", featureId);
  created.set("featureName", featureName);
  return created;
}

describe("toSortedFeatureOptions", () => {
  it("returns one option per feature id", () => {
    const options = toSortedFeatureOptions([
      feature("feature-1", "Block A"),
      feature("feature-1", "Block A"),
      feature("feature-2", "Block B"),
    ]);

    expect(options).toEqual([
      { featureId: "feature-1", featureName: "Block A" },
      { featureId: "feature-2", featureName: "Block B" },
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
      { featureId: "feature-4", featureName: "alpha" },
      { featureId: "feature-3", featureName: "Block_2" },
      { featureId: "feature-1", featureName: "Block_10" },
      { featureId: "feature-2", featureName: "Charlie" },
    ]);
  });

  it("returns no options when there are no features", () => {
    expect(toSortedFeatureOptions([])).toEqual([]);
  });
});
