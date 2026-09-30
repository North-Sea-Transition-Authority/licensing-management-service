<template>
  <ol-vector-layer :declutter="true" :z-index="10">
    <ol-source-vector>
      <ol-feature v-for="point in textPoints" :key="point.id">
        <ol-geom-point :coordinates="point.coordinates" />
        <ol-style>
          <ol-style-circle :radius="6">
            <ol-style-fill color="black" />
          </ol-style-circle>
          <ol-style-text :text="point.text" :font="textPointFont" text-align="left" :offset-x="15">
            <ol-style-fill color="blue" />
            <ol-style-stroke color="white" :width="3" />
          </ol-style-text>
        </ol-style>
      </ol-feature>
    </ol-source-vector>
  </ol-vector-layer>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { JsonFeatureOutlineNodes } from "@/api/features.api";
import { jsonFeatureNodesToTextPoints } from "@/textual-description-utils";

interface Props {
  outlineNodes: JsonFeatureOutlineNodes[],
}

const props = defineProps<Props>();

const textPointFont = "18px \"GDS Transport\"";
const textPoints = computed(() => jsonFeatureNodesToTextPoints(props.outlineNodes));
</script>
