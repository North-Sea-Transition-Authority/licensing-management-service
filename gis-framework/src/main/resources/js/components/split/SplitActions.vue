<template>
  <undo-redo-actions
    v-model:processing="isProcessing"
    :history-status="historyStatus"
    :base-url="baseUrl"
    :command-journey-id="commandJourneyId"
    :csrf-header-name="csrfHeaderName"
    :csrf-token="csrfToken"
    operation-name="split"
    @action-success="emit('action-success')"
    @action-error="emit('action-error', $event)"
  >
    <gv-button v-if="showSplitButton" :disabled="isProcessing || points.length < 2" @click="split">
      Split
    </gv-button>
  </undo-redo-actions>
</template>

<script setup lang="ts">
import type { LinePoint } from "@/grid-utils";
import { useHistoryStatus } from "@/composables/useHistoryStatus";
import { useSplitFeature } from "@/composables/useSplitFeature";
import GvButton from "../govukVue/button/GvButton.vue";
import UndoRedoActions from "../undoRedo/UndoRedoActions.vue";

interface SplitActionsProps {
  refreshCounter: number,
  baseUrl: string,
  csrfHeaderName: string,
  csrfToken: string,
  points: LinePoint[],
  splitUrl: string,
  commandJourneyId: string,
  showSplitButton?: boolean,
  autoSplit?: boolean,
}

const props = defineProps<SplitActionsProps>();

const emit = defineEmits<{
  "action-success": [],
  "action-error": [message: string],
}>();

const { historyStatus } = useHistoryStatus(props.baseUrl, props.commandJourneyId, () => props.refreshCounter, {
  onError: () => emit("action-error", "Unable to load undo/redo status."),
});

const { isProcessing, split } = useSplitFeature(() => props, () => props.points, {
  autoSplit: () => props.autoSplit,
  onSuccess: () => emit("action-success"),
  onError: message => emit("action-error", message),
});
</script>
