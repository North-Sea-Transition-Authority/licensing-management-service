import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import type { DepthRange } from "@/depth-utils";
import { depthRangesOf } from "@/depth-utils";

export interface FeatureOption {
  featureId: string,
  featureName: string,
  depthRanges: DepthRange[],
}

/**
 * Features are returned one per polygon, so collapse them to one option per feature, sorted by name.
 */
export function toSortedFeatureOptions(features: Feature<Geometry>[]): FeatureOption[] {
  const options = new Map<string, FeatureOption>(features.map(feature => [
    feature.get("featureId"),
    { featureId: feature.get("featureId"),
      featureName: feature.get("featureName"),
      depthRanges: depthRangesOf(features, feature.get("featureId")),
    },
  ]));
  return [...options.values()]
    .sort((a, b) =>
      a.featureName.localeCompare(b.featureName, undefined, { numeric: true })
    );
}
