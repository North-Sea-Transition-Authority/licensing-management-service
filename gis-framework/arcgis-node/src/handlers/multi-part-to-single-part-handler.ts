import type { MultiPartToSinglePartHandler } from "./handler-types";
import { multiPartToSinglePart } from "../geometric-operators/multi-part-to-single-part";
import { asyncHandler } from "./async-handler";

export const multiPartToSinglePartHandler: MultiPartToSinglePartHandler = asyncHandler(async (call) => {
  const singlePartPolygons = multiPartToSinglePart(call.request.inputPolygon);
  return { singlePartPolygons };
});
