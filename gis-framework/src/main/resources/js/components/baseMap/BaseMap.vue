<template>
  <ol-map ref="mapRef" class="gis-framework-map" :style="mapStyle" tabindex="0">
    <ol-view :center="[0, 0]" :zoom="2" :max-zoom="15"/>
    <ol-tile-layer>
      <ol-source-osm/>
    </ol-tile-layer>
    <nsta-quadrant-layer v-if="mapRef && includeNstaQuadrants" :ol-map="mapRef"/>
    <snap-points-layer
      v-if="mapRef && includeSnapPoints"
      :ol-map="mapRef"
      :srs-wkid="srsWkid"
      :snap-point-spacing="snapPointSpacing"
    />
    <feature-layer v-if="mapRef" :features-url="featuresUrl" :ol-map="mapRef"/>
  </ol-map>
</template>

<script setup lang="ts">
import type { CSSProperties } from "vue";
import type OlMap from "vue3-openlayers/map/OlMap";
import { useGeographic } from "ol/proj";
import { computed, ref } from "vue";
import FeatureLayer from "./FeatureLayer.vue";
import NstaQuadrantLayer from "./NstaQuadrantLayer.vue";
import SnapPointsLayer from "./SnapPointsLayer.vue";

interface BaseMapProps {
  includeNstaQuadrants?: boolean,
  featuresUrl: string,
  srsWkid: number,
  includeSnapPoints?: boolean,
  snapPointSpacing?: number,
}

withDefaults(defineProps<BaseMapProps>(), {
  includeNstaQuadrants: true,
  includeSnapPoints: true,
});

// Allow openLayers to receive features in WGS84
useGeographic();
const mapRef = ref<InstanceType<typeof OlMap> | null>(null);
const mapStyle = computed<CSSProperties>(() => ({
  width: "100%",
  height: "100%",
  display: "block",
}));
</script>

<style scoped>
.gis-framework-map {
  width: 100%;
  height: 500px;
  display: block;
}
</style>
