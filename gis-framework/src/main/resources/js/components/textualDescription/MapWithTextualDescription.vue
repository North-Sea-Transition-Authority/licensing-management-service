<template>
  <div>
    <div class="gis-map-with-textual-description" :class="`gis-map-with-textual-description--${layout}`">
      <div v-if="view !== 'textual-description'" class="gis-map-with-textual-description__panel">
        <base-map
          :srs-wkid="srsWkid"
          :features="features"
          :outline-nodes="outlineNodes"
          :include-nsta-quadrants="includeNstaQuadrants"
          :include-nsta-blocks="includeNstaBlocks"
          :include-snap-points="false"
          :include-draw-line="false"
          :map-style-override="mapStyleOverride"
          :selected-feature-ids="selectedFeatureIds"
          :described-feature-id="describedFeatureId"
          :refit-on-resize="toggle"
        />
      </div>
      <div
        v-if="view !== 'map'"
        class="gis-map-with-textual-description__panel gis-map-with-textual-description__panel--description"
      >
        <textual-description :textual-description-url="textualDescriptionUrl"/>
      </div>
    </div>
    <gv-radios
      v-if="toggle"
      v-model="view"
      direction="inline"
      size="small"
      legend="Show"
      legend-class="govuk-visually-hidden"
      form-group-class="govuk-!-margin-top-4 govuk-!-margin-bottom-0"
    >
      <gv-radio value="map" label="Map"/>
      <gv-radio value="textual-description" label="Description"/>
      <gv-radio value="map-and-textual-description" label="Map and description"/>
    </gv-radios>
  </div>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "@/coordinate-system-utils";
import { CSSProperties, onMounted, onUnmounted, ref } from "vue";
import { useCommandJourneyFeatures } from "@/composables/useCommandJourneyFeatures";
import BaseMap from "../baseMap/BaseMap.vue";
import GvRadio from "../govukVue/radios/GvRadio.vue";
import GvRadios from "../govukVue/radios/GvRadios.vue";
import TextualDescription from "./TextualDescription.vue";

interface MapWithTextualDescriptionProps {
  // "horizontal" places the description to the right of the map; "vertical" places it underneath.
  layout?: "horizontal" | "vertical",
  // Shows Map / Description / Map and description radios, starting on the map only.
  toggle?: boolean,
  srsWkid: SupportedWkid,
  featuresUrl: string,
  outlineNodesUrl: string,
  textualDescriptionUrl: string,
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
  // Name of host-page checkboxes whose checked values are feature ids to highlight on the map.
  selectionInputName?: string,
  describedFeatureId?: string,
}

const props = withDefaults(defineProps<MapWithTextualDescriptionProps>(), {
  layout: "horizontal",
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

type View = "map" | "textual-description" | "map-and-textual-description";

const view = ref<View>(props.toggle ? "map" : "map-and-textual-description");

// Make the map fill its square panel rather than use BaseMap's default clamped height.
const mapStyleOverride: CSSProperties = {
  width: "100%",
  height: "100%",
  display: "block",
};

const { features, outlineNodes } = useCommandJourneyFeatures(props.featuresUrl, props.outlineNodesUrl);

const selectedFeatureIds = ref<string[]>([]);

function readSelectedFeatureIds(inputName: string) {
  selectedFeatureIds.value = [...document.querySelectorAll<HTMLInputElement>(
    `input[name="${CSS.escape(inputName)}"]:checked`,
  )].map(input => input.value);
}

function onChange(event: Event) {
  const inputName = (event.target as HTMLInputElement | null)?.name;
  if (inputName && inputName === props.selectionInputName) {
    readSelectedFeatureIds(inputName);
  }
}

onMounted(() => {
  if (props.selectionInputName) {
    readSelectedFeatureIds(props.selectionInputName);
    document.addEventListener("change", onChange);
  }
});

onUnmounted(() => document.removeEventListener("change", onChange));
</script>

<style scoped>
.gis-map-with-textual-description {
  container-type: inline-size;
  display: flex;
  gap: 1rem;
  width: 100%;
}

/* Description to the right of the map: two equal squares side by side, filling the width. */
.gis-map-with-textual-description--horizontal {
  flex-direction: row;
}

.gis-map-with-textual-description--horizontal .gis-map-with-textual-description__panel {
  flex: 1 1 0;
}

/* Description underneath the map: two equal squares stacked, each filling the width. */
.gis-map-with-textual-description--vertical {
  flex-direction: column;
}

.gis-map-with-textual-description--vertical .gis-map-with-textual-description__panel {
  width: 100%;
}

.gis-map-with-textual-description__panel {
  aspect-ratio: 1 / 1;
  min-width: 0;
}

/* Long descriptions scroll within the square instead of stretching it. */
.gis-map-with-textual-description__panel--description {
  overflow: auto;
}

/* On its own, the description shrinks to fit its text, up to the size the map would take. */
.gis-map-with-textual-description__panel--description:only-child {
  aspect-ratio: auto;
  max-height: 100cqw;
}

/* govuk-frontend sizes labels for normal radios, which wraps small inline labels */
:deep(.govuk-radios--small .govuk-radios__label) {
  max-width: calc(100% - 50px);
}
</style>
