<template>
  <div>
    <details-component summary="How can I merge blocks?">
      <p class="govuk-body">
        Select two or more features using the checkboxes, then click merge to combine them into a single
        feature.
      </p>
      <p class="govuk-body">
        If you made a mistake, you can use the undo/redo buttons to fix it.
      </p>
    </details-component>
    <error-summary v-if="mergeError" :description="mergeError"/>
    <div class="gis-merge-layout">
      <div class="gis-merge-layout__map">
        <base-map
          :srs-wkid="srsWkid"
          :features="features"
          :outline-nodes="outlineNodes"
          :include-nsta-quadrants="includeNstaQuadrants"
          :include-nsta-blocks="includeNstaBlocks"
          :include-snap-points="false"
          :include-draw-line="false"
          :refresh-counter="refreshCounter"
          :map-style-override="mapStyleOverride"
          :selected-feature-ids="selectedFeatureIds"
        />
      </div>
      <div class="gis-merge-layout__side">
        <div class="gis-merge-layout__description">
          <textual-description
            :textual-description-url="textualDescriptionUrl"
            :command-journey-id="commandJourneyId"
            :refresh-counter="refreshCounter"
          />
        </div>
      </div>
    </div>
    <gv-checkboxes
      v-model="selectedFeatureIds"
      legend="Select the features to merge"
      legend-class="govuk-fieldset__legend--s"
      form-group-class="govuk-!-margin-top-4"
    >
      <gv-checkbox
        v-for="feature in sortedFeatures"
        :key="feature.featureId"
        :value="feature.featureId"
        :label="feature.featureName"
      />
    </gv-checkboxes>
    <merge-actions
      :feature-ids="selectedFeatureIds"
      :merge-url="mergeUrl"
      :command-journey-id="commandJourneyId"
      :refresh-counter="refreshCounter"
      :base-url="baseUrl"
      :csrf-header-name="csrfHeaderName"
      :csrf-token="csrfToken"
      @action-success="onMergeSuccess"
      @action-error="mergeError = $event"
    />
  </div>
</template>

<script setup lang="ts">
import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import type { SupportedWkid } from "@/coordinate-system-utils";
import { computed, CSSProperties, ref, watch } from "vue";
import { buildCommandJourneyUrl } from "@/command-journey-utils";
import { useCommandJourneyFeatures } from "@/composables/useCommandJourneyFeatures";
import BaseMap from "../components/baseMap/BaseMap.vue";
import ErrorSummary from "../components/gdsComponents/error/ErrorSummary.vue";
import GvCheckbox from "../components/govukVue/checkboxes/GvCheckbox.vue";
import GvCheckboxes from "../components/govukVue/checkboxes/GvCheckboxes.vue";
import DetailsComponent from "../components/govukVue/details/GvDetails.vue";
import MergeActions from "../components/merge/MergeActions.vue";
import TextualDescription from "../components/textualDescription/TextualDescription.vue";

interface MergePageProps {
  commandJourneyId: string,
  srsWkid: SupportedWkid,
  featuresBaseUrl: string,
  outlineNodesBaseUrl: string,
  mergeUrl: string,
  baseUrl: string,
  textualDescriptionUrl: string,
  csrfHeaderName: string,
  csrfToken: string,
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
}

interface CommandJourneyFeature {
  featureId: string,
  featureName: string,
}

const props = withDefaults(defineProps<MergePageProps>(), {
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

function getString(feature: Feature<Geometry>, key: string): string {
  return feature.get(key);
}

function toFeatureOptions(features: Feature<Geometry>[]): CommandJourneyFeature[] {
  return [...new Map(features.map(feature =>
    [getString(feature, "featureId"), {
      featureId: getString(feature, "featureId"),
      featureName: getString(feature, "featureName"),
    }])).values()];
}

// Make the map fill its panel rather than use BaseMap's default clamped height.
const mapStyleOverride: CSSProperties = {
  width: "100%",
  height: "100%",
  display: "block",
};

const selectedFeatureIds = ref<string[]>([]);
const mergeError = ref<string | null>(null);
const refreshCounter = ref(0);

const featuresUrl = buildCommandJourneyUrl(props.featuresBaseUrl, props.commandJourneyId);
const outlineNodesUrl = buildCommandJourneyUrl(props.outlineNodesBaseUrl, props.commandJourneyId);
const { features, outlineNodes, hasError } = useCommandJourneyFeatures(featuresUrl, outlineNodesUrl, refreshCounter);
const sortedFeatures = computed<CommandJourneyFeature[]>(() =>
  toFeatureOptions(features.value)
    .sort((a, b) => a.featureName.localeCompare(b.featureName, undefined, { numeric: true })),
);

watch(hasError, (errored) => {
  if (errored) {
    mergeError.value = "Unable to load the features to merge.";
  }
});

function onMergeSuccess() {
  mergeError.value = null;
  selectedFeatureIds.value = [];
  refreshCounter.value++;
}
</script>

<style scoped>
.gis-merge-layout {
  display: flex;
  gap: 1rem;
  width: 100%;
}

.gis-merge-layout__map,
.gis-merge-layout__side {
  min-width: 0;
  aspect-ratio: 1 / 1;
}

.gis-merge-layout__map {
  flex: 2 1 0;
}

.gis-merge-layout__side {
  flex: 1 1 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  overflow: hidden;
}

.gis-merge-layout__description {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}
</style>
