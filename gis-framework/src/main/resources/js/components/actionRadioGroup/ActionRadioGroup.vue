<template>
  <gv-radios
    v-model="selectedAction"
    :legend="hasActions ? 'What action would you like to perform next?' : 'What action would you like to perform first?'"
    legend-class="govuk-fieldset__legend--m"
    :hint="hasActions ? undefined : 'You can perform multiple actions before finalising.'"
    :error-message="validationError"
    form-group-class="govuk-!-margin-top-4"
  >
    <gv-radio
      v-for="(action, index) in visibleActions"
      :id="index === 0 ? firstRadioId : undefined"
      :key="action"
      :value="action"
      :label="mapActionLabels[action]"
    />
  </gv-radios>
  <gv-button variant="secondary" text="Continue" @click="onContinue"/>
</template>

<script setup lang="ts">
import type { MapAction, MapActionValidationError } from "@/map-action";
import { computed, ref, watch } from "vue";
import { getAvailableMapActions, mapActionLabels } from "@/map-action";
import GvButton from "../govukVue/button/GvButton.vue";
import GvRadio from "../govukVue/radios/GvRadio.vue";
import GvRadios from "../govukVue/radios/GvRadios.vue";
import { createUid } from "../govukVue/util/createUid";

interface DepthActionSelectProps {
  hasActions: boolean,
  canMerge: boolean,
  canAddStrata: boolean,
}

const props = defineProps<DepthActionSelectProps>();

const emit = defineEmits<{
  "select": [action: MapAction],
  "validation-error": [error: MapActionValidationError | null],
}>();

const visibleActions = computed(() => getAvailableMapActions(props.canMerge, props.canAddStrata));

// The page's error summary links to the first radio.
const firstRadioId = createUid("depth-action");

const selectedAction = ref<string | number | boolean | undefined>(undefined);
const validationError = ref<string>();

// Clear the selection if its option is no longer available, e.g. after an undo leaves a single feature.
watch(visibleActions, (actions) => {
  if (!actions.includes(selectedAction.value as MapAction)) {
    selectedAction.value = undefined;
  }
});

function onContinue() {
  const action = selectedAction.value as MapAction | undefined;

  if (action === undefined) {
    validationError.value = "Select what action you would like to perform";
    emit("validation-error", { message: validationError.value, fieldId: firstRadioId });
    return;
  }

  validationError.value = undefined;
  emit("validation-error", null);
  emit("select", action);
}

function focus() {
  document.getElementById(firstRadioId)?.focus();
}

defineExpose({ focus });
</script>
