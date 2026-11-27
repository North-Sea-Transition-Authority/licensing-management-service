<template>
  <ol-vector-layer :style="featureStyle" :declutter="true">
    <ol-source-vector
      ref="vectorSourceRef"
    />
  </ol-vector-layer>
</template>

<script setup lang="ts">
import type { EventsKey } from "ol/events";
import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";
import type Map from "ol/Map";
import type OlMap from "vue3-openlayers/map/OlMap";
import type OlSourceVector from "vue3-openlayers/sources/OlSourceVector";
import { unByKey } from "ol/Observable";
import { Fill, Stroke, Style, Text } from "ol/style";
import { onUnmounted, ref, watch } from "vue";

interface Props {
  features: Feature<Geometry>[],
  olMap: InstanceType<typeof OlMap>,
  fillColor?: [number, number, number],
  strokeColor?: [number, number, number, number],
  selectedFillColor?: [number, number, number],
  selectedStrokeColor?: [number, number, number, number],
  selectedFeatureIds?: string[],
  describedFeatureId?: string,
  refitOnFeaturesChange?: boolean,
  refitOnResize?: boolean,
}

const props = withDefaults(defineProps<Props>(), {
  fillColor: () => [255, 221, 0], // yellow
  strokeColor: () => [0, 0, 0, 1], // black
  selectedStrokeColor: () => [212, 53, 28, 1], // red
  selectedFillColor: () => [212, 53, 28], // red
  refitOnFeaturesChange: true,
});

const featureLabelFont = "18px \"GDS Transport\"";
const vectorSourceRef = ref<InstanceType<typeof OlSourceVector> | null>(null);

let sizeListenerKey: EventsKey | null = null;
// Owed on first load, and after every features change when refitOnFeaturesChange
let fitOwed = true;

watch(
  [() => props.features, vectorSourceRef],
  () => {
    const source = vectorSourceRef.value?.source;
    if (!source) {
      return;
    }
    source.clear();
    source.addFeatures(props.features);
    fitOwed ||= props.refitOnFeaturesChange;
    fitToFeatures(false);
  },
  { immediate: true },
);

watch(() => props.selectedFeatureIds, () => {
  vectorSourceRef.value?.source?.changed();
}, { deep: true });

function featureLabel(feature: Feature<Geometry>) {
  return new Text({
    text: feature.get("name") || feature.get("featureName") || "",
    font: featureLabelFont,
    fill: new Fill({ color: "#000" }),
    stroke: new Stroke({ color: "#fff", width: 3 }),
    overflow: true,
  });
}

function featureStyle(feature: Feature<Geometry>) {
  const featureId = feature.get("featureId");
  if (props.describedFeatureId && featureId !== props.describedFeatureId) {
    return new Style({
      zIndex: 0,
      stroke: new Stroke({ color: props.strokeColor, width: 2, lineDash: [6, 6] }),
      text: featureLabel(feature),
    });
  }
  const selected = props.selectedFeatureIds?.includes(featureId);
  return new Style({
    zIndex: 1,
    stroke: new Stroke({ color: selected ? props.selectedStrokeColor : props.strokeColor, width: selected ? 4 : 2 }),
    fill: new Fill({ color: selected ? [...props.selectedFillColor, 0.50] : [...props.fillColor, 0.50] }),
    text: featureLabel(feature),
  });
}

/**
 * Center the map on the extent of the features in the vector source, once the map has a size to fit into.
 */
function fitToFeatures(resized: boolean) {
  const map = props.olMap?.map;
  const extent = vectorSourceRef.value?.source?.getExtent();
  if (!map || !extent || !Number.isFinite(extent[0])) {
    return;
  }

  const sized = hasSize(map);
  if (props.refitOnResize || !sized) {
    sizeListenerKey ??= map.on("change:size", () => fitToFeatures(true));
  }

  if (sized && (fitOwed || (resized && props.refitOnResize))) {
    map.getView().fit(extent, {
      padding: [50, 50, 50, 50],
    });
    fitOwed = false;
  }
}

function hasSize(map: Map) {
  const size = map.getSize();
  return !!size && size[0] > 0 && size[1] > 0;
}

onUnmounted(() => {
  if (sizeListenerKey) {
    unByKey(sizeListenerKey);
  }
});
</script>
