<template>
  <error-summary v-if="error" :description="error"/>
  <error-summary v-else-if="validationError" :key="validationAttempt">
    <li>
      <a :href="`#${validationError.fieldId}`">{{ validationError.message }}</a>
    </li>
  </error-summary>
  <div class="gis-depth-layout">
    <depth-slider
      v-if="hasDepths"
      v-model="currentDepthLevel"
      :options="depths"
      :disabled="points.length > 0"
      class="gis-depth-layout__slider"
    />
    <div class="gis-depth-layout__map-canvas">
      <base-map
        ref="baseMapRef"
        :srs-wkid="srsWkid"
        :features="visibleFeatures"
        :outline-nodes="visibleOutlineNodes"
        :include-nsta-quadrants="includeNstaQuadrants"
        :include-nsta-blocks="includeNstaBlocks"
        :include-snap-points="mode === MapAction.SPLIT"
        :include-draw-line="mode === MapAction.SPLIT"
        :refresh-counter="refreshCounter"
        :refit-on-features-change="false"
        :map-style-override="mapStyleOverride"
        @update:points="points = $event"
      />
    </div>
    <div class="gis-depth-layout__description">
      <textual-description
        :textual-description-url="textualDescriptionUrl"
        :command-journey-id="commandJourneyId"
        :refresh-counter="refreshCounter"
      />
    </div>
  </div>
  <action-radio-group
    v-if="mode === 'select'"
    ref="actionRadioGroup"
    :has-actions="historyStatus?.canUndo ?? false"
    :can-merge="activeFeatureCount >= 2"
    @select="onSelect"
    @validation-error="onValidationError"
  />
  <template v-else-if="mode === MapAction.SPLIT">
    <h2 ref="panelHeading" class="govuk-heading-m govuk-!-margin-top-4" tabindex="-1">
      Split
    </h2>
    <details-component summary="How can I split the block?">
      <p class="govuk-body">
        You can split the block by clicking the snap points on the map to draw a line.
        The split happens as soon as the line fully crosses the block.
        You can only draw a single line at a time.
      </p>
      <p class="govuk-body">
        The current depth slider is locked while you draw. Remove the line points to change depth.
      </p>
    </details-component>
    <line-coordinate-stack :points="points" :srs-wkid="srsWkid" @undo-last-point="undoLastPoint"/>
    <gv-button variant="secondary" text="Back to action select" :disabled="isProcessing" @click="returnToSelect"/>
  </template>
</template>

<script setup lang="ts">
import type { JsonHistoryStatus } from "@/api/history.api";
import type { SupportedWkid } from "@/coordinate-system-utils";
import type { SnapPoint } from "@/grid-utils";
import type { MapActionValidationError } from "@/map-action";
import { computed, CSSProperties, nextTick, ref, useTemplateRef, watch } from "vue";
import { getHistoryStatus } from "@/api/history.api";
import { buildCommandJourneyUrl } from "@/command-journey-utils";
import ActionRadioGroup from "@/components/actionRadioGroup/ActionRadioGroup.vue";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import DepthSlider from "@/components/depthSlider/DepthSlider.vue";
import ErrorSummary from "@/components/gdsComponents/error/ErrorSummary.vue";
import GvButton from "@/components/govukVue/button/GvButton.vue";
import DetailsComponent from "@/components/govukVue/details/GvDetails.vue";
import LineCoordinateStack from "@/components/lineCoordinateStack/LineCoordinateStack.vue";
import TextualDescription from "@/components/textualDescription/TextualDescription.vue";
import { useCommandJourneyFeatures } from "@/composables/useCommandJourneyFeatures";
import { useDepthBands } from "@/composables/useDepthBands";
import { useSplitFeature } from "@/composables/useSplitFeature";
import { MapAction } from "@/map-action";

interface MapActionsPageProps {
  commandJourneyId: string,
  srsWkid: SupportedWkid,
  featuresBaseUrl: string,
  outlineNodesBaseUrl: string,
  baseUrl: string,
  textualDescriptionUrl: string,
  csrfHeaderName: string,
  csrfToken: string,
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
}

