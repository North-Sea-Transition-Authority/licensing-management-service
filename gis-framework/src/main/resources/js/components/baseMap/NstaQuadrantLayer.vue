<script setup lang="ts">
import type OlMap from "vue3-openlayers/map/OlMap";
import VectorLayer from "ol/layer/Vector";
import { Fill, Stroke, Style } from "ol/style";
import { onMounted } from "vue";
import { buildServiceUrl, createPaginatedVectorSource } from "../../nsta-data-source";

interface NstaQuadrantLayerProps {
  olMap: InstanceType<typeof OlMap>,
}

const props = defineProps<NstaQuadrantLayerProps>();

const quadrantStyle = new Style({
  fill: new Fill({ color: "rgba(0, 0, 0, 0)" }),
  stroke: new Stroke({ color: "rgba(0, 46, 109, 0.8)", width: 1.5 }),
});

const quadrantUrl = buildServiceUrl("UKCS_quadrants_(WGS84)", "QUADRANT");
const quadrantLayer = new VectorLayer({
  source: createPaginatedVectorSource(quadrantUrl),
  style: quadrantStyle,
  declutter: true,
});

onMounted(() => {
  props.olMap.map.addLayer(quadrantLayer);
});
</script>
