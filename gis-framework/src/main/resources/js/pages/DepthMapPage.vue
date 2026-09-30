<template>
  <error-summary v-if="error" :description="error"/>
  <div class="gis-depth-layout">
    <div class="gis-depth-layout__map">
      <depth-slider v-model="currentDepthLevel" :options="depths" />
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
    </div>
    <div class="gis-depth-layout__side">
      <div class="gis-depth-layout__description">
        <textual-description
          :textual-description-url="textualDescriptionUrl"
          :command-journey-id="commandJourneyId"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import type { SupportedWkid } from "@/coordinate-system-utils";
import { computed, CSSProperties, ref, watch } from "vue";
import { buildCommandJourneyUrl } from "@/command-journey-utils";
import ErrorSummary from "@/components/gdsComponents/error/ErrorSummary.vue";
import TextualDescription from "@/components/textualDescription/TextualDescription.vue";
import { useCommandJourneyFeatures } from "@/composables/useCommandJourneyFeatures";
import BaseMap from "../components/baseMap/BaseMap.vue";
import DepthSlider from "../components/depthSlider/DepthSlider.vue";

interface DepthMapPageProps {
  commandJourneyId: string,
  srsWkid: SupportedWkid,
  featuresBaseUrl: string,
  outlineNodesBaseUrl: string,
  textualDescriptionUrl: string,
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
}

const props = withDefaults(defineProps<DepthMapPageProps>(), {
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

function startDepthOf(feature: Feature<Geometry>): number {
  return Number(feature.get("startDepth") ?? 0);
}
function endDepthOf(feature: Feature<Geometry>): number {
  return Number(feature.get("endDepth") ?? Number.NEGATIVE_INFINITY);
}

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

const depths = computed<Map<number, string>>(() => {
  const starts = [...new Set(features.value.map(startDepthOf))].sort((a, b) => a - b);
  if (!starts.length) {
    return new Map([[0, "All depths"]]);
  }
  const bands = new Map<number, string>([[starts[0], `-infinity to ${starts[0]}`]]);
  for (let i = 1; i < starts.length; i++) {
    bands.set(starts[i], `${starts[i - 1]} to ${starts[i]}`);
  }
  return bands;
});

const currentDepthLevel = ref(0);

watch(depths, (band) => {
  currentDepthLevel.value = Math.max(...band.keys());
}, { immediate: true });

watch(hasError, (errored) => {
  if (errored) {
    error.value = "Unable to load the features.";
  }
});

const visibleFeatures = computed(() =>
  features.value.filter(feature =>
    endDepthOf(feature) < currentDepthLevel.value && currentDepthLevel.value <= startDepthOf(feature)));
const visiblePolygonIds = computed(() => new Set(visibleFeatures.value.map(f => f.get("polygonId"))));
const visibleOutlineNodes = computed(() =>
  outlineNodes.value
    .map(group => ({ ...group, nodes: group.nodes.filter(n => visiblePolygonIds.value.has(n.polygonId)) }))
    .filter(group => group.nodes.length > 0));
</script>

<style scoped>
.gis-depth-layout {
  display: flex;
  gap: 1rem;
  width: 100%;
}

.gis-depth-layout > :last-child {
  flex: 1 1 0;
  aspect-ratio: 1 / 1;
  min-width: 0;
}

.gis-depth-layout__side {
  min-width: 0;
  aspect-ratio: 1 / 1;
}

.gis-depth-layout__map {
  flex: 2 1 0;
  display: flex;
  flex-direction: row;
  gap: 1rem;
  align-items: flex-start;
}

.gis-depth-layout__map-canvas {
  flex: 1 1 0;
  min-width: 0;
  aspect-ratio: 1 / 1;
}

.gis-depth-layout__side {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  overflow: hidden;
}

.gis-depth-layout__description {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}
</style>
