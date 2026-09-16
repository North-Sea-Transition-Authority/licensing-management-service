export interface JsonHistoryStatus {
  canUndo: boolean;
  canRedo: boolean;
}

export async function getHistoryStatus(historyUrl: string): Promise<JsonHistoryStatus> {
  const response = await fetch(historyUrl);

  if (!response.ok) {
    return Promise.reject(`Response status: ${response.statusText}`);
  }

  return await response.json() as JsonHistoryStatus;
}