const props = withDefaults(defineProps<MapActionsPageProps>(), {
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

// Make the map fill its panel rather than use BaseMap's default clamped height.
const mapStyleOverride: CSSProperties = {
  width: "100%",
  height: "100%",
  display: "block",
};

const mode = ref<"select" | MapAction>("select");
const baseMapRef = useTemplateRef<InstanceType<typeof BaseMap>>("baseMapRef");
const panelHeading = useTemplateRef<HTMLHeadingElement>("panelHeading");
const actionRadioGroup = useTemplateRef<InstanceType<typeof ActionRadioGroup>>("actionRadioGroup");
const points = ref<SnapPoint[]>([]);
const error = ref<string | null>(null);
// Incremented after each action, reloading the features, description and history.
const refreshCounter = ref(0);

const featuresUrl = buildCommandJourneyUrl(props.featuresBaseUrl, props.commandJourneyId);
const outlineNodesUrl = buildCommandJourneyUrl(props.outlineNodesBaseUrl, props.commandJourneyId);

const { features, outlineNodes, hasError } = useCommandJourneyFeatures(featuresUrl, outlineNodesUrl, refreshCounter);
const { hasDepths, depths, currentDepthLevel, visibleFeatures, visibleOutlineNodes } = useDepthBands(features, outlineNodes);

watch(hasError, (errored) => {
  if (errored) {
    error.value = "Unable to load the features.";
  }
});

// TODO - EPGF-271: The history status functions could be moved into a composable when undo / redo is added to this page.
const historyUrl = buildCommandJourneyUrl(`${props.baseUrl}/history`, props.commandJourneyId);
const historyStatus = ref<JsonHistoryStatus | null>(null);

async function fetchHistoryStatus() {
  try {
    historyStatus.value = await getHistoryStatus(historyUrl);
  } catch {
    error.value = "Unable to load the actions taken.";
  }
}

void fetchHistoryStatus();
watch(refreshCounter, fetchHistoryStatus);

// Features are returned one per polygon, so count distinct feature IDs.
const activeFeatureCount = computed(() => new Set(features.value.map(feature => feature.get("featureId"))).size);

const validationError = ref<MapActionValidationError | null>(null);
// Re-mounts the error summary on each failed attempt so it takes focus again.
const validationAttempt = ref(0);

function onValidationError(validation: MapActionValidationError | null) {
  validationError.value = validation;
  if (validation) {
    validationAttempt.value++;
  }
}

const { isProcessing } = useSplitFeature(
  () => ({
    splitUrl: `${props.baseUrl}/split`,
    commandJourneyId: props.commandJourneyId,
    csrfHeaderName: props.csrfHeaderName,
    csrfToken: props.csrfToken,
    depth: hasDepths.value ? currentDepthLevel.value : undefined,
  }),
  points,
  {
    autoSplit: () => mode.value === MapAction.SPLIT,
    onSuccess: onSplitSuccess,
    onError: (message) => {
      error.value = message;
    },
  },
);

async function onSelect(action: MapAction) {
  // TODO - EPGF-105, EPGF-106, later: include add strata, merge and rename
  if (action !== MapAction.SPLIT) {
    return;
  }
  error.value = null;
  mode.value = action;
  await nextTick();
  panelHeading.value?.focus();
}

function undoLastPoint() {
  baseMapRef.value?.removeLastPoint();
}

// Auto-split applies each completed line straight away, then the user picks their next action.
function onSplitSuccess() {
  error.value = null;
  refreshCounter.value++;
  void returnToSelect();
}

async function returnToSelect() {
  points.value = [];
  mode.value = "select";
  await nextTick();
  actionRadioGroup.value?.focus();
}
</script>

<style scoped>
.gis-depth-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  grid-template-areas:
    "slider ."
    "map    description";
  column-gap: 1rem;
}

.gis-depth-layout__slider {
  grid-area: slider;
  margin-bottom: 1rem;
}

.gis-depth-layout__map-canvas {
  grid-area: map;
  justify-self: stretch;
  align-self: start;
  aspect-ratio: 1 / 1;
}

.gis-depth-layout__description {
  grid-area: description;
  contain: size;
  overflow: auto;
}
</style>
