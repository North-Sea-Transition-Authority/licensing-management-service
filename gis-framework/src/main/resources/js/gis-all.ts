import {createApp} from "vue";
import OpenLayersMap from "vue3-openlayers";
import TestMap from "./components/test-map/TestMap.vue";
import "ol/ol.css";
import "vue3-openlayers/vue3-openlayers.css";

for (const element of document.querySelectorAll<HTMLElement>("[data-gis-component='test-map']")) {
  createApp(TestMap, {
    height: element.dataset.gisMapHeight || "500px",
  })
      .use(OpenLayersMap)
      .mount(element);
}
