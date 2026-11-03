<template>
  <h2 ref="heading" class="govuk-heading-m govuk-!-margin-top-4" tabindex="-1">
    Rename subareas
  </h2>
  <details-component summary="How can I rename subareas?">
    <p class="govuk-body">
      Select the subareas to rename and enter a new subarea name and map display name for each one.
    </p>
    <p class="govuk-body">
      Subarea names and map display names must be unique within the block. A map display name must be
      {{ MAP_DISPLAY_NAME_MAX_LENGTH }} characters or fewer.
    </p>
  </details-component>
  <gv-checkboxes
    v-model="selectedFeatureIds"
    legend="Select the subareas to rename"
    legend-class="govuk-fieldset__legend--s"
    :error-message="featuresError"
  >
    <gv-checkbox
      v-for="feature in featureOptions"
      :id="checkboxId(feature.featureId)"
      :key="feature.featureId"
      :value="feature.featureId"
      :label="feature.featureName"
      :hint="formatDepthRanges(feature.depthRanges)"
      :disabled="isProcessing"
    >
      <template #conditional>
        <gv-input
          :id="inputId(feature.featureId, 'name')"
          v-model="renameInputs[feature.featureId].name"
          label-class="govuk-label--s"
          :disabled="isProcessing"
          :spellcheck="false"
          class="govuk-input--width-20"
          :error-message="renameErrors[feature.featureId]?.name"
        >
          <template #label>
            Subarea name <span class="govuk-visually-hidden">for {{ feature.featureName }}</span>
          </template>
        </gv-input>
        <gv-input
          :id="inputId(feature.featureId, 'featureName')"
          v-model="renameInputs[feature.featureId].featureName"
          label-class="govuk-label--s"
          :hint="`Must be ${MAP_DISPLAY_NAME_MAX_LENGTH} characters or fewer`"
          :disabled="isProcessing"
          :spellcheck="false"
          class="govuk-input--width-20"
          :error-message="renameErrors[feature.featureId]?.featureName"
        >
          <template #label>
            Map display name <span class="govuk-visually-hidden">for {{ feature.featureName }}</span>
          </template>
        </gv-input>
      </template>
    </gv-checkbox>
  </gv-checkboxes>
  <gv-button-group>
    <gv-button text="Rename" :disabled="isProcessing" @click="onSubmit"/>
    <gv-button variant="secondary" text="Back to action select" :disabled="isProcessing" @click="emit('back')"/>
  </gv-button-group>
</template>

<script setup lang="ts">
import type { FeatureOption } from "@/feature-option-utils";
import type { ValidationError } from "@/validation-error";
import { ref, useTemplateRef, watch } from "vue";
import GvButton from "@/components/govukVue/button/GvButton.vue";
import GvButtonGroup from "@/components/govukVue/button/GvButtonGroup.vue";
import GvCheckbox from "@/components/govukVue/checkboxes/GvCheckbox.vue";
import GvCheckboxes from "@/components/govukVue/checkboxes/GvCheckboxes.vue";
import DetailsComponent from "@/components/govukVue/details/GvDetails.vue";
import GvInput from "@/components/govukVue/input/GvInput.vue";
import { createUid } from "@/components/govukVue/util/createUid";
import { useRenameFeature } from "@/composables/useRenameFeature";
import { formatDepthRanges } from "@/depth-utils";

interface RenamePanelProps {
  baseUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
  featureOptions: FeatureOption[],
}

interface RenameInput {
  name: string,
  featureName: string,
}

type RenameField = keyof RenameInput;

const props = defineProps<RenamePanelProps>();

const emit = defineEmits<{
  "success": [],
  "error": [message: string],
  "back": [],
  "validation-error": [errors: ValidationError[]],
}>();

const selectedFeatureIds = defineModel<string[]>("selectedFeatureIds", { required: true });

const MAP_DISPLAY_NAME_MAX_LENGTH = 15;

const inputIdPrefix = createUid("rename");

// Kept for unchecked subareas too, so unticking and re-ticking a box keeps what was typed
const renameInputs = ref<Record<string, RenameInput>>({});
const renameErrors = ref<Record<string, Partial<Record<RenameField, string>>>>({});
const featuresError = ref<string>();

