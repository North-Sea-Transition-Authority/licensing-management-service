import type { GroupEqualPolygonsHandler } from "./handler-types";
import { groupEqualPolygons } from "../geometric-operators/group-equal-polygons";
import { syncHandler } from "./sync-handler";

/**
 * Group polygons that are topologically equal.
 * @param request GRPC request with each polygon's id and Esri JSON.
 * @returns The ids of each group of equal polygons.
 */
export const groupEqualPolygonsHandler: GroupEqualPolygonsHandler = syncHandler(({ request }) => {
  const groups = groupEqualPolygons(request.polygons);
  return { groups: groups.map(ids => ({ ids })) };
});
