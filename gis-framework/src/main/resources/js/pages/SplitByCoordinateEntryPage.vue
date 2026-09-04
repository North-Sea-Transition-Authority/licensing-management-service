<template>
  <error-summary v-if="splitError" :description="splitError"/>
  <div class="govuk-grid-row split-row">
    <div class="govuk-grid-column-one-half">
      <coordinate-list :key="refreshCounter" v-model="points" :srs-wkid="srsWkid" :coordinate-precision="coordinatePrecision"/>
    </div>
    <div class="govuk-grid-column-one-half">
      <div class="split-sticky">
        <base-map
          :srs-wkid="srsWkid"
          :features-url="featuresUrl"
          :outline-nodes-url="outlineNodesUrl"
          :include-nsta-quadrants="includeNstaQuadrants"
          :include-nsta-blocks="includeNstaBlocks"
          :include-snap-points="false"
          :include-draw-line="true"
          :selected-points="linePoints"
          :refresh-counter="refreshCounter"
        />
        <split-actions
          :show-split-button="true"
          :points="linePoints"
          :split-url="splitUrl"
          :command-journey-id="commandJourneyId"
          :refresh-counter="refreshCounter"
          :history-url="historyUrl"
          :undo-url="undoUrl"
          :redo-url="redoUrl"
          :csrf-header-name="csrfHeaderName"
          :csrf-token="csrfToken"
          @action-success="onSplitSuccess"
          @action-error="splitError = $event"
        />
        <textual-description
          :textual-description-url="textualDescriptionUrl"
          :command-journey-id="commandJourneyId"
          :refresh-counter="refreshCounter"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { EditablePoint } from "../components/coordinateInput/CoordinateList.vue";
import type { SupportedWkid } from "../coordinate-system-utils";
import type { LinePoint } from "../grid-utils";
import { computed, ref } from "vue";
import BaseMap from "../components/baseMap/BaseMap.vue";
import CoordinateList from "../components/coordinateInput/CoordinateList.vue";
import ErrorSummary from "../components/gdsComponents/error/ErrorSummary.vue";
import SplitActions from "../components/split/SplitActions.vue";
import TextualDescription from "../components/textualDescription/TextualDescription.vue";

interface SplitByCoordinateEntryPageProps {
  commandJourneyId: string,
  srsWkid: SupportedWkid,
  featuresBaseUrl: string,
  outlineNodesBaseUrl: string,
  splitUrl: string,
  historyBaseUrl: string,
  undoBaseUrl: string,
  redoBaseUrl: string,
  textualDescriptionUrl: string,
  csrfHeaderName: string,
  csrfToken: string,
  coordinatePrecision?: number,
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
}

const props = withDefaults(defineProps<SplitByCoordinateEntryPageProps>(), {
  coordinatePrecision: 4,
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

function buildCommandJourneyUrl(baseUrl: string, commandJourney: string): string {
  return `${baseUrl}/${commandJourney}`;
}

const featuresUrl = computed(() => buildCommandJourneyUrl(props.featuresBaseUrl, props.commandJourneyId));
const outlineNodesUrl = computed(() => buildCommandJourneyUrl(props.outlineNodesBaseUrl, props.commandJourneyId));
const historyUrl = computed(() => buildCommandJourneyUrl(props.historyBaseUrl, props.commandJourneyId));
const undoUrl = computed(() => buildCommandJourneyUrl(props.undoBaseUrl, props.commandJourneyId));
const redoUrl = computed(() => buildCommandJourneyUrl(props.redoBaseUrl, props.commandJourneyId));

function createInitialPoint(): EditablePoint {
  return {
    id: 0,
    originalSrsCoordinates: [Number.NaN, Number.NaN],
    coordinates: undefined,
  };
}

const points = ref<EditablePoint[]>([createInitialPoint()]);
const splitError = ref<string | null>(null);
const refreshCounter = ref(0);

const linePoints = computed<LinePoint[]>(() =>
  points.value
    .filter((point): point is EditablePoint & { coordinates: [number, number] } => point.coordinates !== undefined)
    .map(point => ({ coordinates: point.coordinates, originalSrsCoordinates: point.originalSrsCoordinates })),
);

function onSplitSuccess() {
  splitError.value = null;
  refreshCounter.value++;
  points.value = [createInitialPoint()];
}
</script>

<style scoped>
.split-row {
  display: flex;
}
.split-sticky {
  position: sticky;
  top: 1rem;
  max-height: calc(100vh - 2rem);
  overflow-y: auto;
  overflow-x: hidden;
}
@media (max-width: 40.0625em) {
  .split-row {
    display: block;
  }
}
</style>
