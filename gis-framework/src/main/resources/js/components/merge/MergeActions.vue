<template>
  <undo-redo-actions
    v-model:processing="isProcessing"
    :refresh-counter="refreshCounter"
    :base-url="baseUrl"
    :command-journey-id="commandJourneyId"
    :csrf-header-name="csrfHeaderName"
    :csrf-token="csrfToken"
    operation-name="merge"
    @action-success="emit('action-success')"
    @action-error="emit('action-error', $event)"
  >
    <gv-button :disabled="featureIds.length < 2 || isProcessing" @click="merge">
      Merge
    </gv-button>
  </undo-redo-actions>
</template>

<script setup lang="ts">
import { useMergeFeature } from "@/composables/useMergeFeature";
import GvButton from "../govukVue/button/GvButton.vue";
import UndoRedoActions from "../undoRedo/UndoRedoActions.vue";

interface MergeActionsProps {
  refreshCounter: number,
  baseUrl: string,
  mergeUrl: string,
  commandJourneyId: string,
  featureIds: string[],
  csrfHeaderName: string,
  csrfToken: string,
}

const props = defineProps<MergeActionsProps>();

const emit = defineEmits<{
  "action-success": [],
  "action-error": [message: string],
}>();

const { isProcessing, merge } = useMergeFeature(() => props, () => props.featureIds, {
  onSuccess: () => emit("action-success"),
  onError: message => emit("action-error", message),
});
</script>
