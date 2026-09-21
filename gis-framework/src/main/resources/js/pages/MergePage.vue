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
          :features-url="featuresUrl"
          :outline-nodes-url="outlineNodesUrl"
          :include-nsta-quadrants="includeNstaQuadrants"
          :include-nsta-blocks="includeNstaBlocks"
          :include-snap-points="false"
          :include-draw-line="false"
          :refresh-counter="refreshCounter"
          :map-style-override="mapStyleOverride"
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
        v-for="feature in features"
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
import type { CommandJourneyFeature } from "@/api/features.api";
import type { SupportedWkid } from "@/coordinate-system-utils";
import { computed, CSSProperties, onMounted, ref, watch } from "vue";
import { getCommandJourneyFeatures } from "@/api/features.api";
import { buildCommandJourneyUrl } from "@/command-journey-utils";
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

const props = withDefaults(defineProps<MergePageProps>(), {
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

const features = ref<CommandJourneyFeature[]>([]);
const selectedFeatureIds = ref<string[]>([]);
const mergeError = ref<string | null>(null);
const refreshCounter = ref(0);

// Make the map fill its panel rather than use BaseMap's default clamped height.
const mapStyleOverride: CSSProperties = {
  width: "100%",
  height: "100%",
  display: "block",
};

const featuresUrl = computed(() => buildCommandJourneyUrl(props.featuresBaseUrl, props.commandJourneyId));
const outlineNodesUrl = computed(() => buildCommandJourneyUrl(props.outlineNodesBaseUrl, props.commandJourneyId));

async function loadFeatures() {
  try {
    features.value = await getCommandJourneyFeatures(featuresUrl.value);
  } catch {
    mergeError.value = "Unable to load the features to merge.";
  }
}

onMounted(loadFeatures);
watch(refreshCounter, loadFeatures);

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
