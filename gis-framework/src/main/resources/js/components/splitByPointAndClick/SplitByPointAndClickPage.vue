<template>
  <div>
    <error-summary v-if="splitError" :description="splitError"/>
    <base-map
      :srs-wkid="srsWkid"
      :features-url="featuresUrl"
      :outline-nodes-url="outlineNodesUrl"
      :include-nsta-quadrants="includeNstaQuadrants"
      :include-nsta-blocks="includeNstaBlocks"
      :include-snap-points="true"
      :include-draw-line="true"
      :refresh-counter="refreshCounter"
      @update:points="points = $event"
    />
    <split-actions
      :refresh-counter="refreshCounter"
      :history-url="historyUrl"
      :undo-url="undoUrl"
      :redo-url="redoUrl"
      :csrf-header-name="csrfHeaderName"
      :csrf-token="csrfToken"
      @action-success="onSplitSuccess"
      @action-error="splitError = $event"
    />
  </div>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "../../coordinate-system-utils";
import type { LinePoint } from "../../grid-utils";
import { computed, ref, watch } from "vue";
import { splitFeature } from "../../api/split.api";
import BaseMap from "../baseMap/BaseMap.vue";
import ErrorSummary from "../gdsComponents/error/ErrorSummary.vue";
import SplitActions from "./SplitActions.vue";

interface SplitByPointAndClickPageProps {
  commandJourneyId: string,
  srsWkid: SupportedWkid,
  featuresBaseUrl: string,
  outlineNodesBaseUrl: string,
  splitUrl: string,
  historyBaseUrl: string,
  undoBaseUrl: string,
  redoBaseUrl: string,
  csrfHeaderName: string,
  csrfToken: string,
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
}

const props = withDefaults(defineProps<SplitByPointAndClickPageProps>(), {
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

const points = ref<LinePoint[]>([]);
const splitError = ref<string | null>(null);
const autoSplitInProgress = ref(false);
const refreshCounter = ref(0);

function buildCommandJourneyUrl(baseUrl: string, commandJourney: string): string {
  return `${baseUrl}/${commandJourney}`;
}

const featuresUrl = computed(() => buildCommandJourneyUrl(props.featuresBaseUrl, props.commandJourneyId));
const outlineNodesUrl = computed(() => buildCommandJourneyUrl(props.outlineNodesBaseUrl, props.commandJourneyId));
const historyUrl = computed(() => buildCommandJourneyUrl(props.historyBaseUrl, props.commandJourneyId));
const undoUrl = computed(() => buildCommandJourneyUrl(props.undoBaseUrl, props.commandJourneyId));
const redoUrl = computed(() => buildCommandJourneyUrl(props.redoBaseUrl, props.commandJourneyId));

function onSplitSuccess() {
  splitError.value = null;
  refreshCounter.value++;
  points.value = [];
}

watch(points, async () => {
  if (autoSplitInProgress.value || points.value.length < 2) {
    return;
  }

  autoSplitInProgress.value = true;
  try {
    const splitResponse = await splitFeature(
      props.splitUrl,
      points.value,
      props.commandJourneyId,
      props.csrfHeaderName,
      props.csrfToken,
    );
    if (splitResponse.outputFeatureIds.length > 0) {
      onSplitSuccess();
    } else {
      console.warn("No split took place. Make sure your line crosses the feature boundary.");
    }
  } catch {
    splitError.value = "An error occurred while attempting to split the feature. Please try again.";
  } finally {
    autoSplitInProgress.value = false;
  }
});
</script>
