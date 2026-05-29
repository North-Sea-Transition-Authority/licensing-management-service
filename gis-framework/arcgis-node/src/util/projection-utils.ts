import GeographicTransformation from "@arcgis/core/geometry/operators/support/GeographicTransformation";
import GeographicTransformationStep from "@arcgis/core/geometry/operators/support/GeographicTransformationStep";

// 1311 is the wkid of the ED_1950_To_WGS_1984_18 transformation approved by the NSTA
const ed50ToWgs84TransformationStep = new GeographicTransformationStep({ wkid: 1311 });
const wgs84ToEd50TransformationStep = ed50ToWgs84TransformationStep.getInverse();

export const ed50ToWgs84Transformation = new GeographicTransformation({
  steps: [ed50ToWgs84TransformationStep],
});

export const wgs84ToEd50Transformation = new GeographicTransformation({
  steps: [wgs84ToEd50TransformationStep],
});
