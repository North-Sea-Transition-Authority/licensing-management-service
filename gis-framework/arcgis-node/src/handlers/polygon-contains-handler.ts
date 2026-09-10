import type { PolygonContainsHandler } from "./handler-types";
import { parentContainsChild } from "../geometric-operators/polygon-contains-operator";
import { asyncHandler } from "./async-handler";

/**
 * Check whether one polygon fully contains another.
 * @param request GRPC request with the container polygon and the contained polygon, as Esri JSON strings.
 * @returns True if the container polygon fully contains the contained polygon.
 */
export const polygonContainsHandler: PolygonContainsHandler = asyncHandler(async ({ request }) => {
  const contains = parentContainsChild(request.esriJsonContainerPolygon, request.esriJsonContainedPolygon);
  return { contains };
});
