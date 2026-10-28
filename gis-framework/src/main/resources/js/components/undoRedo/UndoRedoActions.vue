<template>
  <gv-button-group>
    <slot/>
    <gv-button variant="secondary" :disabled="!historyStatus?.undoTransformationType || isProcessing || disabled" @click="undoAction">
      Undo {{ undoLabel }}
    </gv-button>
    <gv-button variant="secondary" :disabled="!historyStatus?.redoTransformationType || isProcessing || disabled" @click="redoAction">
      Redo {{ redoLabel }}
    </gv-button>
  </gv-button-group>
</template>

<script setup lang="ts">
import type { JsonHistoryStatus } from "@/api/history.api";
import type { TransformationType } from "@/transformation-type";
import { computed } from "vue";
import { redo, undo } from "@/api/operator.api";
import { buildCommandJourneyUrl } from "@/command-journey-utils";
import { getTransformationTypeLabel } from "@/transformation-type";
import GvButton from "../govukVue/button/GvButton.vue";
import GvButtonGroup from "../govukVue/button/GvButtonGroup.vue";

interface UndoRedoActionsProps {
  historyStatus: JsonHistoryStatus | null,
  baseUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
  operationName?: string,
  disabled?: boolean,
}

const props = withDefaults(defineProps<UndoRedoActionsProps>(), {
  disabled: false,
});

const emit = defineEmits<{
  "action-success": [],
  "action-error": [message: string],
}>();

// Shared with the parent's primary action so all buttons disable together while any request is in flight.
const isProcessing = defineModel<boolean>("processing", { default: false });

const undoUrl = computed(() => buildCommandJourneyUrl(`${props.baseUrl}/undo`, props.commandJourneyId));
const redoUrl = computed(() => buildCommandJourneyUrl(`${props.baseUrl}/redo`, props.commandJourneyId));

function labelFor(type: TransformationType | null | undefined) {
  return props.operationName ?? (type ? getTransformationTypeLabel(type).toLowerCase() : undefined);
}

const undoLabel = computed(() => labelFor(props.historyStatus?.undoTransformationType));
const redoLabel = computed(() => labelFor(props.historyStatus?.redoTransformationType));

async function undoAction() {
  isProcessing.value = true;
  try {
    await undo(undoUrl.value, props.csrfHeaderName, props.csrfToken);
    emit("action-success");
  } catch {
    emit("action-error", `An error occurred while attempting to undo the last ${undoLabel.value ?? "action"}. Please try again.`);
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
    emit("action-error", `An error occurred while attempting to redo the last ${redoLabel.value ?? "action"}. Please try again.`);
  } finally {
    isProcessing.value = false;
  }
}
</script>
