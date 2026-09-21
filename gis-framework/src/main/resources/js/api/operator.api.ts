import type {LinePoint} from "@/grid-utils";

export interface JsonSplitResponse {
  outputFeatureIds: string[];
}

export interface JsonMergeResponse {
  outputFeatureId: string;
}

export async function splitFeature(
  splitUrl: string,
  points: LinePoint[],
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
): Promise<JsonSplitResponse> {
  const cutterLineOriginalSrsCoordinates: [number, number][][] = [];
  for (let i = 0; i < points.length - 1; i++) {
    cutterLineOriginalSrsCoordinates.push([points[i].originalSrsCoordinates, points[i + 1].originalSrsCoordinates]);
  }

  const response = await fetch(splitUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json", [csrfHeaderName]: csrfToken },
    body: JSON.stringify({ cutterLineOriginalSrsCoordinates, commandJourneyId }),
  });

  if (!response.ok) {
    return Promise.reject(`Response status: ${response.statusText}`);
  }

  return await response.json() as JsonSplitResponse;
}

export async function mergeFeatures(
  mergeUrl: string,
  featureIds: string[],
  commandJourneyId: string,
  csrfHeaderName: string,
  csrfToken: string,
): Promise<JsonMergeResponse> {
  const response = await fetch(mergeUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json", [csrfHeaderName]: csrfToken },
    body: JSON.stringify({ featureIds, commandJourneyId }),
  });
  if (!response.ok) return Promise.reject(`Response status: ${response.statusText}`);
  return await response.json() as JsonMergeResponse;
}

export async function undo(
  undoUrl: string,
  csrfHeaderName: string,
  csrfToken: string,
): Promise<void> {
  const response = await fetch(undoUrl, {
    method: "POST",
    headers: { [csrfHeaderName]: csrfToken },
  });

  if (!response.ok) {
    return Promise.reject(`Response status: ${response.statusText}`);
  }
}

export async function redo(
  redoUrl: string,
  csrfHeaderName: string,
  csrfToken: string,
): Promise<void> {
  const response = await fetch(redoUrl, {
    method: "POST",
    headers: { [csrfHeaderName]: csrfToken },
  });

  if (!response.ok) {
    return Promise.reject(`Response status: ${response.statusText}`);
  }
}
