import type Polyline from "@arcgis/core/geometry/Polyline.js";
import type { LineWithNavigationTypeAndId } from "./line-with-navigation-wrapper";
import Point from "@arcgis/core/geometry/Point.js";
import { LineNavigationType } from "../../../generated/uk/co/fivium/grpc/gis/LineNavigationType";

export enum SetBearing {
  LATITUDE = "LATITUDE",
  LONGITUDE = "LONGITUDE",
}

export type LineWithSetBearingAndId = {
  line: Polyline,
  setBearing: SetBearing,
  id: number,
};

/**
 * This function searches for a loxodrome line out of a list of possible lines that is connected to a given point on a set bearing.
 * If one is found that it will return {@link LineWithSetBearingAndId} which is Polyline wrapper that contains the set bearing
 * of the line. If there is no connecting line, or a line that connects to the point but not on a set bearing,
 * then it will return undefined.
 * @param point The point which the loxodrome line should connect to.
 * @param lines A list of possible lines.
 * @returns {@link LineWithSetBearingAndId} if a connecting loxodrome line is found is on a set bearing, otherwise undefined.
 */
export function findLoxodromeThatConnectsToPointOnSetBearing(
  point: Point,
  lines: LineWithNavigationTypeAndId[],
): LineWithSetBearingAndId | undefined {
  let pointConnectsToStartOfLine: boolean = false;
  const connectingLine = lines.find((lineWrapper: LineWithNavigationTypeAndId) => {
    if (lineWrapper.navigationType !== LineNavigationType.LOXODROME) {
      return false;
    }
    const line = lineWrapper.line;
    const startPoint = line.getPoint(0, 0);
    const endPoint = line.getPoint(0, line.paths[0].length - 1);

    if (point.equals(startPoint)) {
      pointConnectsToStartOfLine = true;
      return true;
    } else if (point.equals(endPoint)) {
      pointConnectsToStartOfLine = false;
      return true;
    } else {
      return false;
    }
  });

  if (!connectingLine) {
    return undefined;
  }

  const line = connectingLine.line;
  let pointA: Point | undefined | null;
  let pointB: Point | undefined | null;
  if (pointConnectsToStartOfLine) {
    pointA = line.getPoint(0, 0);
    pointB = line.getPoint(0, 1);
  } else {
    pointA = line.getPoint(0, line.paths[0].length - 1);
    pointB = line.getPoint(0, line.paths[0].length - 2);
  }

  if (!(pointA instanceof Point) || !(pointB instanceof Point)) {
    return undefined;
  }

  const setBearing = getSetBearingOfPoints(pointA, pointB);

  if (!setBearing) {
    return undefined;
  }

  return {
    line,
    setBearing,
    id: connectingLine.id,
  };
}

/**
 * Determines if the line between two points is on a set bearing, and if so, which one and which way it should be extended by
 * in order to keep the bearing.
 *
 * If the two points have the same x coordinate, that means they have the same longitude, so we want to extend the line latitudinally.
 * If the two points have the same y coordinate, that means they have the same latitude, so we want to extend the line longitudinally.
 *
 * @param pointA The first point.
 * @param pointB The second point.
 * @returns The {@link SetBearing} of the line between the two points, or undefined if it is not on a set bearing.
 */
function getSetBearingOfPoints(pointA: Point, pointB: Point): SetBearing | undefined {
  if (pointA.x === pointB.x) {
    return SetBearing.LATITUDE;
  }
  if (pointA.y === pointB.y) {
    return SetBearing.LONGITUDE;
  }
  return undefined;
}
