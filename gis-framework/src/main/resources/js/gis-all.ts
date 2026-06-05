import { createApp } from "vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "./components/baseMap/BaseMap.vue";
import "ol/ol.css";
import "vue3-openlayers/vue3-openlayers.css";

for (const element of document.querySelectorAll<HTMLElement>("[data-gis-component='gis-base-map']")) {
  createApp(BaseMap, {
    includeNstaQuadrants: element.dataset.gisIncludeNstaQuadrants === "true",
    includeSnapPoints: element.dataset.gisIncludeSnapPoints === "true",
    featuresUrl: element.dataset.gisFeaturesUrl,
    srsWkid: Number(element.dataset.gisSrsWkid),
  })
    .use(OpenLayersMap)
    .mount(element);
}
