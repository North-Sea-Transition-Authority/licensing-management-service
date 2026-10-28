import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import type { JsonFeatureOutlineNodes } from "@/api/features.api";
import { computed, ComputedRef, Ref, ref, watch } from "vue";
import { endDepthOf, startDepthOf } from "@/depth-utils";

export interface DepthBands {
  hasDepths: ComputedRef<boolean>,
  depths: ComputedRef<Map<number, string>>,
  currentDepthLevel: Ref<number>,
  visibleFeatures: ComputedRef<Feature<Geometry>[]>,
  visibleOutlineNodes: ComputedRef<JsonFeatureOutlineNodes[]>,
}

function formatDepth(depth: number): string {
  return depth === Number.NEGATIVE_INFINITY ? "-∞" : `${depth}`;
}

export function useDepthBands(
  features: Ref<Feature<Geometry>[]>,
  outlineNodes: Ref<JsonFeatureOutlineNodes[]>,
): DepthBands {
  // Whether any feature has custom depths defined; features without them span all depths.
  const hasDepths = computed(() =>
    features.value.some(feature => feature.get("startDepth") != null || feature.get("endDepth") != null));

  const depths = computed<Map<number, string>>(() => {
    const boundaries = [...new Set(features.value.flatMap(f => [startDepthOf(f), endDepthOf(f)]))]
      .sort((a, b) => a - b);
    if (!boundaries.length) {
      return new Map([[0, "All depths"]]);
    }
    const bands = new Map<number, string>();
    for (let i = 1; i < boundaries.length; i++) {
      bands.set(boundaries[i], `${boundaries[i]}m to ${formatDepth(boundaries[i - 1])}m`);
    }
    return bands;
  });

  const currentDepthLevel = ref(0);

  // Keep the selected band across feature reloads, e.g. after a split, falling back to the shallowest if it's gone.
  watch(depths, (bands) => {
    if (!bands.has(currentDepthLevel.value)) {
      currentDepthLevel.value = Math.max(...bands.keys());
    }
  }, { immediate: true });

  const visibleFeatures = computed(() =>
    features.value.filter(feature =>
      endDepthOf(feature) < currentDepthLevel.value && currentDepthLevel.value <= startDepthOf(feature)));
  const visiblePolygonIds = computed(() => new Set(visibleFeatures.value.map(f => f.get("polygonId"))));
  const visibleOutlineNodes = computed(() =>
    outlineNodes.value
      .map(group => ({ ...group, nodes: group.nodes.filter(n => visiblePolygonIds.value.has(n.polygonId)) }))
      .filter(group => group.nodes.length > 0));

  return { hasDepths, depths, currentDepthLevel, visibleFeatures, visibleOutlineNodes };
}
