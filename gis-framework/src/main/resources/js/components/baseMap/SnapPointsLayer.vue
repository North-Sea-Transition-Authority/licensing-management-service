<template>
  <ol-vector-layer>
    <ol-source-vector>
      <ol-feature v-for="point in snapPoints" :key="point.id">
        <ol-geom-point :coordinates="point.coordinates" />
        <ol-style>
          <ol-style-circle :radius="3">
            <ol-style-fill color="grey" />
          </ol-style-circle>
        </ol-style>
      </ol-feature>
    </ol-source-vector>
  </ol-vector-layer>
</template>

<script setup lang="ts">
import type Map from "ol/Map";
import type { SnapPoint } from "../../grid-utils";
import { ref, watch } from "vue";
import { SupportedWkid } from "../../coordinate-system-utils";
import { debounce } from "../../debounce";
import { generateSnapPoints } from "../../grid-utils";

interface OpenLayersMapComponent {
  map?: Map,
}

interface SnapPointProps {
  olMap: OpenLayersMapComponent,
  srsWkid: number,
  snapPointSpacing?: number,
}

const props = defineProps<SnapPointProps>();

const snapPoints = ref<SnapPoint[]>([]);

/**
 * Get the minimum zoom level at which snap points should be generated. Different zoom levels to accomodate
 * for different coordinate systems.
 */
function getMinSnapZoom(): number {
  if (props.srsWkid === SupportedWkid.ED50_WKID) {
    return 11;
  } else if (props.srsWkid === SupportedWkid.BNG_WKID) {
    return 12;
  }
  throw new Error(`Unsupported SRS WKID in SnapPointsLayer: ${props.srsWkid}`);
}

watch(() => props.olMap?.map, (map, _oldMap, onCleanup) => {
  if (!map) {
    return;
  }

  const view = map.getView();
  regenerateSnapPoints();
  const debouncedRegenerate = debounce(regenerateSnapPoints, 200);
  view.on("change:resolution", debouncedRegenerate);
  view.on("change:center", debouncedRegenerate);
  onCleanup(() => {
    view.un("change:resolution", debouncedRegenerate);
    view.un("change:center", debouncedRegenerate);
  });
}, { immediate: true });

watch(() => [props.srsWkid, props.snapPointSpacing], regenerateSnapPoints);

function regenerateSnapPoints(): void {
  const map = props.olMap?.map;
  if (!map) {
    return;
  }

  // Only generate points above this zoom level, to avoid running out of memory.
  const zoom = map.getView().getZoom();
  if (zoom === undefined || zoom < getMinSnapZoom()) {
    snapPoints.value = [];
    return;
  }

  // useGeographic() makes calculateExtent return WGS84 (EPSG:4326)
  const [wgs84MinLon, wgs84MinLat, wgs84MaxLon, wgs84MaxLat] = map.getView().calculateExtent(map.getSize());

  snapPoints.value = generateSnapPoints(
    wgs84MinLon,
    wgs84MinLat,
    wgs84MaxLon,
    wgs84MaxLat,
    props.srsWkid as SupportedWkid,
    props.snapPointSpacing,
  );
}
</script>
