import type { Find2dOverlappingFeaturesHandler } from "./handler-types";
import { find2dOverlappingFeatures } from "../geometric-operators/find-2d-overlapping-features-operator";
import { syncHandler } from "./sync-handler";

/**
 * Find the features that share any area with a shape, in two dimensions.
 * @param request GRPC request with the polygons that form the shape, and the features to compare along with their polygons, as Esri JSON strings.
 * @returns The ids of the features that share any area with the shape.
 */
export const find2dOverlappingFeaturesHandler: Find2dOverlappingFeaturesHandler = syncHandler(({ request }) => {
  const overlappingFeatureIds = find2dOverlappingFeatures(request.esriJsonPolygons, request.featuresToCompare);
  return { overlappingFeatureIds };
});
