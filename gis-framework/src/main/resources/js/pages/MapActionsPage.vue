<template>
  <error-summary v-if="error" :description="error"/>
  <error-summary v-else-if="validationErrors.length" :key="validationAttempt">
    <li v-for="validationError in validationErrors" :key="validationError.fieldId">
      <a :href="`#${validationError.fieldId}`">{{ validationError.message }}</a>
    </li>
  </error-summary>
  <div class="gis-depth-layout govuk-!-margin-bottom-6" :class="{ 'gis-depth-layout--no-slider': !hasDepths }">
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
        :include-snap-points="mode === 'SPLIT'"
        :include-draw-line="mode === 'SPLIT'"
        :refresh-counter="refreshCounter"
        :refit-on-features-change="false"
        :map-style-override="mapStyleOverride"
        :selected-feature-ids="mode === 'MERGE' || mode === 'STRATA_SPLIT' ? selectedFeatureIds : []"
        @update:points="points = $event"
      />
    </div>
    <section class="gis-depth-layout__description" aria-label="Textual description">
      <textual-description
        :textual-description-url="textualDescriptionUrl"
        :command-journey-id="commandJourneyId"
        :refresh-counter="refreshCounter"
      />
    </section>
  </div>
  <div v-if="mode === 'select'">
    <undo-redo-actions
      v-model:processing="isProcessing"
      :history-status="historyStatus"
      :base-url="baseUrl"
      :command-journey-id="commandJourneyId"
      :csrf-header-name="csrfHeaderName"
      :csrf-token="csrfToken"
      :disabled="isReloading"
      @action-success="onHistoryActionSuccess"
      @action-error="error = $event"
    />
    <action-radio-group
      ref="actionRadioGroup"
      :has-actions="!!historyStatus?.undoTransformationType"
      :can-merge="activeFeatureCount >= 2"
      :can-add-strata="hasDepths"
      :processing="isProcessing || isReloading"
      :error-message="validationErrors[0]?.message"
      @select="onSelect"
      @validation-error="onValidationError($event ? [$event] : [])"
    />
  </div>
  <split-panel
    v-else-if="mode === 'SPLIT'"
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
    v-else-if="mode === 'MERGE'"
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
  <strata-split-panel
    v-else-if="mode === 'STRATA_SPLIT'"
    ref="actionPanel"
    v-model:selected-feature-ids="selectedFeatureIds"
    :base-url="baseUrl"
    :command-journey-id="commandJourneyId"
    :csrf-header-name="csrfHeaderName"
    :csrf-token="csrfToken"
    :feature-options="featureOptions"
    @success="onActionSuccess"
    @error="error = $event"
    @back="returnToSelect"
    @validation-error="onValidationError"
  />
</template>

<script setup lang="ts">
import type { SupportedWkid } from "@/coordinate-system-utils";
import type { SnapPoint } from "@/grid-utils";
import type { TransformationType } from "@/transformation-type";
import type { ValidationError } from "@/validation-error";
import { computed, CSSProperties, nextTick, ref, useTemplateRef } from "vue";
import { buildCommandJourneyUrl } from "@/command-journey-utils";
import ActionRadioGroup from "@/components/actionRadioGroup/ActionRadioGroup.vue";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import DepthSlider from "@/components/depthSlider/DepthSlider.vue";
import ErrorSummary from "@/components/gdsComponents/error/ErrorSummary.vue";
import MergePanel from "@/components/mapActions/MergePanel.vue";
import SplitPanel from "@/components/mapActions/SplitPanel.vue";
import StrataSplitPanel from "@/components/mapActions/StrataSplitPanel.vue";
import TextualDescription from "@/components/textualDescription/TextualDescription.vue";
import UndoRedoActions from "@/components/undoRedo/UndoRedoActions.vue";
import { useCommandJourneyFeatures } from "@/composables/useCommandJourneyFeatures";
import { useDepthBands } from "@/composables/useDepthBands";
import { useHistoryStatus } from "@/composables/useHistoryStatus";
import { toSortedFeatureOptions } from "@/feature-option-utils";

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

const mode = ref<"select" | TransformationType>("select");
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

const { features, outlineNodes, isLoading: isFeaturesLoading } = useCommandJourneyFeatures(
  featuresUrl,
  outlineNodesUrl,
  refreshCounter,
  {
    onError: () => {
      error.value = "Unable to load the features.";
    },
  },
);
const { hasDepths, depths, currentDepthLevel, visibleFeatures, visibleOutlineNodes } = useDepthBands(features, outlineNodes);

const { historyStatus, isLoading: isHistoryLoading } = useHistoryStatus(props.baseUrl, props.commandJourneyId, refreshCounter, {
  onError: () => {
    error.value = "Unable to load the actions taken.";
  },
});

const isProcessing = ref(false);

// Until both reloads finish, undo/redo would act on a stale history entry and Continue on stale features.
const isReloading = computed(() => isHistoryLoading.value || isFeaturesLoading.value);

// Features are returned one per polygon, so count distinct feature IDs.
const activeFeatureCount = computed(() => new Set(features.value.map(feature => feature.get("featureId"))).size);

// Lists every feature, not only those in the current depth band.
const featureOptions = computed(() => toSortedFeatureOptions(features.value));

const validationErrors = ref<ValidationError[]>([]);
// Re-mounts the error summary on each failed attempt so it takes focus again.
const validationAttempt = ref(0);

function onValidationError(errors: ValidationError[]) {
  validationErrors.value = errors;
  if (errors.length) {
    error.value = null;
    validationAttempt.value++;
  }
}

// TODO - LMS2-268: include rename. Can remove once all actions have panels
const hasPanel: Record<TransformationType, boolean> = {
  SPLIT: true,
  MERGE: true,
  STRATA_SPLIT: true,
  RENAME: false,
};

async function onSelect(action: TransformationType) {
  if (!hasPanel[action]) {
    return;
  }
  clearErrors();
  mode.value = action;
  await nextTick();
  actionPanel.value?.focus();
}

function undoLastPoint() {
  baseMapRef.value?.removeLastPoint();
}

function clearErrors() {
  error.value = null;
  validationErrors.value = [];
}

function onActionSuccess() {
  refreshCounter.value++;
  void returnToSelect();
}

function onHistoryActionSuccess() {
  clearErrors();
  refreshCounter.value++;
}

async function returnToSelect() {
  clearErrors();
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
  grid-template-columns: auto minmax(0, calc((100% - 1rem) * 2 / 3));
  grid-template-areas:
    "slider      map"
    "description description";
  justify-content: start;
  gap: 1rem;
}

.gis-depth-layout--no-slider {
  grid-template-columns: calc((100% - 1rem) * 2 / 3);
  grid-template-areas:
    "map"
    "description";
}

.gis-depth-layout__slider {
  grid-area: slider;
}

.gis-depth-layout__map-canvas {
  grid-area: map;
  aspect-ratio: 1 / 1;
}

.gis-depth-layout__description {
  grid-area: description;
  contain: inline-size;
  max-height: 14rem;
  overflow-y: auto;
}
</style>
