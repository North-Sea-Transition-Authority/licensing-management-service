<template>
  <h2 ref="heading" class="govuk-heading-m govuk-!-margin-top-4" tabindex="-1">
    Merge
  </h2>
  <details-component summary="How can I merge subareas?">
    <p class="govuk-body">
      Select two or more subareas using the checkboxes, then click merge to combine them into a single
      subarea.
    </p>
    <p v-if="hasDepths" class="govuk-body">
      Subareas can be merged across depths, use the depth slider to see selected subareas at other depths.
    </p>
  </details-component>
  <gv-checkboxes
    v-model="selectedFeatureIds"
    legend="Select the subareas to merge"
    legend-class="govuk-fieldset__legend--s"
  >
    <gv-checkbox
      v-for="feature in featureOptions"
      :key="feature.featureId"
      :value="feature.featureId"
      :label="feature.featureName"
      :hint="formatDepthRanges(feature.depthRanges)"
    />
  </gv-checkboxes>
  <!--    TODO: determine when attributes will be lost and show warning -->
  <gv-warning-text v-if="selectedFeatureIds.length >= 2">
    The attributes for the merged subarea will need to be defined again.
  </gv-warning-text>
  <gv-button-group>
    <gv-button text="Merge" :disabled="selectedFeatureIds.length < 2 || isProcessing" @click="merge"/>
    <gv-button variant="secondary" text="Back to action select" :disabled="isProcessing" @click="emit('back')"/>
  </gv-button-group>
</template>

<script setup lang="ts">
import type { FeatureOption } from "@/feature-option-utils";
import { useTemplateRef } from "vue";
import { useMergeFeature } from "@/composables/useMergeFeature";
import { formatDepthRanges } from "@/depth-utils";
import GvButton from "../govukVue/button/GvButton.vue";
import GvButtonGroup from "../govukVue/button/GvButtonGroup.vue";
import GvCheckbox from "../govukVue/checkboxes/GvCheckbox.vue";
import GvCheckboxes from "../govukVue/checkboxes/GvCheckboxes.vue";
import DetailsComponent from "../govukVue/details/GvDetails.vue";
import GvWarningText from "../govukVue/warning-text/GvWarningText.vue";

interface MergePanelProps {
  baseUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
  featureOptions: FeatureOption[],
  hasDepths: boolean,
}

const props = defineProps<MergePanelProps>();

const emit = defineEmits<{
  success: [],
  error: [message: string],
  back: [],
}>();

const selectedFeatureIds = defineModel<string[]>("selectedFeatureIds", { required: true });

const { isProcessing, merge } = useMergeFeature(
  () => ({
    mergeUrl: `${props.baseUrl}/merge`,
    commandJourneyId: props.commandJourneyId,
    csrfHeaderName: props.csrfHeaderName,
    csrfToken: props.csrfToken,
  }),
  selectedFeatureIds,
  {
    onSuccess: () => emit("success"),
    onError: message => emit("error", message),
  },
);

const heading = useTemplateRef<HTMLHeadingElement>("heading");

defineExpose({ focus: () => heading.value?.focus() });
</script>
