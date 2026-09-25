import { MaybeRefOrGetter, onMounted, Ref, ref, ShallowRef, shallowRef, toValue, watch } from "vue";
import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import { getFeatures } from "@/api/features.api";

export interface CommandJourneyFeatures {
  features: ShallowRef<Feature<Geometry>[]>,
  hasError: Ref<boolean>,
}

export function useCommandJourneyFeatures(
  featuresUrl: string,
  refreshCounter?: MaybeRefOrGetter<number>,
): CommandJourneyFeatures {
  const features = shallowRef<Feature<Geometry>[]>([]);
  const hasError = ref(false);

  async function reload() {
    try {
      features.value = await getFeatures(featuresUrl);
      hasError.value = false;
    } catch {
      features.value = [];
      hasError.value = true;
    }
  }

  onMounted(reload);
  if (refreshCounter !== undefined) {
    watch(() => toValue(refreshCounter), reload);
  }

  return { features, hasError };
}