<template>
  <h2 ref="heading" class="govuk-heading-m govuk-!-margin-top-4" tabindex="-1">
    Add strata
  </h2>
  <details-component summary="How can I add a strata?">
    <p class="govuk-body">
      Select the subareas to split and enter the depth of the new stratigraphic boundary. Each selected subarea is
      split into the part above the depth and the part below it.
    </p>
    <p class="govuk-body">
      A subarea can only be split at a depth it exists at, and not at a depth that is already one of its boundaries.
    </p>
  </details-component>
  <gv-checkboxes
    v-model="selectedFeatureIds"
    legend="Select the subareas to add strata"
    legend-class="govuk-fieldset__legend--s"
    :error-message="featuresError"
  >
    <gv-checkbox
      v-for="(feature, index) in featureOptions"
      :id="index === 0 ? firstCheckboxId : undefined"
      :key="feature.featureId"
      :value="feature.featureId"
      :label="feature.featureName"
      :hint="formatDepthRanges(feature.depthRanges)"
    />
  </gv-checkboxes>
  <gv-input
    :id="depthInputId"
    v-model="depthInput"
    label="What depth is the new stratigraphic boundary at?"
    label-class="govuk-label--s"
    hint="Enter a whole number, in metres, below 0. For example -2000"
    suffix="m"
    :spellcheck="false"
    class="govuk-input--width-10"
    :error-message="depthError"
  />
  <gv-button-group>
    <gv-button text="Add strata" :disabled="isProcessing" @click="onSubmit"/>
    <gv-button variant="secondary" text="Back to action select" :disabled="isProcessing" @click="emit('back')"/>
  </gv-button-group>
</template>

<script setup lang="ts">
import type { FeatureOption } from "@/feature-option-utils";
import type { ValidationError } from "@/validation-error";
import { ref, useTemplateRef } from "vue";
import GvButton from "@/components/govukVue/button/GvButton.vue";
import GvButtonGroup from "@/components/govukVue/button/GvButtonGroup.vue";
import GvCheckbox from "@/components/govukVue/checkboxes/GvCheckbox.vue";
import GvCheckboxes from "@/components/govukVue/checkboxes/GvCheckboxes.vue";
import DetailsComponent from "@/components/govukVue/details/GvDetails.vue";
import GvInput from "@/components/govukVue/input/GvInput.vue";
import { createUid } from "@/components/govukVue/util/createUid";
import { useStrataSplitFeature } from "@/composables/useStrataSplitFeature";
import { canSplitAt, formatDepthRanges } from "@/depth-utils";

interface StrataSplitPanelProps {
  baseUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
  featureOptions: FeatureOption[],
}

const props = defineProps<StrataSplitPanelProps>();

const emit = defineEmits<{
  "success": [],
  "error": [message: string],
  "back": [],
  "validation-error": [errors: ValidationError[]],
}>();

const selectedFeatureIds = defineModel<string[]>("selectedFeatureIds", { required: true });

const depthInputId = createUid("strata-depth");
const firstCheckboxId = createUid("strata-feature");

const depthInput = ref("");
const depthError = ref<string>();
const featuresError = ref<string>();

const { isProcessing, strataSplit } = useStrataSplitFeature(
  () => ({
    strataSplitUrl: `${props.baseUrl}/strata-split`,
    commandJourneyId: props.commandJourneyId,
    csrfHeaderName: props.csrfHeaderName,
    csrfToken: props.csrfToken,
  }),
  {
    onSuccess: () => emit("success"),
    onError: message => emit("error", message),
  },
);

function validateDepth(): number | undefined {
  const trimmedDepth = depthInput.value.trim();
  if (!trimmedDepth) {
    depthError.value = "Enter the depth of the new stratigraphic boundary";
  } else if (!/^-?\d+$/.test(trimmedDepth)) {
    depthError.value = "Depth must be a whole number";
  } else if (Number(trimmedDepth) >= 0) {
    depthError.value = "Depth must be below 0";
  } else {
    depthError.value = undefined;
    return Number(trimmedDepth);
  }
  return undefined;
}

function validateFeatures(depth: number | undefined) {
  if (!selectedFeatureIds.value.length) {
    featuresError.value = "Select at least one subarea to split";
    return;
  }
  if (depth === undefined) {
    featuresError.value = undefined;
    return;
  }
  const unsplittableFeatureNames = props.featureOptions
    .filter(option => selectedFeatureIds.value.includes(option.featureId))
    .filter(option => !canSplitAt(option.depthRanges, depth))
    .map(option => option.featureName);
  featuresError.value = unsplittableFeatureNames.length
    ? `${unsplittableFeatureNames.join(", ")} cannot be split at ${depth} as it is outside the subarea or is already a boundary`
    : undefined;
}

async function onSubmit() {
  const depth = validateDepth();
  validateFeatures(depth);

  const errors: ValidationError[] = [];
  if (featuresError.value) {
    errors.push({ message: featuresError.value, fieldId: firstCheckboxId });
  }
  if (depthError.value) {
    errors.push({ message: depthError.value, fieldId: depthInputId });
  }
  emit("validation-error", errors);

  if (errors.length || depth === undefined) {
    return;
  }
  await strataSplit(selectedFeatureIds.value, depth);
}

const heading = useTemplateRef<HTMLHeadingElement>("heading");

defineExpose({ focus: () => heading.value?.focus() });
</script>
