import Polyline from '@arcgis/core/geometry/Polyline';
import Point from '@arcgis/core/geometry/Point';
import * as proximityOperator from '@arcgis/core/geometry/operators/proximityOperator.js';

export interface LineWithId {
  id: string;
  polyline: Polyline;
}

/**
 * Finds the line with the northwestern-most starting point.
 * @param linesWithId List of lines with IDs.
 * @returns The ID of the line with the northwestern-most starting point.
 */
export function findNorthwestMostLine(linesWithId: LineWithId[]): string {
  const firstLineWithId = linesWithId[0];
  if (!firstLineWithId) {
    throw new Error('No lines provided');
  }

  const startPoints: { startPoint: Point; id: string }[] = [];
  for (const line of linesWithId) {
    const startPoint = line.polyline.getPoint(0, 0);
    if (!startPoint) {
      throw new Error(`Could not get start point for line ${line.id}`);
    }

    startPoints.push({
      startPoint,
      id: line.id,
    });
  }

  const firstStartPoint = startPoints[0];
  if (!firstStartPoint) {
    throw new Error('No start points found');
  }

  //Find the most northern (max Y) and most western (min X) coordinates
  let maxY = firstStartPoint.startPoint.y;
  let minX = firstStartPoint.startPoint.x;

  for (const pointWithId of startPoints) {
    maxY = Math.max(maxY, pointWithId.startPoint.y);
    minX = Math.min(minX, pointWithId.startPoint.x);
  }

  // Reference point at northwestern-most point
  const nwReferencePoint = new Point({
    x: minX,
    y: maxY,
    spatialReference: { wkid: firstLineWithId.polyline.spatialReference.wkid },
  });

  //Find the closest start point to NW reference using the proximity operator
  let closestId = firstStartPoint.id;
  let minDistance = Number.POSITIVE_INFINITY;

  for (const pointWithId of startPoints) {
    const point = pointWithId.startPoint;
    const proximityResult = proximityOperator.getNearestVertex(point, nwReferencePoint);
    const distance = proximityResult.distance;

    if (distance < minDistance) {
      minDistance = distance;
      closestId = pointWithId.id;
    }
  }

  return closestId;
}
