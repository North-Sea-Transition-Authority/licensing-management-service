export interface JsonSplitHistoryStatus {
  canUndo: boolean;
  canRedo: boolean;
}

export async function getSplitHistoryStatus(historyUrl: string): Promise<JsonSplitHistoryStatus> {
  const response = await fetch(historyUrl);

  if (!response.ok) {
    return Promise.reject(`Response status: ${response.statusText}`);
  }

  return await response.json() as JsonSplitHistoryStatus;
}
