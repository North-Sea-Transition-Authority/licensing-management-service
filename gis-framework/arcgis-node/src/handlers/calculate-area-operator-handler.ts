import type { ArcGisServiceHandlers } from "../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import type { CalculateAreaResponse } from "../../generated/uk/co/fivium/grpc/gis/CalculateAreaResponse";
import type { LineWithNavigationType } from "../geometric-operators/calculate-area-operator";
import { densifyLoxodromesAndCalculateArea } from "../geometric-operators/calculate-area-operator";
import { esriJsonToPolyline } from "../util/esrijson-util";
import { asyncHandler } from "./async-handler";

/**
 * Calculate the area of a feature. Loxodrome lines that are not part of a feature with BRITISH_NATIONAL_GRID coordinate
 * system will be densified before calculating the area to ensure the curvature of the earth is taken into account.
 * @param call GRPC call with a list of lines and the coordinate system of the feature.
 * @param callback Response callback. Contains the area of the feature.
 */
export const calculateAreaHandler: ArcGisServiceHandlers["calculateArea"] = asyncHandler(async (call): Promise<CalculateAreaResponse> => {
  const linesWithNavigationType: LineWithNavigationType[] = call.request.linesWithNavigationType.map(line => ({
    line: esriJsonToPolyline(line.esriJsonPolyline),
    navigationType: line.lineNavigationType,
  }));
  const area = await densifyLoxodromesAndCalculateArea(linesWithNavigationType, call.request.coordinateSystem);
  return { area };
});
