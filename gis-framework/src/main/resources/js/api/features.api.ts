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

export interface CommandJourneyFeature {
  featureId: string;
  featureName: string;
}

interface JsonFeaturesResponse {
  features: { attributes: CommandJourneyFeature }[];
}

export async function getCommandJourneyFeatures(featuresUrl: string): Promise<CommandJourneyFeature[]> {
  const response = await fetch(featuresUrl);
  if (response.ok) {
    const body: JsonFeaturesResponse = await response.json();
    return [...new Map(body.features.map(feature =>
      [feature.attributes.featureId, feature.attributes])).values()
    ];
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
