<template>
  <div>
    <details-component summary="How can I split the block?">
      <p class="govuk-body">
        You can split the block by clicking the snap points on the map to draw a line.
        Click split once you are done drawing the line.
        You can only draw a single line at a time, but you can perform multiple splits.
      </p>
      <p class="govuk-body">
        If you made a mistake, you can use the undo/redo split buttons to fix it.
        You can also remove single line points before triggering a split.
      </p>
    </details-component>
    <error-summary v-if="splitError" :description="splitError"/>
    <base-map
      ref="baseMapRef"
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
  <line-coordinate-stack :points="points" :srs-wkid="srsWkid" @undo-last-point="undoLastPoint"/>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "../../coordinate-system-utils";
import type { SnapPoint } from "../../grid-utils";
import { computed, ref, watch } from "vue";
import { splitFeature } from "../../api/split.api";
import BaseMap from "../baseMap/BaseMap.vue";
import ErrorSummary from "../gdsComponents/error/ErrorSummary.vue";
import DetailsComponent from "../govukVue/details/GvDetails.vue";
import LineCoordinateStack from "../lineCoordinateStack/LineCoordinateStack.vue";
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

const baseMapRef = ref<InstanceType<typeof BaseMap> | null>(null);
const points = ref<SnapPoint[]>([]);
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

function undoLastPoint() {
  baseMapRef.value?.removeLastPoint();
}

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
