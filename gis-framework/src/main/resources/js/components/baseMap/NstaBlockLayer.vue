<script setup lang="ts">
import type OlMap from "vue3-openlayers/map/OlMap";
import VectorLayer from "ol/layer/Vector";
import { Fill, Stroke, Style } from "ol/style";
import { onMounted } from "vue";
import { buildServiceUrl, createPaginatedVectorSource } from "../../nsta-data-source";

interface NstaBlockLayerProps {
  olMap: InstanceType<typeof OlMap>,
}

const props = defineProps<NstaBlockLayerProps>();

const blockStyle = new Style({
  fill: new Fill({ color: "rgba(0, 0, 0, 0)" }),
  stroke: new Stroke({ color: "rgba(0, 46, 109, 0.8)", width: 0.5 }),
});

const blockUrl = buildServiceUrl("UKCS_blocks_(WGS84)", "BLOCK_REF");
const blockLayer = new VectorLayer({
  source: createPaginatedVectorSource(blockUrl),
  style: blockStyle,
  declutter: true,
});

onMounted(() => {
  props.olMap.map.addLayer(blockLayer);
});
</script>
