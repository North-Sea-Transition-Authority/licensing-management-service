import type { FindCrossingLinesHandler } from "./handler-types";
import { findCrossingLines } from "../geometric-operators/find-crossing-lines-operator";
import { syncHandler } from "./sync-handler";

/**
 * Find the lines that cross any of the given polygons, in two dimensions.
 * @param request GRPC request with the polygons, and the lines to check along with their ids, as Esri JSON strings.
 * @returns The ids of the lines that cross any of the polygons.
 */
export const findCrossingLinesHandler: FindCrossingLinesHandler = syncHandler(({ request }) => {
  const crossingLineIds = findCrossingLines(request.esriJsonPolygons, request.lines);
  return { crossingLineIds };
});
