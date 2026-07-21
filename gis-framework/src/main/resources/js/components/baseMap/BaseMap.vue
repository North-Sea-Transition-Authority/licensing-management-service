<template>
  <ol-map ref="mapRef" :style="mapStyle" tabindex="0">
    <ol-view :center="[0, 0]" :zoom="2" :max-zoom="15"/>
    <ol-tile-layer>
      <ol-source-osm/>
    </ol-tile-layer>
    <nsta-quadrant-layer v-if="mapRef && includeNstaQuadrants" :ol-map="mapRef"/>
    <nsta-block-layer v-if="mapRef && includeNstaBlocks" :ol-map="mapRef"/>
    <snap-points-layer
      v-if="mapRef && includeSnapPoints"
      :ol-map="mapRef"
      :srs-wkid="srsWkid"
      @hovered-point-change="hoveredSnapPoint = $event"
    />
    <draw-line-layer
      v-if="mapRef && includeDrawLine"
      :ol-map="mapRef"
      :hovered-snap-point="hoveredSnapPoint"
      :require-orthogonal="true"
    />
    <feature-layer v-if="mapRef" :features-url="featuresUrl" :ol-map="mapRef"/>
    <node-numbering-layer :outline-nodes-url="outlineNodesUrl"/>
  </ol-map>
</template>

<script setup lang="ts">
import type OlMap from "vue3-openlayers/map/OlMap";
import type { SupportedWkid } from "../../coordinate-system-utils";
import type { SnapPoint } from "../../grid-utils";
import { useGeographic } from "ol/proj";
import { computed, CSSProperties, ref } from "vue";
import DrawLineLayer from "./DrawLineLayer.vue";
import FeatureLayer from "./FeatureLayer.vue";
import NodeNumberingLayer from "./NodeNumberingLayer.vue";
import NstaBlockLayer from "./NstaBlockLayer.vue";
import NstaQuadrantLayer from "./NstaQuadrantLayer.vue";
import SnapPointsLayer from "./SnapPointsLayer.vue";

interface BaseMapProps {
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
  featuresUrl: string,
  outlineNodesUrl: string,
  srsWkid: SupportedWkid,
  includeSnapPoints?: boolean,
  includeDrawLine?: boolean,
}

withDefaults(defineProps<BaseMapProps>(), {
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
  includeSnapPoints: true,
  includeDrawLine: true,
});

// Allow openLayers to receive features in WGS84
useGeographic();
const mapRef = ref<InstanceType<typeof OlMap> | null>(null);
const hoveredSnapPoint = ref<SnapPoint | undefined>(undefined);
const mapStyle = computed<CSSProperties>(() => ({
  width: "100%",
  maxWidth: "1000px",
  height: "clamp(18.75rem, 60vh, 31.25rem)",
  display: "block",
}));
</script>
