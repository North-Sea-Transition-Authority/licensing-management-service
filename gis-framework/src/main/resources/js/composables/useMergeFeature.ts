import type { MaybeRefOrGetter } from "vue";
import { ref, toValue } from "vue";
import { mergeFeatures } from "@/api/operator.api";

export interface MergeRequest {
  mergeUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
}

interface MergeFeatureOptions {
  onSuccess: () => void,
  onError: (message: string) => void,
}

export function useMergeFeature(
  request: MaybeRefOrGetter<MergeRequest>,
  featureIds: MaybeRefOrGetter<string[]>,
  { onSuccess, onError }: MergeFeatureOptions,
) {
  const isProcessing = ref(false);

  async function merge() {
    const currentFeatureIds = toValue(featureIds);
    if (currentFeatureIds.length < 2) {
      return;
    }

    const { mergeUrl, commandJourneyId, csrfHeaderName, csrfToken } = toValue(request);
    isProcessing.value = true;
    try {
      await mergeFeatures(mergeUrl, currentFeatureIds, commandJourneyId, csrfHeaderName, csrfToken);
      onSuccess();
    } catch {
      onError("An error occurred while attempting to merge the features. Please try again.");
    } finally {
      isProcessing.value = false;
    }
  }

  return { isProcessing, merge };
}