const { isProcessing, rename } = useRenameFeature(
  () => ({
    renameUrl: `${props.baseUrl}/rename`,
    commandJourneyId: props.commandJourneyId,
    csrfHeaderName: props.csrfHeaderName,
    csrfToken: props.csrfToken,
  }),
  {
    onSuccess: () => emit("success"),
    onError: message => emit("error", message),
  },
);

watch(() => props.featureOptions, (options) => {
  options
    .filter(option => !renameInputs.value[option.featureId])
    .forEach((option) => {
      renameInputs.value[option.featureId] = { name: option.name, featureName: option.featureName };
    });
}, { immediate: true });

function checkboxId(featureId: string) {
  return `${inputIdPrefix}-${featureId}`;
}

function inputId(featureId: string, field: RenameField) {
  return `${inputIdPrefix}-${featureId}-${field}`;
}

function normalise(value: string) {
  return value.trim().toLowerCase();
}

function isSelected(option: FeatureOption) {
  return selectedFeatureIds.value.includes(option.featureId);
}

/**
 * Groups the subareas sharing each name once the rename is applied: unselected subareas keep their current names,
 * selected ones take their new names.
 */
function findDuplicates(field: RenameField): Map<string, FeatureOption[]> {
  const optionsByName = new Map<string, FeatureOption[]>();
  props.featureOptions.forEach((option) => {
    const value = normalise(isSelected(option) ? renameInputs.value[option.featureId][field] : option[field]);
    if (value) {
      optionsByName.set(value, [...(optionsByName.get(value) ?? []), option]);
    }
  });
  return new Map([...optionsByName].filter(([, options]) => options.length > 1));
}

function validateRenames(): ValidationError[] {
  const errors: ValidationError[] = [];
  const duplicateNames = findDuplicates("name");
  const duplicateFeatureNames = findDuplicates("featureName");

  renameErrors.value = {};
  props.featureOptions
    .filter(isSelected)
    .forEach((option) => {
      const { name, featureName } = renameInputs.value[option.featureId];
      const subarea = option.featureName;
      const featureErrors: Partial<Record<RenameField, string>> = {};

      if (!name.trim()) {
        featureErrors.name = `Enter a subarea name for ${subarea}`;
      } else if (duplicateNames.has(normalise(name))) {
        featureErrors.name = `Subarea name for ${subarea} must be unique within the block`;
      }

      if (!featureName.trim()) {
        featureErrors.featureName = `Enter a map display name for ${subarea}`;
      } else if (featureName.trim().length > MAP_DISPLAY_NAME_MAX_LENGTH) {
        featureErrors.featureName
          = `Map display name for ${subarea} must be ${MAP_DISPLAY_NAME_MAX_LENGTH} characters or fewer`;
      } else if (duplicateFeatureNames.has(normalise(featureName))) {
        featureErrors.featureName = `Map display name for ${subarea} must be unique within the block`;
      }

      renameErrors.value[option.featureId] = featureErrors;
      (Object.keys(featureErrors) as RenameField[]).forEach(field => errors.push({
        message: featureErrors[field]!,
        fieldId: inputId(option.featureId, field),
      }));
    });
  return errors;
}

function isChanged(option: FeatureOption) {
  const { name, featureName } = renameInputs.value[option.featureId];
  return name.trim() !== option.name || featureName.trim() !== option.featureName;
}

async function onSubmit() {
  const changedOptions = props.featureOptions
    .filter(isSelected)
    .filter(isChanged);

  const errors: ValidationError[] = [];
  if (!selectedFeatureIds.value.length) {
    errors.push({
      message: "Select at least one subarea to rename",
      fieldId: checkboxId(props.featureOptions[0]?.featureId),
    });
  }
  featuresError.value = errors[0]?.message;
  errors.push(...validateRenames());
  emit("validation-error", errors);

  if (errors.length) {
    return;
  }
  // Ticked subareas left unchanged aren't sent; if none changed there's nothing to rename
  if (!changedOptions.length) {
    emit("back");
    return;
  }
  await rename(changedOptions.map(option => ({
    featureId: option.featureId,
    name: renameInputs.value[option.featureId].name.trim(),
    featureName: renameInputs.value[option.featureId].featureName.trim(),
  })));
}

const heading = useTemplateRef<HTMLHeadingElement>("heading");

defineExpose({ focus: () => heading.value?.focus() });
</script>
