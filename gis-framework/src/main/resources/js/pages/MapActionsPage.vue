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
        :selected-feature-ids="mode === MapAction.MERGE ? selectedFeatureIds : []"
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
  <split-panel
    v-else-if="mode === MapAction.SPLIT"
    ref="actionPanel"
    :base-url="baseUrl"
    :command-journey-id="commandJourneyId"
    :csrf-header-name="csrfHeaderName"
    :csrf-token="csrfToken"
    :points="points"
    :srs-wkid="srsWkid"
    :depth="hasDepths ? currentDepthLevel : undefined"
    @success="onActionSuccess"
    @error="error = $event"
    @back="returnToSelect"
    @undo-last-point="undoLastPoint"
  />
  <merge-panel
    v-else-if="mode === MapAction.MERGE"
    ref="actionPanel"
    v-model:selected-feature-ids="selectedFeatureIds"
    :base-url="baseUrl"
    :command-journey-id="commandJourneyId"
    :csrf-header-name="csrfHeaderName"
    :csrf-token="csrfToken"
    :feature-options="featureOptions"
    :has-depths="hasDepths"
    @success="onActionSuccess"
    @error="error = $event"
    @back="returnToSelect"
  />
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
import MergePanel from "@/components/mapActions/MergePanel.vue";
import SplitPanel from "@/components/mapActions/SplitPanel.vue";
import TextualDescription from "@/components/textualDescription/TextualDescription.vue";
import { useCommandJourneyFeatures } from "@/composables/useCommandJourneyFeatures";
import { useDepthBands } from "@/composables/useDepthBands";
import { toSortedFeatureOptions } from "@/feature-option-utils";
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
const actionPanel = useTemplateRef<{ focus: () => void }>("actionPanel");
const actionRadioGroup = useTemplateRef<InstanceType<typeof ActionRadioGroup>>("actionRadioGroup");
const points = ref<SnapPoint[]>([]);
const selectedFeatureIds = ref<string[]>([]);
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

// Lists every subarea, not only those in the current depth band.
const featureOptions = computed(() => toSortedFeatureOptions(features.value));

const validationError = ref<MapActionValidationError | null>(null);
// Re-mounts the error summary on each failed attempt so it takes focus again.
const validationAttempt = ref(0);

function onValidationError(validation: MapActionValidationError | null) {
  validationError.value = validation;
  if (validation) {
    validationAttempt.value++;
  }
}

// TODO - EPGF-106, later: include add strata and rename.
const panelActions: MapAction[] = [MapAction.SPLIT, MapAction.MERGE];

async function onSelect(action: MapAction) {
  if (!panelActions.includes(action)) {
    return;
  }
  error.value = null;
  mode.value = action;
  await nextTick();
  actionPanel.value?.focus();
}

function undoLastPoint() {
  baseMapRef.value?.removeLastPoint();
}

function onActionSuccess() {
  error.value = null;
  refreshCounter.value++;
  void returnToSelect();
}

async function returnToSelect() {
  error.value = null;
  points.value = [];
  selectedFeatureIds.value = [];
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
