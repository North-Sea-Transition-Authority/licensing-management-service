<template>
  <ol-vector-layer :style="featureStyle" :declutter="true">
    <ol-source-vector
      ref="vectorSourceRef"
      :url="featuresUrl"
      :format="esriJson"
      @featuresloadend="fitToExtent"
    />
  </ol-vector-layer>
</template>

<script setup lang="ts">
import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import type { VectorSourceEvent } from "ol/source/Vector";
import type OlMap from "vue3-openlayers/map/OlMap";
import type OlSourceVector from "vue3-openlayers/sources/OlSourceVector";
import { EsriJSON } from "ol/format";
import { Fill, Stroke, Style, Text } from "ol/style";
import { ref } from "vue";

interface Props {
  featuresUrl: string,
  olMap: InstanceType<typeof OlMap>,
  fillColor?: [number, number, number],
  strokeColor?: [number, number, number, number],
}

const props = withDefaults(defineProps<Props>(), {
  fillColor: () => [255, 221, 0], // yellow
  strokeColor: () => [0, 0, 0, 1], // black
});

const esriJson = new EsriJSON();
const featureLabelFont = "18px \"GDS Transport\"";
const vectorSourceRef = ref<InstanceType<typeof OlSourceVector> | null>(null);

function featureStyle(feature: Feature<Geometry>) {
  return new Style({
    stroke: new Stroke({
      color: props.strokeColor,
      width: 2,
    }),
    fill: new Fill({
      color: [...props.fillColor, 0.50],
    }),
    text: new Text({
      text: feature.get("featureName") || "",
      font: featureLabelFont,
      fill: new Fill({ color: "#000" }),
      stroke: new Stroke({ color: "#fff", width: 3 }),
      overflow: true,
    }),
  });
}

/**
 * Center the map on the extent of the features loaded from the vector source.
 */
function fitToExtent(event: VectorSourceEvent<Feature<Geometry>>) {
  const map = props.olMap?.map;
  if (!map) {
    return;
  }

  const source = event.target;
  const extent = source.getExtent();

  if (extent && Number.isFinite(extent[0])) {
    map.getView().fit(extent, {
      padding: [50, 50, 50, 50],
    });
  }
}
</script>
