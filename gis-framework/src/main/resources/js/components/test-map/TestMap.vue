<template>
  <ol-map ref="mapRef" class="gis-framework-map" :style="mapStyle" tabindex="0">
    <ol-view :center="[0, 0]" :zoom="2" :maxZoom="15"/>
    <ol-tile-layer>
      <ol-source-osm/>
    </ol-tile-layer>
  </ol-map>
  <div  class="govuk-button-group govuk-!-margin-top-4">
    <button class="govuk-button">Split</button>
    <button class="govuk-button govuk-button--secondary">Undo change</button>
    <button class="govuk-button govuk-button--secondary">Redo change</button>
  </div>
</template>

<script setup lang="ts">
import type {CSSProperties} from "vue";
import {computed, ref} from "vue";

interface TestMapProps {
  height?: string;
}

const props = withDefaults(defineProps<TestMapProps>(), {
  height: "500px",
});

const mapRef = ref<unknown>(null);
const mapStyle = computed<CSSProperties>(() => ({
  width: "100%",
  height: props.height,
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
