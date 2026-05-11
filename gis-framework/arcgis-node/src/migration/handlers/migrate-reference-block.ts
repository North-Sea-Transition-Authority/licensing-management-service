import { ArcGisServiceHandlers } from '../../../generated/uk/co/fivium/grpc/gis/ArcGisService';
import {
  geoJsonLineInputToLinesWithNavigationTypeAndId,
  LineWithNavigationTypeAndId,
} from '../types/line-with-navigation-wrapper';
import Polyline from '@arcgis/core/geometry/Polyline.js';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType';
import {
  findLineConnectingToPointNotOnBearing,
  findPointOfIntersectionBetweenChildPointOnBearingAndParentLine,
  GENERALIZE_TOLERANCE_DEGREES,
  GEODESIC_DENSE_POINT_METERS_INTERVAL,
  getIndexOfPointOnLine,
  isApproximatelyEqual,
  ONE_ARC_SECOND,
} from '../utils/migration-utils';
import { getLineStartAndEndPoints, ONE_HUNDRED_METERS_ED50 } from '../utils/migration-line-utils';
import Point from '@arcgis/core/geometry/Point';
import { findLoxodromeThatConnectsToPointOnSetBearing } from '../types/line-with-bearing-wrapper';
import * as geodeticDensifyOperator from '@arcgis/core/geometry/operators/geodeticDensifyOperator.js';
import * as proximityOperator from '@arcgis/core/geometry/operators/proximityOperator.js';
import * as unionOperator from '@arcgis/core/geometry/operators/unionOperator.js';
import * as generalizeOperator from '@arcgis/core/geometry/operators/generalizeOperator.js';
import { getCoordinateSystemWkid } from '../../util/coordinate-system-utils';
import { logger } from '../../config/logger';
import { toGrpcInternalError } from '../../handlers/grpc-error';
import { esriJsonToPolyline } from '../../util/esrijson-util';

export const migrateReferenceBlockHandler: ArcGisServiceHandlers['migrateReferenceBlock'] = async (call, callback) => {
  try {
    logger.info(`migrateReferenceBlock: starting`);
    const { geoJsonLineWrappers, coordinateSystem, licenseBlockLines } = call.request;
    const wkid = getCoordinateSystemWkid(coordinateSystem);

    // Convert all ref block lines to polylines
    const idToLineWithNavigationWrapper = geoJsonLineInputToLinesWithNavigationTypeAndId(geoJsonLineWrappers, wkid);

    // Combine consecutive geodesic lines into single line, loxodromes remain unchanged.
    const idToConnectionOrder = new Map(geoJsonLineWrappers.map((wrapper) => [wrapper.oracleLineSsid, wrapper.connectionOrder]));
    const combinedGeodesicAndLoxodromes = mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers(
      idToLineWithNavigationWrapper,
      idToConnectionOrder,
    );

    // Get geodesic license lines
    const geodesicLicenseLines = licenseBlockLines
      .filter((line) => line.navigationType === LineNavigationType.GEODESIC)
      .map((line) => esriJsonToPolyline(line.esriJsonString));
    callback(null, await migrateReferenceBlock(combinedGeodesicAndLoxodromes, geodesicLicenseLines));
  } catch (error) {
    logger.error({ error: error }, 'Error migrating reference block');
    callback(toGrpcInternalError(error), null);
  }
};

