<template>
  <ol-vector-layer ref="snapLayerRef">
    <ol-source-vector>
      <ol-feature v-for="point in snapPoints" :key="point.id">
        <ol-geom-point :coordinates="point.coordinates"/>
        <ol-style>
          <ol-style-circle :radius="3">
            <ol-style-fill color="grey"/>
          </ol-style-circle>
        </ol-style>
      </ol-feature>
    </ol-source-vector>
  </ol-vector-layer>
  <ol-overlay v-if="hoveredPoint" :position="hoveredPoint.coordinates" positioning="top-left" :offset="[8, 8]">
    <div class="snap-tooltip">
      {{ hoveredPoint.displayName }}
    </div>
  </ol-overlay>
</template>

<script setup lang="ts">
import type { Point } from "ol/geom";
import type Map from "ol/Map";
import type MapBrowserEvent from "ol/MapBrowserEvent";
import type { SupportedWkid } from "../../coordinate-system-utils";
import type { SnapPoint } from "../../grid-utils";
import { computed, ref, watch } from "vue";
import { debounce } from "../../debounce";
import { generateSnapPoints, getMinSnapZoom, getSpacingForZoom } from "../../grid-utils";

interface OpenLayersMapComponent {
  map?: Map,
}

interface SnapPointProps {
  olMap: OpenLayersMapComponent,
  srsWkid: SupportedWkid,
}

const props = defineProps<SnapPointProps>();
const emit = defineEmits<{ hoveredPointChange: [point: SnapPoint | undefined] }>();

const snapLayerRef = ref();
const snapPoints = ref<SnapPoint[]>([]);
const currentZoom = ref<number>(0);
const hoveredPoint = ref<SnapPoint>();

const computedSpacing = computed(() => {
  return getSpacingForZoom(currentZoom.value, props.srsWkid);
});

const HOVER_THRESHOLD_PX = 8;

watch(() => props.olMap?.map, (map, _oldMap, onCleanup) => {
  if (!map) {
    return;
  }

  const view = map.getView();
  regenerateSnapPoints();
  const debouncedRegenerate = debounce(regenerateSnapPoints, 100);
  view.on("change:resolution", debouncedRegenerate);
  view.on("change:center", debouncedRegenerate);

  const clearHovered = () => {
    if (hoveredPoint.value === undefined) {
      return;
    }
    hoveredPoint.value = undefined;
    emit("hoveredPointChange", undefined);
  };

  const pointerMoveHandler = (event: MapBrowserEvent) => {
    const pixel = event.pixel;
    const olLayer = snapLayerRef.value?.vectorLayer;
    // get features close to the mouse coordinates to avoid comparing against every snap point on the map
    const candidates = map.getFeaturesAtPixel(pixel, {
      hitTolerance: HOVER_THRESHOLD_PX,
      layerFilter: olLayer ? l => l === olLayer : undefined,
    });

    if (candidates.length === 0) {
      clearHovered();
      return;
    }

    let nearest: SnapPoint | undefined;
    let minDistSq = Infinity;

    for (const feature of candidates) {
      const coords = (feature.getGeometry() as Point | undefined)?.getCoordinates();
      if (!coords) {
        continue;
      }
      const pointPixel = map.getPixelFromCoordinate(coords);
      if (!pointPixel) {
        continue;
      }
      const dx = pixel[0] - pointPixel[0];
      const dy = pixel[1] - pointPixel[1];
      const distSq = dx * dx + dy * dy;
      if (distSq < minDistSq) {
        minDistSq = distSq;
        nearest = snapPoints.value.find(
          p => p.coordinates[0] === coords[0] && p.coordinates[1] === coords[1],
        );
      }
    }

    if (hoveredPoint.value === nearest) {
      // point hasn't changed, so don't emmit an event
      return;
    }
    hoveredPoint.value = nearest;
    emit("hoveredPointChange", nearest);
  };

  map.on("pointermove", pointerMoveHandler);
  map.getViewport().addEventListener("mouseleave", clearHovered);

  onCleanup(() => {
    view.un("change:resolution", debouncedRegenerate);
    view.un("change:center", debouncedRegenerate);
    map.un("pointermove", pointerMoveHandler);
    map.getViewport().removeEventListener("mouseleave", clearHovered);
    hoveredPoint.value = undefined;
  });
}, { immediate: true });

function regenerateSnapPoints(): void {
  const map = props.olMap?.map;
  if (!map) {
    return;
  }
  hoveredPoint.value = undefined;
  emit("hoveredPointChange", undefined);

  // Only generate points above this zoom level, to avoid running out of memory.
  const zoom = map.getView().getZoom();
  if (zoom === undefined || zoom < getMinSnapZoom(props.srsWkid)) {
    snapPoints.value = [];
    return;
  }

  currentZoom.value = zoom;

  // useGeographic() makes calculateExtent return WGS84 (EPSG:4326)
  const [wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat] = map.getView().calculateExtent(map.getSize());

  snapPoints.value = generateSnapPoints(
    wgs84MinLon,
    wgs84MinLat,
    wgs84MaxLon,
    wgs84MaxLat,
    props.srsWkid,
    computedSpacing.value,
  );
}
</script>

<style scoped>
.snap-tooltip {
  background: #484949;
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: "GDS Transport", sans-serif;
  font-size: 18px;
  white-space: pre-line;
}
</style>
