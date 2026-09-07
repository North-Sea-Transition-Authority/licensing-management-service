import type { MergePolygonsHandler } from "./handler-types";
import { mergePolygons } from "../geometric-operators/merge-polygons";
import { asyncHandler } from "./async-handler";

export const mergePolygonsHandler: MergePolygonsHandler = asyncHandler(async (call) => {
  const resultPolygon = mergePolygons(call.request.inputPolygon1, call.request.inputPolygon2);
  return { resultPolygon };
});
