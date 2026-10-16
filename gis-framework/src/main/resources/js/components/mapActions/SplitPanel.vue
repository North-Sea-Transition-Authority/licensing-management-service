<template>
  <h2 ref="heading" class="govuk-heading-m govuk-!-margin-top-4" tabindex="-1">
    Split
  </h2>
  <details-component summary="How can I split the feature?">
    <p class="govuk-body">
      You can split the feature by clicking the snap points on the map to draw a line.
      The split happens as soon as the line fully crosses the feature.
      You can only draw a single line at a time.
    </p>
    <p v-if="depth !== undefined" class="govuk-body">
      The current depth slider is locked while you draw. Remove the line points to change depth.
    </p>
  </details-component>
  <line-coordinate-stack :points="points" :srs-wkid="srsWkid" @undo-last-point="emit('undo-last-point')"/>
  <gv-button variant="secondary" text="Back to action select" :disabled="isProcessing" @click="emit('back')"/>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "@/coordinate-system-utils";
import type { SnapPoint } from "@/grid-utils";
import { useTemplateRef } from "vue";
import { useSplitFeature } from "@/composables/useSplitFeature";
import GvButton from "../govukVue/button/GvButton.vue";
import DetailsComponent from "../govukVue/details/GvDetails.vue";
import LineCoordinateStack from "../lineCoordinateStack/LineCoordinateStack.vue";

interface SplitPanelProps {
  baseUrl: string,
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
  points: SnapPoint[],
  srsWkid: SupportedWkid,
  depth?: number,
}

const props = defineProps<SplitPanelProps>();

const emit = defineEmits<{
  "success": [],
  "error": [message: string],
  "back": [],
  "undo-last-point": [],
}>();

// The panel is only shown in split mode, so every completed line is split straight away.
const { isProcessing } = useSplitFeature(
  () => ({
    splitUrl: `${props.baseUrl}/split`,
    commandJourneyId: props.commandJourneyId,
    csrfHeaderName: props.csrfHeaderName,
    csrfToken: props.csrfToken,
    depth: props.depth,
  }),
  () => props.points,
  {
    autoSplit: true,
    onSuccess: () => emit("success"),
    onError: message => emit("error", message),
  },
);

const heading = useTemplateRef<HTMLHeadingElement>("heading");

defineExpose({ focus: () => heading.value?.focus() });
</script>
