// Values match the Java TransformationType enum, which is persisted by name.
const transformationTypes = {
  SPLIT: "Split",
  STRATA_SPLIT: "Add strata",
  MERGE: "Merge",
  RENAME: "Rename",
};

const TRANSFORMATION_TYPES = Object.keys(transformationTypes) as Array<TransformationType>;

export type TransformationType = keyof typeof transformationTypes;

export function getTransformationTypeLabel(transformationType: TransformationType) {
  return transformationTypes[transformationType];
}

export function getAvailableTransformationTypes(canMerge: boolean, canAddStrata: boolean): TransformationType[] {
  const isAvailable: Record<TransformationType, boolean> = {
    SPLIT: true,
    STRATA_SPLIT: canAddStrata,
    MERGE: canMerge,
    RENAME: true,
  };
  return TRANSFORMATION_TYPES
    .filter(type => isAvailable[type])
    .sort((a, b) => getTransformationTypeLabel(a).localeCompare(getTransformationTypeLabel(b)));
}
