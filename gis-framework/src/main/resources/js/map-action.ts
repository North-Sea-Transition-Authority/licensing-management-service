export const MapAction = {
  SPLIT: "SPLIT",
  ADD_STRATA: "ADD_STRATA",
  MERGE: "MERGE",
  RENAME: "RENAME",
} as const;

export type MapAction = typeof MapAction[keyof typeof MapAction];

export const mapActionLabels: Record<MapAction, string> = {
  SPLIT: "Split",
  ADD_STRATA: "Add strata",
  MERGE: "Merge",
  RENAME: "Rename",
};

export function getAvailableMapActions(canMerge: boolean, canAddStrata: boolean): MapAction[] {
  const isAvailable: Record<MapAction, boolean> = {
    SPLIT: true,
    ADD_STRATA: canAddStrata,
    MERGE: canMerge,
    RENAME: true,
  };
  return Object.values(MapAction)
    .filter(action => isAvailable[action])
    .sort((a, b) => mapActionLabels[a].localeCompare(mapActionLabels[b]));
}

export interface MapActionValidationError {
  message: string,
  fieldId: string,
}
