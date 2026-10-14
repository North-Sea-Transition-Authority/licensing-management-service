import type { MaybeRefOrGetter } from "vue";
import type { LinePoint } from "@/grid-utils";
import { ref, toValue, watch } from "vue";
import { splitFeature } from "@/api/operator.api";

export interface SplitRequest {
  splitUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
  depth?: number,
}

interface SplitFeatureOptions {
  autoSplit?: MaybeRefOrGetter<boolean>,
  onSuccess: () => void,
  onError: (message: string) => void,
}

export function useSplitFeature(
  request: MaybeRefOrGetter<SplitRequest>,
  points: MaybeRefOrGetter<LinePoint[]>,
  { autoSplit = false, onSuccess, onError }: SplitFeatureOptions,
) {
  const isProcessing = ref(false);

  watch(() => toValue(points), () => {
    if (toValue(autoSplit) && !isProcessing.value) {
      void split();
    }
  });

  async function split() {
    const currentPoints = toValue(points);
    if (currentPoints.length < 2) {
      return;
    }

    const { splitUrl, commandJourneyId, csrfHeaderName, csrfToken, depth } = toValue(request);
    isProcessing.value = true;
    try {
      const splitResponse = await splitFeature(
        splitUrl,
        currentPoints,
        commandJourneyId,
        csrfHeaderName,
        csrfToken,
        depth,
      );
      if (splitResponse.outputFeatureIds.length > 0) {
        onSuccess();
      } else {
        toValue(autoSplit)
          ? console.warn("No split took place. Make sure your line crosses the feature boundary.")
          : onError("No split took place. Make sure your line crosses the feature boundary.");
      }
    } catch {
      onError("An error occurred while attempting to split the feature. Please try again.");
    } finally {
      isProcessing.value = false;
    }
  }

  return { isProcessing, split };
}
