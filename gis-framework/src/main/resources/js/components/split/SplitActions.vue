<template>
  <undo-redo-actions
    v-model:processing="isProcessing"
    :refresh-counter="refreshCounter"
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
import { ref, watch } from "vue";
import { splitFeature } from "@/api/operator.api";
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

const isProcessing = ref(false);

watch(() => props.points, () => {
  if (props.autoSplit && !isProcessing.value) {
    split();
  }
});

async function split() {
  if (props.points.length < 2) {
    return;
  }

  isProcessing.value = true;
  try {
    const splitResponse = await splitFeature(
      props.splitUrl,
      props.points,
      props.commandJourneyId,
      props.csrfHeaderName,
      props.csrfToken,
    );
    if (splitResponse.outputFeatureIds.length > 0) {
      emit("action-success");
    } else {
      props.autoSplit
        ? console.warn("No split took place. Make sure your line crosses the feature boundary.")
        : emit("action-error", "No split took place. Make sure your line crosses the feature boundary.");
    }
  } catch {
    emit("action-error", "An error occurred while attempting to split the feature. Please try again.");
  } finally {
    isProcessing.value = false;
  }
}
</script>
