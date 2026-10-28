import type { MaybeRefOrGetter, Ref } from "vue";
import type { JsonHistoryStatus } from "@/api/history.api";
import { onMounted, ref, toValue, watch } from "vue";
import { getHistoryStatus } from "@/api/history.api";
import { buildCommandJourneyUrl } from "@/command-journey-utils";

interface HistoryStatusOptions {
  onError: () => void,
}

export function useHistoryStatus(
  baseUrl: string,
  commandJourneyId: string,
  refreshCounter: MaybeRefOrGetter<number>,
  { onError }: HistoryStatusOptions,
): { historyStatus: Ref<JsonHistoryStatus | null>, isLoading: Ref<boolean> } {
  const historyUrl = buildCommandJourneyUrl(`${baseUrl}/history`, commandJourneyId);
  const historyStatus = ref<JsonHistoryStatus | null>(null);
  const isLoading = ref(true);

  async function reload() {
    isLoading.value = true;
    try {
      historyStatus.value = await getHistoryStatus(historyUrl);
    } catch {
      historyStatus.value = null;
      onError();
    } finally {
      isLoading.value = false;
    }
  }

  onMounted(reload);
  watch(() => toValue(refreshCounter), reload);

  return { historyStatus, isLoading };
}