async function migrateReferenceBlock(
  combinedGeodesicAndLoxodromes: LineWithNavigationTypeAndId[],
  geodesicLicenseLines: Polyline[],
) {
  // loop through all geodesic lines and shift points
  if (!geodeticDensifyOperator.isLoaded()) {
    await geodeticDensifyOperator.load();
  }
  for (const refBlockLineWrapper of combinedGeodesicAndLoxodromes.filter(
    (wrapper) => wrapper.navigationType != LineNavigationType.LOXODROME,
  )) {
    refBlockLineWrapper.line = geodeticDensifyOperator.execute(refBlockLineWrapper.line, GEODESIC_DENSE_POINT_METERS_INTERVAL, {
      curveType: 'geodesic',
      unit: 'meters',
    }) as Polyline;

    // Generalize the line after densification to remove unnecessary points that don't affect the overall shape of the line.
    refBlockLineWrapper.line = generalizeOperator.execute(refBlockLineWrapper.line, GENERALIZE_TOLERANCE_DEGREES) as Polyline;

    const { startPoint: refBlockGeodesicStartPoint, endPoint: refBlockGeodesicEndPoint } = getLineStartAndEndPoints(
      refBlockLineWrapper.line,
    );
    logger.info(`Original start point: ${JSON.stringify([refBlockGeodesicStartPoint.x, refBlockGeodesicStartPoint.y])}`);
    logger.info(`Original end point: ${JSON.stringify([refBlockGeodesicEndPoint.x, refBlockGeodesicEndPoint.y])}`);

    updateGeodesicReferenceBlockLine(
      geodesicLicenseLines,
      refBlockGeodesicStartPoint,
      refBlockGeodesicEndPoint,
      refBlockLineWrapper,
      combinedGeodesicAndLoxodromes,
    );

    // After processing, get new start/end points and update connected loxodrome lines
    const { startPoint: newStartPoint, endPoint: newEndPoint } = getLineStartAndEndPoints(refBlockLineWrapper.line);
    logger.info(`New start point: ${JSON.stringify([newStartPoint.x, newStartPoint.y])}`);
    logger.info(`New end point: ${JSON.stringify([newEndPoint.x, newEndPoint.y])}`);

    // Update connected loxodrome lines if start point changed
    if (!isApproximatelyEqual(refBlockGeodesicStartPoint, newStartPoint)) {
      logger.info(`Start point shifted, updating connected loxodrome line`);
      const connectedLine = findLineConnectingToPointNotOnBearing(
        refBlockGeodesicStartPoint,
        refBlockLineWrapper.id,
        combinedGeodesicAndLoxodromes,
      );
      if (connectedLine) {
        const index = getIndexOfPointOnLine(refBlockGeodesicStartPoint, connectedLine.line);
        connectedLine.line.setPoint(0, index, newStartPoint);
        logger.info(`Updated loxodrome line ${connectedLine.id} at index ${index}`);
      }
    }

    // Update connected loxodrome lines if end point changed
    if (!isApproximatelyEqual(refBlockGeodesicEndPoint, newEndPoint)) {
      logger.info(`End point shifted, updating connected loxodrome line`);
      const connectedLine = findLineConnectingToPointNotOnBearing(
        refBlockGeodesicEndPoint,
        refBlockLineWrapper.id,
        combinedGeodesicAndLoxodromes,
      );
      if (connectedLine) {
        const index = getIndexOfPointOnLine(refBlockGeodesicEndPoint, connectedLine.line);
        connectedLine.line.setPoint(0, index, newEndPoint);
        logger.info(`Updated loxodrome line ${connectedLine.id} at index ${index}`);
      }
    }
  }

  logger.info(`Building result from ${combinedGeodesicAndLoxodromes.length} lines`);
  const result: { esriJsonString: string; oracleLineSsid: number }[] = [];
  combinedGeodesicAndLoxodromes.forEach((lineWrapper) => {
    logger.info(`oracleLineSsid: ${lineWrapper.id} json: ${JSON.stringify(lineWrapper.line.toJSON())} `);
    result.push({
      esriJsonString: JSON.stringify(lineWrapper.line),
      oracleLineSsid: lineWrapper.id,
    });
  });

  logger.info(`migrateReferenceBlock: completed, returning ${result.length} lines`);
  return { esriJsonLineWithId: result };
}

