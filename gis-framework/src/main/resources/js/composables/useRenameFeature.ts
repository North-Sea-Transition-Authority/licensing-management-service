import type { MaybeRefOrGetter } from "vue";
import type { JsonFeatureRename } from "@/api/operator.api";
import { ref, toValue } from "vue";
import { renameFeatures } from "@/api/operator.api";

export interface RenameRequest {
  renameUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
}

interface RenameFeatureOptions {
  onSuccess: () => void,
  onError: (message: string) => void,
}

export function useRenameFeature(
  request: MaybeRefOrGetter<RenameRequest>,
  { onSuccess, onError }: RenameFeatureOptions,
) {
  const isProcessing = ref(false);

  async function rename(featureRenames: JsonFeatureRename[]) {
    const { renameUrl, commandJourneyId, csrfHeaderName, csrfToken } = toValue(request);
    isProcessing.value = true;
    try {
      await renameFeatures(renameUrl, featureRenames, commandJourneyId, csrfHeaderName, csrfToken);
      onSuccess();
    } catch {
      onError("An error occurred while attempting to rename the subareas. Please try again.");
    } finally {
      isProcessing.value = false;
    }
  }

  return { isProcessing, rename };
}