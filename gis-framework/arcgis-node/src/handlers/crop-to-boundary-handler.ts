import type { CropToBoundaryHandler } from "./handler-types";
import { cropToBoundary } from "../geometric-operators/crop-to-boundary-operator";
import { asyncHandler } from "./async-handler";

/**
 * Crop features to a boundary.
 * @param request GRPC request with the boundary polygons, and the features to crop along with their polygons, as Esri JSON strings.
 * @returns Per feature, its intersection status and every polygon that does not lie fully inside the boundary.
 */
export const cropToBoundaryHandler: CropToBoundaryHandler = asyncHandler(async ({ request }) => {
  const featureCropResults = cropToBoundary(request.esriJsonBoundaryPolygons, request.featuresToBeCropped);
  return { featureCropResults };
});
