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
    <div class="gis-split-layout">
      <div class="gis-split-layout__map">
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
          :map-style-override="mapStyleOverride"
          @update:points="points = $event"
        />
      </div>
      <div class="gis-split-layout__description">
        <textual-description
          :textual-description-url="textualDescriptionUrl"
          :command-journey-id="commandJourneyId"
          :refresh-counter="refreshCounter"
        />
      </div>
    </div>
    <split-actions
      :auto-split="true"
      :points="points"
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
  </div>
  <line-coordinate-stack :points="points" :srs-wkid="srsWkid" @undo-last-point="undoLastPoint"/>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "../coordinate-system-utils";
import type { SnapPoint } from "../grid-utils";
import { computed, CSSProperties, ref } from "vue";
import BaseMap from "../components/baseMap/BaseMap.vue";
import ErrorSummary from "../components/gdsComponents/error/ErrorSummary.vue";
import DetailsComponent from "../components/govukVue/details/GvDetails.vue";
import LineCoordinateStack from "../components/lineCoordinateStack/LineCoordinateStack.vue";
import SplitActions from "../components/split/SplitActions.vue";
import TextualDescription from "../components/textualDescription/TextualDescription.vue";

interface SplitByPointAndClickPageProps {
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
const refreshCounter = ref(0);

// Make the map fill its panel rather than use BaseMap's default clamped height.
const mapStyleOverride: CSSProperties = {
  width: "100%",
  height: "100%",
  display: "block",
};

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
</script>

<style scoped>
.gis-split-layout {
  display: flex;
  gap: 1rem;
  width: 100%;
}

.gis-split-layout__map,
.gis-split-layout__description {
  min-width: 0;
  aspect-ratio: 1 / 1;
}

.gis-split-layout__map {
  flex: 2 1 0;
}

.gis-split-layout__description {
  flex: 1 1 0;
  overflow: auto;
}
</style>
