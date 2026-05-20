import type Point from "@arcgis/core/geometry/Point.js";
import type Polyline from "@arcgis/core/geometry/Polyline.js";
import * as proximityOperator from "@arcgis/core/geometry/operators/proximityOperator.js";
import { logger } from "../../config/logger";
import { esriJsonToPolyline } from "../../util/esrijson-util";

export const FIVE_CM_IN_DEGREES_AT_48N_ED50 = 0.00000067;
export const ONE_HUNDRED_METERS_ED50 = FIVE_CM_IN_DEGREES_AT_48N_ED50 * 2000;

/**
 * Gets the start and end points of a polyline.
 * @param polyline The polyline must have a single path.
 * @returns An object containing the start and end points of the polyline.
 */
export function getLineStartAndEndPoints(polyline: Polyline): { startPoint: Point, endPoint: Point } {
  if (polyline.paths.length !== 1) {
    throw new Error("Polyline must have exactly one path");
  }

  const startPoint = polyline.getPoint(0, 0);
  const endPoint = polyline.getPoint(0, polyline.paths[0].length - 1);

  if (!startPoint || !endPoint) {
    throw new Error("Polyline has no start or end point");
  }

  return { startPoint, endPoint };
}

/**
 * Finds the closest parent line to the child line
 * There is a 5cm tolerance for the start and end points of the child line. Anything further away is considered invalid.
 * @param parentLines Possible parent lines
 * @param childStartPoint the first point, e.g., the start of a child line.
 * @param childEndPoint the second point, e.g., the end of a child line.
 * @returns A Polyline which is the closest to both points, or undefined if there is no line within 5 cm of both points.
 */
export function findParentLine(parentLines: string[], childStartPoint: Point, childEndPoint: Point): Polyline | undefined {
  let parent: Polyline | undefined;
  let closestCombinedDistance = Number.POSITIVE_INFINITY;
  let closestStartDistance = Number.POSITIVE_INFINITY;
  let closestEndDistance = Number.POSITIVE_INFINITY;

  parentLines.forEach((line) => {
    const possibleParent = esriJsonToPolyline(line);
    const nearestStartPoint = proximityOperator.getNearestCoordinate(possibleParent, childStartPoint);
    const nearestEndPoint = proximityOperator.getNearestCoordinate(possibleParent, childEndPoint);
    const combinedDistance = nearestStartPoint.distance + nearestEndPoint.distance;

    if (combinedDistance < closestCombinedDistance) {
      closestCombinedDistance = combinedDistance;
      closestStartDistance = nearestStartPoint.distance;
      closestEndDistance = nearestEndPoint.distance;
      parent = possibleParent;
    }
  });

  if (closestStartDistance > FIVE_CM_IN_DEGREES_AT_48N_ED50 || closestEndDistance > FIVE_CM_IN_DEGREES_AT_48N_ED50) {
    logger.error(`Parent line is too far away. Start difference: ${closestStartDistance} end difference: ${closestEndDistance}`);
    return undefined;
  }

  return parent;
}
