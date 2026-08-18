import {createApp} from "vue";
import OpenLayersMap from "vue3-openlayers";
import BaseMap from "./components/baseMap/BaseMap.vue";
import TextualDescription from "./components/textualDescription/TextualDescription.vue";
import SplitByCoordinateEntryPage from "./components/coordinateInput/SplitByCoordinateEntryPage.vue";
import SplitByPointAndClickPage from "./components/splitByPointAndClick/SplitByPointAndClickPage.vue";
import MapWithTextualDescription from "./components/textualDescription/MapWithTextualDescription.vue";
import "ol/ol.css";
import "vue3-openlayers/vue3-openlayers.css";

for (const element of document.querySelectorAll<HTMLElement>("[data-gis-component='gis-base-map']")) {
  createApp(BaseMap, {
    includeNstaQuadrants: element.dataset.gisIncludeNstaQuadrants === "true",
    includeNstaBlocks: element.dataset.gisIncludeNstaBlocks === "true",
    includeSnapPoints: element.dataset.gisIncludeSnapPoints === "true",
    featuresUrl: element.dataset.gisFeaturesUrl,
    outlineNodesUrl: element.dataset.gisOutlineNodesUrl,
    srsWkid: Number(element.dataset.gisSrsWkid),
  })
    .use(OpenLayersMap)
    .mount(element);
}

for (const element of document.querySelectorAll<HTMLElement>("[data-gis-component='gis-textual-description']")) {
  createApp(TextualDescription, {
    textualDescriptionUrl: element.dataset.gisTextualDescriptionUrl,
  })
    .mount(element);
}

for (const element of document.querySelectorAll<HTMLElement>("[data-gis-component='gis-map-with-textual-description']")) {
  createApp(MapWithTextualDescription, {
    layout: element.dataset.gisLayout === "vertical" ? "vertical" : "horizontal",
    featuresUrl: element.dataset.gisFeaturesUrl,
    outlineNodesUrl: element.dataset.gisOutlineNodesUrl,
    textualDescriptionUrl: element.dataset.gisTextualDescriptionUrl,
    srsWkid: Number(element.dataset.gisSrsWkid),
    includeNstaQuadrants: element.dataset.gisIncludeNstaQuadrants === "true",
    includeNstaBlocks: element.dataset.gisIncludeNstaBlocks === "true",
  })
    .use(OpenLayersMap)
    .mount(element);
}

for (const element of document.querySelectorAll<HTMLElement>("[data-gis-component='gis-split-by-coordinate-entry']")) {
  createApp(SplitByCoordinateEntryPage, {
    srsWkid: Number(element.dataset.gisSrsWkid),
    coordinatePrecision: element.dataset.gisPrecision !== undefined ? Number(element.dataset.gisPrecision) : undefined,
    featuresUrl: element.dataset.gisFeaturesUrl,
    outlineNodesUrl: element.dataset.gisOutlineNodesUrl,
    includeNstaQuadrants: element.dataset.gisIncludeNstaQuadrants === "true",
    includeNstaBlocks: element.dataset.gisIncludeNstaBlocks === "true",
  })
    .use(OpenLayersMap)
    .mount(element);
}

for (const element of document.querySelectorAll<HTMLElement>("[data-gis-component='gis-split-by-point-and-click']")) {
  createApp(SplitByPointAndClickPage, {
    commandJourneyId: element.dataset.gisCommandJourneyId,
    srsWkid: Number(element.dataset.gisSrsWkid),
    featuresBaseUrl: element.dataset.gisFeaturesBaseUrl,
    outlineNodesBaseUrl: element.dataset.gisOutlineNodesBaseUrl,
    splitUrl: element.dataset.gisSplitUrl,
    historyBaseUrl: element.dataset.gisHistoryBaseUrl,
    undoBaseUrl: element.dataset.gisUndoBaseUrl,
    csrfHeaderName: element.dataset.gisCsrfHeaderName,
    csrfToken: element.dataset.gisCsrfToken,
    includeNstaQuadrants: element.dataset.gisIncludeNstaQuadrants === "true",
    includeNstaBlocks: element.dataset.gisIncludeNstaBlocks === "true",
  })
    .use(OpenLayersMap)
    .mount(element);
}
