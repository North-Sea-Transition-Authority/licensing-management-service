import type { Geometry } from "ol/geom";
import type Feature from "ol/Feature";
import { EsriJSON } from "ol/format";

const esriJson = new EsriJSON();

export interface JsonOutlineNode  {
  polygonId: string;
  lineId: string;
  ringNumber: number;
  displayOrder: number;
  x: number;
  y: number;
  mapText: string;
}

export interface JsonFeatureOutlineNodes  {
  featureId: string;
  nodes: JsonOutlineNode[];
}

export interface JsonFeatureOutlineNodesResponse  {
  featureOutlineNodes: JsonFeatureOutlineNodes[];
}

export async function getOutlineNodes(outlineNodesUrl: string): Promise<JsonFeatureOutlineNodes[]> {
  const response = await fetch(outlineNodesUrl);
  if (response.ok) {
    const body: JsonFeatureOutlineNodesResponse = await response.json();
    return body.featureOutlineNodes;
  } else {
    return Promise.reject(`Response status: ${response.statusText}`);
  }
}

export interface JsonFeaturesResponse {
  features: { attributes: { featureId: string; featureName: string }; geometry: unknown }[];
  spatialReference?: { wkid: number };
}

export async function getFeatures(featuresUrl: string): Promise<Feature<Geometry>[]> {
  const response = await fetch(featuresUrl);
  if (response.ok) {
    const body: JsonFeaturesResponse = await response.json();
    return esriJson.readFeatures(body, {
      dataProjection: "EPSG:4326",
      featureProjection: "EPSG:4326",
    }) as Feature<Geometry>[];
  } else {
    return Promise.reject(`Response status: ${response.statusText}`);
  }
}

export interface TextualDescriptionResponse {
  textualDescription: string;
}

export async function getTextualDescription(textualDescriptionUrl: string): Promise<string> {
  const response = await fetch(textualDescriptionUrl);
  if (response.ok) {
    const body: TextualDescriptionResponse = await response.json();
    return body.textualDescription;
  } else {
    return Promise.reject(`Response status: ${response.statusText}`);
  }
}
