<template>
  <gv-button-group>
    <slot/>
    <gv-button variant="secondary" :disabled="!historyStatus?.canUndo || isProcessing" @click="undoAction">
      Undo {{ operationName }}
    </gv-button>
    <gv-button variant="secondary" :disabled="!historyStatus?.canRedo || isProcessing" @click="redoAction">
      Redo {{ operationName }}
    </gv-button>
  </gv-button-group>
</template>

<script setup lang="ts">
import type { JsonHistoryStatus } from "@/api/history.api";
import { computed, onMounted, ref, watch } from "vue";
import { getHistoryStatus } from "@/api/history.api";
import { redo, undo } from "@/api/operator.api";
import { buildCommandJourneyUrl } from "@/command-journey-utils";
import GvButton from "../govukVue/button/GvButton.vue";
import GvButtonGroup from "../govukVue/button/GvButtonGroup.vue";

interface UndoRedoActionsProps {
  refreshCounter: number,
  baseUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
  operationName: string,
}

const props = defineProps<UndoRedoActionsProps>();

const emit = defineEmits<{
  "action-success": [],
  "action-error": [message: string],
}>();

// Shared with the parent's primary action so all buttons disable together while any request is in flight.
const isProcessing = defineModel<boolean>("processing", { default: false });

const historyStatus = ref<JsonHistoryStatus | null>(null);

const historyUrl = computed(() => buildCommandJourneyUrl(`${props.baseUrl}/history`, props.commandJourneyId));
const undoUrl = computed(() => buildCommandJourneyUrl(`${props.baseUrl}/undo`, props.commandJourneyId));
const redoUrl = computed(() => buildCommandJourneyUrl(`${props.baseUrl}/redo`, props.commandJourneyId));

async function fetchHistoryStatus() {
  try {
    historyStatus.value = await getHistoryStatus(historyUrl.value);
  } catch {
    emit("action-error", "Unable to load undo/redo status.");
  }
}

onMounted(fetchHistoryStatus);
watch(() => props.refreshCounter, fetchHistoryStatus);

async function undoAction() {
  isProcessing.value = true;
  try {
    await undo(undoUrl.value, props.csrfHeaderName, props.csrfToken);
    emit("action-success");
  } catch {
    emit("action-error", `An error occurred while attempting to undo the last ${props.operationName}. Please try again.`);
  } finally {
    isProcessing.value = false;
  }
}

async function redoAction() {
  isProcessing.value = true;
  try {
    await redo(redoUrl.value, props.csrfHeaderName, props.csrfToken);
    emit("action-success");
  } catch {
    emit("action-error", `An error occurred while attempting to redo the last ${props.operationName}. Please try again.`);
  } finally {
    isProcessing.value = false;
  }
}
</script>
