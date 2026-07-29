<template>
  <div class="govuk-grid-row">
    <div class="govuk-grid-column-one-half">
      <coordinate-list v-model="points" :srs-wkid="srsWkid" :coordinate-precision="coordinatePrecision"/>
    </div>
    <div class="govuk-grid-column-one-half">
      <base-map
        :srs-wkid="srsWkid"
        :features-url="featuresUrl"
        :outline-nodes-url="outlineNodesUrl"
        :include-nsta-quadrants="includeNstaQuadrants"
        :include-nsta-blocks="includeNstaBlocks"
        :include-snap-points="false"
        :include-draw-line="true"
        :selected-points="linePoints"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "../../coordinate-system-utils";
import type { LinePoint } from "../../grid-utils";
import type { EditablePoint } from "./CoordinateList.vue";
import { computed, ref } from "vue";
import BaseMap from "../baseMap/BaseMap.vue";
import CoordinateList from "./CoordinateList.vue";

interface SplitByCoordinateEntryPageProps {
  srsWkid: SupportedWkid,
  featuresUrl: string,
  outlineNodesUrl: string,
  coordinatePrecision?: number,
  includeNstaQuadrants?: boolean,
  includeNstaBlocks?: boolean,
}

withDefaults(defineProps<SplitByCoordinateEntryPageProps>(), {
  coordinatePrecision: 4,
  includeNstaQuadrants: true,
  includeNstaBlocks: true,
});

function createInitialPoint(): EditablePoint {
  return {
    id: 0,
    originalSrsCoordinates: [Number.NaN, Number.NaN],
    coordinates: undefined,
  };
}

const points = ref<EditablePoint[]>([createInitialPoint()]);

const linePoints = computed<LinePoint[]>(() =>
  points.value
    .filter((point): point is EditablePoint & { coordinates: [number, number] } => point.coordinates !== undefined)
    .map(point => ({ coordinates: point.coordinates, originalSrsCoordinates: point.originalSrsCoordinates })),
);
</script>
