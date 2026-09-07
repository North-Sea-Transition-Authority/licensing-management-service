import type { MergeAndGeneralizeLinesHandler } from "./handler-types";
import { mergeAndGeneralizeLines } from "../geometric-operators/merge-and-generalize-lines";
import { asyncHandler } from "./async-handler";

export const mergeAndGeneralizeLinesHandler: MergeAndGeneralizeLinesHandler = asyncHandler(async (call) => {
  const esriPolyline = mergeAndGeneralizeLines(call.request.esriPolylines);
  return { esriPolyline };
});