/**
 * Attempt to update the dense points of the ref block geodesic line {@link refBlockLineWrapper} with the dense points from any
 * overlapping licence block line {@link geodesicLicenseLines}. If no lines overlap, then the ref block line won't be updated.
 * @param geodesicLicenseLines A list of {@link Polyline} which is all the geodesic licence block lines
 * @param refBlockGeodesicStartPoint A copy of the start point of {@link refBlockLineWrapper}
 * @param refBlockGeodesicEndPoint A copy of the end point of {@link refBlockLineWrapper}
 * @param refBlockLineWrapper The geodesic reference block line we want to update
 * @param combinedGeodesicAndLoxodromes A list of {@link LineWithNavigationTypeAndId} which make up the reference block.
 * @returns nothing, instead it will update the inputted variables.
 */
export function updateGeodesicReferenceBlockLine(
  geodesicLicenseLines: Polyline[],
  refBlockGeodesicStartPoint: Point,
  refBlockGeodesicEndPoint: Point,
  refBlockLineWrapper: LineWithNavigationTypeAndId,
  combinedGeodesicAndLoxodromes: LineWithNavigationTypeAndId[],
) {
  // For each geodesic license line that the ref block contains
  for (const licenseLine of geodesicLicenseLines) {
    // Save original start/end points before processing
    const { startPoint: licenseStartPoint, endPoint: licenseEndPoint } = getLineStartAndEndPoints(licenseLine);
    logger.info(`license start point: ${JSON.stringify([licenseStartPoint.x, licenseStartPoint.y])}`);
    logger.info(`license end point: ${JSON.stringify([licenseEndPoint.x, licenseEndPoint.y])}`);

    // Find where the license block start and end nodes intersect with the ref block
    // Taking bearing into account if the node connects to a loxodrome line following a bearing
    // Determine which license point is closest to which ref block point (handles opposite directions)
    const startToStartDistance = proximityOperator.getNearestCoordinate(licenseStartPoint, refBlockGeodesicStartPoint).distance;
    const startToEndDistance = proximityOperator.getNearestCoordinate(licenseEndPoint, refBlockGeodesicStartPoint).distance;

    // Match ref block endpoints to closest license endpoints
    const isLinesGoingSameDirection = startToStartDistance <= startToEndDistance;
    const licensePointForRefStart = isLinesGoingSameDirection ? licenseStartPoint : licenseEndPoint;
    const licensePointForRefEnd = isLinesGoingSameDirection ? licenseEndPoint : licenseStartPoint;
    logger.info(`Lines direction matching: ${isLinesGoingSameDirection}`);

    const startIntersection = findIntersectionPoint(
      refBlockGeodesicStartPoint,
      licensePointForRefStart,
      licenseLine,
      refBlockLineWrapper.line,
      combinedGeodesicAndLoxodromes,
    );
    const endIntersection = findIntersectionPoint(
      refBlockGeodesicEndPoint,
      licensePointForRefEnd,
      licenseLine,
      refBlockLineWrapper.line,
      combinedGeodesicAndLoxodromes,
    );

    if (startIntersection && endIntersection) {
      logger.info('start and end intersection found, replacing segment');
      // Replace the ref block segment between the start and end nodes with the license block line
      refBlockLineWrapper.line = replaceSegment(
        refBlockLineWrapper.line,
        licenseLine,
        startIntersection,
        endIntersection,
        isLinesGoingSameDirection,
      );
    } else {
      logger.info(
        `Intersections not found, not replacing segment startIntersection for reference block line ${refBlockLineWrapper.id}`,
      );
    }
  }
}

/**
 * Combines adjacent geodesic lines based on their connection order defined in {@link idToConnectionOrder}. The resulting line
 * will take the id of the line at the start of the chain.
 * @param idToLineWrapper A Map of the line id to the line wrapper {@link LineWithNavigationTypeAndId}
 * @param idToConnectionOrder A map of the line id to the connection order.
 * @returns a new list of {@link LineWithNavigationTypeAndId}, where previously adjacent geodesic lines have been merged,
 * and their id has been updated.
 */
