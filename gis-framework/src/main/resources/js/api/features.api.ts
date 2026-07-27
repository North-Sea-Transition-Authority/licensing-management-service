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
