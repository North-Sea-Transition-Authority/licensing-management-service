<template>
  <error-summary v-if="error" :description="error"/>
  <error-summary v-else-if="validationError" :key="validationAttempt">
    <li>
      <a :href="`#${validationError.fieldId}`">{{ validationError.message }}</a>
    </li>
  </error-summary>
  <div class="gis-depth-layout">
    <depth-slider v-if="hasDepths" v-model="currentDepthLevel" :options="depths" class="gis-depth-layout__slider" />
    <div class="gis-depth-layout__map-canvas">
      <base-map
        :srs-wkid="srsWkid"
        :features="visibleFeatures"
        :outline-nodes="visibleOutlineNodes"
        :include-nsta-quadrants="includeNstaQuadrants"
        :include-nsta-blocks="includeNstaBlocks"
        :include-snap-points="false"
        :include-draw-line="false"
        :refit-on-features-change="false"
        :map-style-override="mapStyleOverride"
      />
    </div>
    <div class="gis-depth-layout__description">
      <textual-description
        :textual-description-url="textualDescriptionUrl"
        :command-journey-id="commandJourneyId"
      />
    </div>
  </div>
  <action-radio-group :can-merge="activeFeatureCount >= 2" @validation-error="onValidationError"/>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "@/coordinate-system-utils";
import type { MapActionValidationError } from "@/map-action";
import { computed, CSSProperties, ref, watch } from "vue";
import { buildCommandJourneyUrl } from "@/command-journey-utils";
import ActionRadioGroup from "@/components/actionRadioGroup/ActionRadioGroup.vue";
import BaseMap from "@/components/baseMap/BaseMap.vue";
import DepthSlider from "@/components/depthSlider/DepthSlider.vue";
import ErrorSummary from "@/components/gdsComponents/error/ErrorSummary.vue";
import TextualDescription from "@/components/textualDescription/TextualDescription.vue";
import { useCommandJourneyFeatures } from "@/composables/useCommandJourneyFeatures";
import { useDepthBands } from "@/composables/useDepthBands";

interface DepthActionsPageProps {
  commandJourneyId: string,
  srsWkid: SupportedWkid,
  featuresBaseUrl: string,
  outlineNodesBaseUrl: string,
  textualDescriptionUrl: string,
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
}

const props = withDefaults(defineProps<DepthActionsPageProps>(), {
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

// Make the map fill its panel rather than use BaseMap's default clamped height.
const mapStyleOverride: CSSProperties = {
  width: "100%",
  height: "100%",
  display: "block",
};

const error = ref<string | null>(null);

const featuresUrl = buildCommandJourneyUrl(props.featuresBaseUrl, props.commandJourneyId);
const outlineNodesUrl = buildCommandJourneyUrl(props.outlineNodesBaseUrl, props.commandJourneyId);

const { features, outlineNodes, hasError } = useCommandJourneyFeatures(featuresUrl, outlineNodesUrl);
const { hasDepths, depths, currentDepthLevel, visibleFeatures, visibleOutlineNodes } = useDepthBands(features, outlineNodes);

watch(hasError, (errored) => {
  if (errored) {
    error.value = "Unable to load the features.";
  }
});

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