export function mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers(
  idToLineWrapper: Map<number, LineWithNavigationTypeAndId>,
  idToConnectionOrder: Map<number, number>,
): LineWithNavigationTypeAndId[] {
  logger.info(`mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers: starting`);
  const geodesicEntries = Array.from(idToLineWrapper.values())
    .filter((wrapper) => wrapper.navigationType === LineNavigationType.GEODESIC)
    .sort((a, b) => (idToConnectionOrder.get(a.id) ?? 0) - (idToConnectionOrder.get(b.id) ?? 0));
  logger.info(`geodesicEntries.length=${geodesicEntries.length}`);

  if (geodesicEntries.length <= 1) {
    logger.info(`1 or less geodesic lines, no adjacent lines merged.`);
    return Array.from(idToLineWrapper.values());
  }

  const firstOrder = Math.min(...idToConnectionOrder.values());
  const lastOrder = Math.max(...idToConnectionOrder.values());

  const processedLines: LineWithNavigationTypeAndId[] = [];
  const mergedIds: number[] = [];

  for (const currentWrapper of geodesicEntries) {
    const currentConnectionOrder = idToConnectionOrder.get(currentWrapper.id);

    if (mergedIds.includes(currentWrapper.id)) {
      continue;
    }

    let mergedLine = currentWrapper.line;
    let maxConnectionOrder = currentConnectionOrder!;

    for (const nextWrapper of geodesicEntries) {
      const nextConnectionOrder = idToConnectionOrder.get(nextWrapper.id);

      if (mergedIds.includes(nextWrapper.id) || nextWrapper.id === currentWrapper.id) {
        continue;
      }

      if (
        nextConnectionOrder === maxConnectionOrder + 1 ||
        (firstOrder === currentConnectionOrder && nextConnectionOrder === lastOrder)
      ) {
        mergedLine = unionOperator.execute(mergedLine, nextWrapper.line) as Polyline;
        mergedIds.push(nextWrapper.id);
        maxConnectionOrder = nextConnectionOrder;
      }
    }

    processedLines.push({
      line: mergedLine,
      navigationType: LineNavigationType.GEODESIC,
      id: currentWrapper.id,
    });
  }

  // Add any non-merged and loxodrome lines to our processedLine list
  Array.from(idToLineWrapper.values()).forEach((wrapper) => {
    if (processedLines.some((processedLine) => processedLine.id === wrapper.id)) {
      return;
    }
    if (mergedIds.includes(wrapper.id)) {
      return;
    }
    processedLines.push(wrapper);
  });

  logger.info(`mergeAdjacentGeodesicLinesAndReturnAllNewLineWrappers: completed, returning ${processedLines.length} lines`);
  return processedLines;
}

/**
 * Finds the intersection point between a licence block line and a reference block line. If the reference block line is on a
 * set bearing then it will extend {@link refBlockPoint} or  {@link licensePoint} on the bearing to find an intersection.
 * Otherwise it will find the nearest point.
 *
 * @param refBlockPoint The start or end of the {@link refBlockLine}
 * @param licensePoint The start or end of the {@link licensePoint}
 * @param licenseLine The geodesic licence block {@link Polyline} we want to intersect
 * @param refBlockLine The geodesic reference block {@link Polyline} we want to intersect refBlockLine
 * @param allLines A list of all the {@link LineWithNavigationTypeAndId} which make up the reference block.
 * @returns  The {@link Point} of interesection, or undefined if their is no intersection found.
 */
