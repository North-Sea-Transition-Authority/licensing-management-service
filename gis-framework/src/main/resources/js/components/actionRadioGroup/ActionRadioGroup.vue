<template>
  <gv-radios
    v-model="selectedAction"
    :legend="hasActions ? 'What action would you like to perform next?' : 'What action would you like to perform first?'"
    legend-class="govuk-fieldset__legend--m"
    :hint="hasActions ? undefined : 'You can perform multiple actions before finalising.'"
    :error-message="errorMessage"
    form-group-class="govuk-!-margin-top-4 govuk-!-margin-bottom-6"
  >
    <gv-radio
      v-for="(action, index) in visibleActions"
      :id="index === 0 ? firstRadioId : undefined"
      :key="action"
      :value="action"
      :label="getTransformationTypeLabel(action)"
    />
  </gv-radios>
  <gv-button variant="secondary" text="Continue" :disabled="processing" @click="onContinue"/>
</template>

<script setup lang="ts">
import type { TransformationType } from "@/transformation-type";
import type { ValidationError } from "@/validation-error";
import { computed, ref, watch } from "vue";
import { getAvailableTransformationTypes, getTransformationTypeLabel } from "@/transformation-type";
import GvButton from "../govukVue/button/GvButton.vue";
import GvRadio from "../govukVue/radios/GvRadio.vue";
import GvRadios from "../govukVue/radios/GvRadios.vue";
import { createUid } from "../govukVue/util/createUid";

interface ActionRadioGroupProps {
  hasActions: boolean,
  canMerge: boolean,
  canAddStrata: boolean,
  processing?: boolean,
  errorMessage?: string,
}

const props = withDefaults(defineProps<ActionRadioGroupProps>(), {
  processing: false,
});

const emit = defineEmits<{
  "select": [action: TransformationType],
  "validation-error": [error: ValidationError | null],
}>();

const visibleActions = computed(() => getAvailableTransformationTypes(props.canMerge, props.canAddStrata));

// The page's error summary links to the first radio.
const firstRadioId = createUid("depth-action");

const selectedAction = ref<string | number | boolean | undefined>(undefined);

// Clear the selection if its option is no longer available, e.g. after an undo leaves a single feature.
watch(visibleActions, (actions) => {
  if (!actions.includes(selectedAction.value as TransformationType)) {
    selectedAction.value = undefined;
  }
});

function onContinue() {
  const action = selectedAction.value as TransformationType | undefined;

  if (action === undefined) {
    emit("validation-error", { message: "Select what action you would like to perform", fieldId: firstRadioId });
    return;
  }

  emit("validation-error", null);
  emit("select", action);
}

function focus() {
  document.getElementById(firstRadioId)?.focus();
}

defineExpose({ focus });
</script>
