import { MaybeRefOrGetter, onMounted, Ref, ref, ShallowRef, shallowRef, toValue, watch } from "vue";
import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import { getFeatures, getOutlineNodes, JsonFeatureOutlineNodes } from "@/api/features.api";

export interface CommandJourneyFeatures {
  features: ShallowRef<Feature<Geometry>[]>,
  outlineNodes: ShallowRef<JsonFeatureOutlineNodes[]>,
  hasError: Ref<boolean>,
}

export function useCommandJourneyFeatures(
  featuresUrl: string,
  outlineNodesUrl: string,
  refreshCounter?: MaybeRefOrGetter<number>,
): CommandJourneyFeatures {
  const features = shallowRef<Feature<Geometry>[]>([]);
  const outlineNodes = shallowRef<JsonFeatureOutlineNodes[]>([]);
  const hasError = ref(false);

  async function reload() {
    try {
      // Fetch in parallel, and only assign once both succeed so features and nodes never mismatch
      const [loadedFeatures, loadedNodes] = await Promise.all([
        getFeatures(featuresUrl),
        getOutlineNodes(outlineNodesUrl),
      ]);
      features.value = loadedFeatures;
      outlineNodes.value = loadedNodes;
      hasError.value = false;
    } catch {
      features.value = [];
      outlineNodes.value = [];
      hasError.value = true;
    }
  }

  onMounted(reload);
  if (refreshCounter !== undefined) {
    watch(() => toValue(refreshCounter), reload);
  }

  return { features, outlineNodes, hasError };
}
