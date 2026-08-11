<template>
  <div>
    <single-error-summary :message="splitError"/>
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
  </div>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "../../coordinate-system-utils";
import type { LinePoint } from "../../grid-utils";
import { computed, ref, watch } from "vue";
import { splitFeature } from "../../api/split.api";
import BaseMap from "../baseMap/BaseMap.vue";
import SingleErrorSummary from "../SingleErrorSummary.vue";

interface SplitByPointAndClickPageProps {
  commandJourneyId: string,
  srsWkid: SupportedWkid,
  featuresBaseUrl: string,
  outlineNodesBaseUrl: string,
  splitUrl: string,
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

function buildFeatureIdsUrl(baseUrl: string, commandJourney: string): string {
  return `${baseUrl}/${commandJourney}`;
}

const featuresUrl = computed(() => buildFeatureIdsUrl(props.featuresBaseUrl, props.commandJourneyId));
const outlineNodesUrl = computed(() => buildFeatureIdsUrl(props.outlineNodesBaseUrl, props.commandJourneyId));

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
