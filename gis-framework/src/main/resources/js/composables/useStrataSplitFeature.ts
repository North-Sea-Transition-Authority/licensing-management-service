import type { MaybeRefOrGetter } from "vue";
import { ref, toValue } from "vue";
import { strataSplitFeatures } from "@/api/operator.api";

export interface StrataSplitRequest {
  strataSplitUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
}

interface StrataSplitFeatureOptions {
  onSuccess: () => void,
  onError: (message: string) => void,
}

export function useStrataSplitFeature(
  request: MaybeRefOrGetter<StrataSplitRequest>,
  { onSuccess, onError }: StrataSplitFeatureOptions,
) {
  const isProcessing = ref(false);

  async function strataSplit(featureIds: string[], depth: number) {
    const { strataSplitUrl, commandJourneyId, csrfHeaderName, csrfToken } = toValue(request);
    isProcessing.value = true;
    try {
      await strataSplitFeatures(strataSplitUrl, featureIds, depth, commandJourneyId, csrfHeaderName, csrfToken);
      onSuccess();
    } catch {
      onError("An error occurred while attempting to stratigraphically split the features. Please try again.");
    } finally {
      isProcessing.value = false;
    }
  }

  return { isProcessing, strataSplit };
}
