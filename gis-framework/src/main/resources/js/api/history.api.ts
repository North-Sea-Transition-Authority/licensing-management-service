import type { TransformationType } from "@/transformation-type";

export interface JsonHistoryStatus {
  undoTransformationType: TransformationType | null;
  redoTransformationType: TransformationType | null;
  actions: JsonHistoryAction[];
}

export interface JsonHistoryAction {
  number: number;
  transformationType: TransformationType;
  inputFeatureNames: string[];
  outputFeatureNames: string[];
}

export async function getHistoryStatus(historyUrl: string): Promise<JsonHistoryStatus> {
  const response = await fetch(historyUrl);

  if (!response.ok) {
    return Promise.reject(`Response status: ${response.statusText}`);
  }

  return await response.json() as JsonHistoryStatus;
}