export function findIntersectionPoint(
  refBlockPoint: Point,
  licensePoint: Point,
  licenseLine: Polyline,
  refBlockLine: Polyline,
  allLines: LineWithNavigationTypeAndId[],
): Point | undefined {
  logger.info(`refblockPoint: ${refBlockPoint.x}, ${refBlockPoint.y} licensePoint: ${licensePoint.x}, ${licensePoint.y}`);
  // Check if the point connects to a loxodrome line following a bearing
  const lineOnBearing = findLoxodromeThatConnectsToPointOnSetBearing(refBlockPoint, allLines);

  if (lineOnBearing) {
    // Find intersection using bearing
    const intersectionWithRefPoint = findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
      refBlockPoint,
      lineOnBearing.setBearing,
      licenseLine,
      ONE_ARC_SECOND * 30,
    );

    if (intersectionWithRefPoint) {
      logger.info(`intersectionWithRefPoint ${intersectionWithRefPoint.x}, ${intersectionWithRefPoint.y}`);
      return intersectionWithRefPoint;
    }

    const intersectionWithLicensePoint = findPointOfIntersectionBetweenChildPointOnBearingAndParentLine(
      licensePoint,
      lineOnBearing.setBearing,
      refBlockLine,
      ONE_ARC_SECOND * 30,
    );
    if (intersectionWithLicensePoint) {
      logger.info(`intersectionWithLicensePoint ${intersectionWithLicensePoint.x}, ${intersectionWithLicensePoint.y}`);
    }
    return intersectionWithLicensePoint;
  }

  // Find the nearest point on the ref block line
  const nearestToRefBlockPoint = proximityOperator.getNearestCoordinate(licenseLine, refBlockPoint);
  const nearestToLicensePoint = proximityOperator.getNearestCoordinate(refBlockLine, licensePoint);
  const nearest =
    nearestToRefBlockPoint.distance < nearestToLicensePoint.distance ? nearestToRefBlockPoint : nearestToLicensePoint;
  if (nearest.distance > ONE_HUNDRED_METERS_ED50) {
    logger.info(`Nearest distance is ${nearest.distance}, which is further than the 100m limit`);
    return undefined;
  }
  logger.info('Nearest non bearing point used');

  return nearest.coordinate;
}

/**
 * This method replaces all the points on the {@link refBlockLine} between the {@link startPoint} and {@link endPoint} with the
 * points from {@link licenseLine} that are between the {@link startPoint} and {@link endPoint} and returns a new {@link Polyline}
 *
 * @param refBlockLine The geodesic reference block {@link Polyline} that needs updating.
 * @param licenseLine The geodesic lincense block {@link Polyline} whose points we are going to copy.
 * @param startPoint The start point for where we should start the replacement of points
 * @param endPoint The end point for where we should start the replacement of points
 * @param isLinesGoingSameDirection true if the lines are going the same direction, false if they are not
 * @returns a new {@link Polyline} whose points are a combinationis of {@link refBlockLine} and {@link licenseLine}
 */
export function replaceSegment(
  refBlockLine: Polyline,
  licenseLine: Polyline,
  startPoint: Point,
  endPoint: Point,
  isLinesGoingSameDirection: boolean,
): Polyline {
  const [fromIndex, toIndex] = [
    getIndexOfPointOnLine(startPoint, refBlockLine),
    getIndexOfPointOnLine(endPoint, refBlockLine),
  ].sort((a, b) => a - b);

  // Only part of the license line might cover the ref block so we only want to copy that section.
  const licenseStartIndex = getIndexOfPointOnLine(startPoint, licenseLine);
  const licenseEndIndex = getIndexOfPointOnLine(endPoint, licenseLine);
  const [licenseFromIndex, licenseToIndex] = [licenseStartIndex, licenseEndIndex].sort((a, b) => a - b);

  const licensePoints = licenseLine.paths[0];
  const fromPoint = licenseStartIndex <= licenseEndIndex ? startPoint : endPoint;
  const toPoint = licenseStartIndex <= licenseEndIndex ? endPoint : startPoint;

  const licenseSegment = licensePoints.slice(licenseFromIndex, licenseToIndex + 1);
  const licenseMiddle = isLinesGoingSameDirection
    ? [[fromPoint.x, fromPoint.y], ...licenseSegment, [toPoint.x, toPoint.y]]
    : [[toPoint.x, toPoint.y], ...licenseSegment.toReversed(), [fromPoint.x, fromPoint.y]];

  const newPath = [
    ...refBlockLine.paths[0].slice(0, fromIndex),
    ...licenseMiddle,
    ...refBlockLine.paths[0].slice(toIndex + 1),
  ].filter((currentPoint, index, points) => {
    return index === 0 || currentPoint[0] !== points[index - 1][0] || currentPoint[1] !== points[index - 1][1];
  });

  return new Polyline({
    paths: [newPath],
    spatialReference: refBlockLine.spatialReference,
  });
}
