<template>
  <gv-button-group>
    <gv-button v-if="showSplitButton" :disabled="isProcessing || points.length < 2" @click="split">
      Split
    </gv-button>
    <gv-button variant="secondary" :disabled="!historyStatus?.canUndo || isProcessing" @click="undo">
      Undo split
    </gv-button>
    <gv-button variant="secondary" :disabled="!historyStatus?.canRedo || isProcessing" @click="redo">
      Redo split
    </gv-button>
  </gv-button-group>
</template>

<script setup lang="ts">
import type { JsonSplitHistoryStatus } from "../../api/split-history.api";
import type { LinePoint } from "../../grid-utils";
import { onMounted, ref, watch } from "vue";
import { getSplitHistoryStatus } from "../../api/split-history.api";
import { redoSplit, splitFeature, undoSplit } from "../../api/split.api";
import GvButton from "../govukVue/button/GvButton.vue";
import GvButtonGroup from "../govukVue/button/GvButtonGroup.vue";

interface SplitActionsProps {
  refreshCounter: number,
  historyUrl: string,
  undoUrl: string,
  redoUrl: string,
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

const historyStatus = ref<JsonSplitHistoryStatus | null>(null);
const isProcessing = ref(false);

async function fetchHistoryStatus() {
  try {
    historyStatus.value = await getSplitHistoryStatus(props.historyUrl);
  } catch {
    emit("action-error", "Unable to load undo/redo status.");
  }
}

onMounted(fetchHistoryStatus);
watch(() => props.refreshCounter, fetchHistoryStatus);

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

async function undo() {
  isProcessing.value = true;
  try {
    await undoSplit(props.undoUrl, props.csrfHeaderName, props.csrfToken);
    emit("action-success");
  } catch {
    emit("action-error", "An error occurred while attempting to undo the last split. Please try again.");
  } finally {
    isProcessing.value = false;
  }
}

async function redo() {
  isProcessing.value = true;
  try {
    await redoSplit(props.redoUrl, props.csrfHeaderName, props.csrfToken);
    emit("action-success");
  } catch {
    emit("action-error", "An error occurred while attempting to redo the last split. Please try again.");
  } finally {
    isProcessing.value = false;
  }
}
</script>
