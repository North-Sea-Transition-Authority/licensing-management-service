import type { JsonFeatureOutlineNodes, JsonOutlineNode } from "@/api/features.api";
import Feature from "ol/Feature";
import { describe, expect, it } from "vitest";
import { nextTick, shallowRef } from "vue";
import { useDepthBands } from "@/composables/useDepthBands";

function feature(polygonId: string, startDepth: number, endDepth: number): Feature {
  const created = new Feature();
  created.set("polygonId", polygonId);
  created.set("startDepth", startDepth);
  created.set("endDepth", endDepth);
  return created;
}

function node(polygonId: string): JsonOutlineNode {
  return { polygonId, lineId: `${polygonId}-line`, ringNumber: 1, displayOrder: 1, x: 1, y: 2, mapText: "(1)" };
}

const shallowFeature = feature("shallow-polygon", 0, -100);
const deepFeature = feature("deep-polygon", -100, -200);

const outlineNodes: JsonFeatureOutlineNodes[] = [
  { featureId: "feature-1", nodes: [node("shallow-polygon"), node("deep-polygon")] },
  { featureId: "feature-2", nodes: [node("deep-polygon")] },
];

describe("useDepthBands", () => {
  it.each([
    { description: "a feature has depths", features: [shallowFeature, new Feature()], expected: true },
    { description: "no feature has depths", features: [new Feature(), new Feature()], expected: false },
  ])("reports hasDepths as $expected when $description", ({ features, expected }) => {
    const { hasDepths } = useDepthBands(shallowRef(features), shallowRef([]));

    expect(hasDepths.value).toBe(expected);
  });

  it("builds a depth band for each distinct start depth", () => {
    const { depths } = useDepthBands(shallowRef([shallowFeature, deepFeature, feature("other", 0, -50)]), shallowRef([]));

    expect(depths.value).toEqual(new Map([
      [-100, "-100 to -infinity"],
      [0, "0 to -100"],
    ]));
  });

  it("builds a single all depths band when there are no features", () => {
    const { depths } = useDepthBands(shallowRef([]), shallowRef([]));

    expect(depths.value).toEqual(new Map([[0, "All depths"]]));
  });

  it("selects the shallowest band when the features change", async () => {
    const features = shallowRef<Feature[]>([]);
    const { currentDepthLevel } = useDepthBands(features, shallowRef([]));
    currentDepthLevel.value = -100;

    features.value = [shallowFeature, deepFeature];
    await nextTick();

    expect(currentDepthLevel.value).toBe(0);
  });

  it.each([
    { depth: 0, expectedFeatures: [shallowFeature] },
    { depth: -100, expectedFeatures: [deepFeature] },
  ])("shows only the features present at depth $depth", ({ depth, expectedFeatures }) => {
    const { currentDepthLevel, visibleFeatures } = useDepthBands(shallowRef([shallowFeature, deepFeature]), shallowRef([]));

    currentDepthLevel.value = depth;

    expect(visibleFeatures.value).toEqual(expectedFeatures);
  });

  it("shows only the outline nodes of visible polygons and drops features left without nodes", () => {
    const { currentDepthLevel, visibleOutlineNodes } = useDepthBands(shallowRef([shallowFeature, deepFeature]), shallowRef(outlineNodes));

    currentDepthLevel.value = 0;

    expect(visibleOutlineNodes.value).toEqual([
      { featureId: "feature-1", nodes: [node("shallow-polygon")] },
    ]);
  });
});
