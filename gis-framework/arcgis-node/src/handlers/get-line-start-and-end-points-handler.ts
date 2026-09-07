import type { GetLineStartAndEndPointsResponse } from "../../generated/uk/co/fivium/grpc/gis/GetLineStartAndEndPointsResponse";
import type { PolylineWithId } from "../geometric-operators/get-line-start-and-end-points";
import type { GetLineStartAndEndPointsHandler } from "./handler-types";
import { getLineStartAndEndPoints } from "../geometric-operators/get-line-start-and-end-points";
import { esriJsonToPolyline } from "../util/esrijson-util";
import { asyncHandler } from "./async-handler";

/**
 * Get the start and end points of a list of lines.
 * @param call a list of lines to get the start and end points of,
 *             and a flag to determine if the points should be projected to WGS84.
 * @returns a list of lines with their start and end points.
 */
export const getLineStartAndEndPointsHandler: GetLineStartAndEndPointsHandler = asyncHandler(async (call): Promise<GetLineStartAndEndPointsResponse> => {
  const polylinesWithId: PolylineWithId[] = call.request.lines.map((line) => {
    return {
      id: line.id,
      polyline: esriJsonToPolyline(line.polyLineEsriJson),
    };
  });

  const result = await getLineStartAndEndPoints(polylinesWithId, call.request.shouldProjectToWgs84);
  return {
    lines: result,
  };
});
