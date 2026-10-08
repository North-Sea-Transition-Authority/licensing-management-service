import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import type { JsonFeatureOutlineNodes } from "@/api/features.api";
import { computed, ComputedRef, Ref, ref, watch } from "vue";

export interface DepthBands {
  hasDepths: ComputedRef<boolean>,
  depths: ComputedRef<Map<number, string>>,
  currentDepthLevel: Ref<number>,
  visibleFeatures: ComputedRef<Feature<Geometry>[]>,
  visibleOutlineNodes: ComputedRef<JsonFeatureOutlineNodes[]>,
}

function startDepthOf(feature: Feature<Geometry>): number {
  return Number(feature.get("startDepth") ?? 0);
}

function endDepthOf(feature: Feature<Geometry>): number {
  return Number(feature.get("endDepth") ?? Number.NEGATIVE_INFINITY);
}

export function useDepthBands(
  features: Ref<Feature<Geometry>[]>,
  outlineNodes: Ref<JsonFeatureOutlineNodes[]>,
): DepthBands {
  // Whether any feature has custom depths defined; features without them span all depths.
  const hasDepths = computed(() =>
    features.value.some(feature => feature.get("startDepth") != null || feature.get("endDepth") != null));

  const depths = computed<Map<number, string>>(() => {
    const starts = [...new Set(features.value.map(startDepthOf))].sort((a, b) => a - b);
    if (!starts.length) {
      return new Map([[0, "All depths"]]);
    }
    const bands = new Map<number, string>([[starts[0], `${starts[0]} to -infinity`]]);
    for (let i = 1; i < starts.length; i++) {
      bands.set(starts[i], `${starts[i]} to ${starts[i - 1]}`);
    }
    return bands;
  });

  const currentDepthLevel = ref(0);

  watch(depths, (band) => {
    currentDepthLevel.value = Math.max(...band.keys());
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
