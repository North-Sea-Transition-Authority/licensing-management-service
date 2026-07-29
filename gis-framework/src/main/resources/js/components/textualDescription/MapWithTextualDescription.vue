<template>
  <div class="gis-map-with-textual-description" :class="`gis-map-with-textual-description--${layout}`">
    <div class="gis-map-with-textual-description__panel">
      <base-map
        :srs-wkid="srsWkid"
        :features-url="featuresUrl"
        :outline-nodes-url="outlineNodesUrl"
        :include-nsta-quadrants="includeNstaQuadrants"
        :include-nsta-blocks="includeNstaBlocks"
        :include-snap-points="false"
        :include-draw-line="false"
        :map-style-override="mapStyleOverride"
      />
    </div>
    <div class="gis-map-with-textual-description__panel gis-map-with-textual-description__panel--description">
      <textual-description :textual-description-url="textualDescriptionUrl"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "../../coordinate-system-utils";
import { CSSProperties } from "vue";
import BaseMap from "../baseMap/BaseMap.vue";
import TextualDescription from "./TextualDescription.vue";

interface MapWithTextualDescriptionProps {
  // "horizontal" places the description to the right of the map; "vertical" places it underneath.
  layout?: "horizontal" | "vertical",
  srsWkid: SupportedWkid,
  featuresUrl: string,
  outlineNodesUrl: string,
  textualDescriptionUrl: string,
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
}

withDefaults(defineProps<MapWithTextualDescriptionProps>(), {
  layout: "horizontal",
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

// Make the map fill its square panel rather than use BaseMap's default clamped height.
const mapStyleOverride: CSSProperties = {
  width: "100%",
  height: "100%",
  display: "block",
};
</script>

<style scoped>
.gis-map-with-textual-description {
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
</style>
