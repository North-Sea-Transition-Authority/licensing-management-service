<template>
  <ol-map ref="mapRef" class="gis-framework-map" :style="mapStyle" tabindex="0">
    <ol-view :center="[0, 0]" :zoom="2" :max-zoom="15"/>
    <ol-tile-layer>
      <ol-source-osm/>
    </ol-tile-layer>
  </ol-map>
  <nsta-quadrant-layer v-if="mapRef && includeNstaQuadrants" :ol-map="mapRef"/>
</template>

<script setup lang="ts">
import type { CSSProperties } from "vue";
import type OlMap from "vue3-openlayers/map/OlMap";
import { computed, ref } from "vue";
import NstaQuadrantLayer from "./NstaQuadrantLayer.vue";

interface BaseMapProps {
  includeNstaQuadrants?: boolean,
}

defineProps<BaseMapProps>();

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
