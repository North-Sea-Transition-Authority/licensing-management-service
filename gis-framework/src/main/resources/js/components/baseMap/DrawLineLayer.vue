<script setup lang="ts">
import type { FeatureLike } from "ol/Feature";
import type Map from "ol/Map";
import type { LinePoint } from "../../grid-utils";
import Feature from "ol/Feature";
import LineString from "ol/geom/LineString";
import MultiPoint from "ol/geom/MultiPoint";
import VectorLayer from "ol/layer/Vector";
import VectorSource from "ol/source/Vector";
import { Circle as CircleStyle, Fill, Stroke, Style } from "ol/style";
import { onMounted, onUnmounted, ref, watchEffect } from "vue";
import { isOrthogonalSegment } from "../../draw-line-utils";

interface DrawLineLayerProps {
  olMap: { map?: Map },
  hoveredSnapPoint?: LinePoint,
  requireOrthogonal?: boolean,
  // When provided, the line is drawn from these points (controlled mode, e.g. coordinate entry)
  // instead of from points selected by clicking snap points on the map.
  points?: LinePoint[],
}

const props = withDefaults(defineProps<DrawLineLayerProps>(), {
  requireOrthogonal: false,
});

const selectedPoints = ref<LinePoint[]>([]);

const vectorSource = new VectorSource();
const lineFeature = new Feature({ geometry: new LineString([]) });
const previewFeature = new Feature({ geometry: new LineString([]) });
const markersFeature = new Feature({ geometry: new MultiPoint([]) });
const lastMarkerFeature = new Feature({ geometry: new MultiPoint([]) });
lineFeature.set("type", "line");
previewFeature.set("type", "preview");
markersFeature.set("type", "marker");
lastMarkerFeature.set("type", "lastMarker");
vectorSource.addFeatures([lineFeature, previewFeature, markersFeature, lastMarkerFeature]);

const gdsRed = "#ca3535";
const gdsBlue = "#1d70b8";

const vectorLayer = new VectorLayer({
  source: vectorSource,
  style: (feature: FeatureLike) => {
    switch (feature.get("type")) {
      case "line":
        return new Style({ stroke: new Stroke({ color: gdsRed, width: 2 }) });
      case "preview":
        return new Style({ stroke: new Stroke({ color: gdsRed, width: 2, lineDash: [5, 5] }) });
      case "marker":
        return new Style({
          image: new CircleStyle({
            radius: 5,
            fill: new Fill({ color: gdsBlue }),
            stroke: new Stroke({ color: "white", width: 1.5 }),
          }),
        });
      case "lastMarker":
        return new Style({
          image: new CircleStyle({
            radius: 6,
            fill: new Fill({ color: "white" }),
            stroke: new Stroke({ color: gdsRed, width: 2 }),
          }),
        });
      default:
        throw new Error("Unknown feature type");
    }
  },
});

function addPoint(point: LinePoint): void {
  selectedPoints.value = [...selectedPoints.value, point];
}

function handleClick(): void {
  const point = props.hoveredSnapPoint;
  if (!point) {
    return;
  }
  const last = selectedPoints.value.at(-1);
  if (last && props.requireOrthogonal && !isOrthogonalSegment(last, point)) {
    return;
  }
  addPoint(point);
}

onMounted(() => {
  const map = props.olMap.map;
  if (!map) {
    return;
  }
  map.addLayer(vectorLayer);
  map.on("singleclick", handleClick);
});

onUnmounted(() => {
  const map = props.olMap.map;
  if (!map) {
    return;
  }
  map.un("singleclick", handleClick);
  map.removeLayer(vectorLayer);
});

watchEffect(() => {
  const activePoints = props.points ?? selectedPoints.value;
  const coords = activePoints
    .map(p => p.coordinates)
    .filter(([x, y]) => Number.isFinite(x) && Number.isFinite(y));
  (lineFeature.getGeometry() as LineString).setCoordinates(coords.length >= 2 ? coords : []);
  (markersFeature.getGeometry() as MultiPoint).setCoordinates(coords.slice(0, -1));
  (lastMarkerFeature.getGeometry() as MultiPoint).setCoordinates(coords.length > 0 ? [coords[coords.length - 1]] : []);
});

watchEffect(() => {
  const hovered = props.hoveredSnapPoint;
  const last = selectedPoints.value.at(-1);
  const valid = !props.requireOrthogonal || (last !== undefined && hovered !== undefined && isOrthogonalSegment(last, hovered));
  (previewFeature.getGeometry() as LineString).setCoordinates(
    hovered && last && valid ? [last.coordinates, hovered.coordinates] : [],
  );
});

defineExpose({ addPoint, selectedPoints });
</script>
