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

export interface MapActionValidationError {
  message: string,
  fieldId: string,
}
