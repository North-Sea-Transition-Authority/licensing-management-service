<template>
  <gv-button-group>
    <gv-button variant="secondary" :disabled="!historyStatus?.canUndo" @click="undo">
      Undo split
    </gv-button>
  </gv-button-group>
</template>

<script setup lang="ts">
import type { JsonSplitHistoryStatus } from "../../api/split-history.api";
import { onMounted, ref, watch } from "vue";
import { getSplitHistoryStatus } from "../../api/split-history.api";
import { undoSplit } from "../../api/split.api";
import GvButton from "../govukVue/GvButton.vue";
import GvButtonGroup from "../govukVue/GvButtonGroup.vue";

interface SplitActionsProps {
  refreshCounter: number,
  historyUrl: string,
  undoUrl: string,
  csrfHeaderName: string,
  csrfToken: string,
}

const props = defineProps<SplitActionsProps>();

const emit = defineEmits<{
  "action-success": [],
  "action-error": [message: string],
}>();

const historyStatus = ref<JsonSplitHistoryStatus | null>(null);

async function fetchHistoryStatus() {
  try {
    historyStatus.value = await getSplitHistoryStatus(props.historyUrl);
  } catch {
    emit("action-error", "Unable to load undo status.");
  }
}

onMounted(fetchHistoryStatus);
watch(() => props.refreshCounter, fetchHistoryStatus);

async function undo() {
  try {
    await undoSplit(props.undoUrl, props.csrfHeaderName, props.csrfToken);
    emit("action-success");
  } catch {
    emit("action-error", "An error occurred while attempting to undo the last split. Please try again.");
  }
}
</script>
