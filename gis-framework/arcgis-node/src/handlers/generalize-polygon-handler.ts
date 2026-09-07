import type { GeneralizePolygonHandler } from "./handler-types";
import { generalizePolygon } from "../geometric-operators/generalize-polygon";
import { asyncHandler } from "./async-handler";

export const generalizePolygonHandler: GeneralizePolygonHandler = asyncHandler(async (call) => {
  const esriPolygon = generalizePolygon(call.request.esriPolygon);
  return { esriPolygon };
});
