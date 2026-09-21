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
import { ref } from "vue";
import { mergeFeatures } from "@/api/operator.api";
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

const isProcessing = ref(false);

async function merge() {
  if (props.featureIds.length < 2) {
    return;
  }

  isProcessing.value = true;
  try {
    await mergeFeatures(
      props.mergeUrl,
      props.featureIds,
      props.commandJourneyId,
      props.csrfHeaderName,
      props.csrfToken,
    );
    emit("action-success");
  } catch {
    emit("action-error", "An error occurred while attempting to merge the features. Please try again.");
  } finally {
    isProcessing.value = false;
  }
}
</script>
