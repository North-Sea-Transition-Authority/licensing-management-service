import { ArcGisServiceHandlers } from '../../../generated/uk/co/fivium/grpc/gis/ArcGisService';
import { logger } from '../../config/logger';
import { getCoordinateSystemWkid } from '../../util/coordinate-system-utils';
import { findParentLine, getLineStartAndEndPoints } from '../utils/migration-line-utils';
import grpc from '@grpc/grpc-js';
import {
  getNearestParentStartAndEndNodes,
  mergeParentDensePointsIntoChildLine,
  migrateBlock,
  shiftNodeAndUpdateConnectedLine,
} from '../utils/migration-utils';
import { LineNavigationType } from '../../../generated/uk/co/fivium/grpc/gis/LineNavigationType';
import {
  geoJsonLineInputToLinesWithNavigationTypeAndId,
  LineWithNavigationTypeAndId,
} from '../types/line-with-navigation-wrapper';
import { densifyLoxodromesAndCalculateArea } from '../../geometric-operators/calculate-area-operator';
import { fixDirectionOfAllLines } from '../utils/fix-direction-of-all-lines';

export const migrateBlockOrSubarea: ArcGisServiceHandlers['migrateBlockOrSubarea'] = async (call, callback) => {
  const { geoJsonLineWrappers, coordinateSystem, parentLineEsriJsonStrings } = call.request;
  const wkid = getCoordinateSystemWkid(coordinateSystem);
  logger.info(`Migrating ${geoJsonLineWrappers.length} lines, srs: ${wkid}`);

  const idToLineWithNavigationWrapper = geoJsonLineInputToLinesWithNavigationTypeAndId(geoJsonLineWrappers, wkid);

  if (parentLineEsriJsonStrings.length === 0) {
    const lineWithNavigationTypeAndIds = await migrateBlock(Array.from(idToLineWithNavigationWrapper.values()));
    const area = await densifyLoxodromesAndCalculateArea(lineWithNavigationTypeAndIds, coordinateSystem);
    const esriJsonLineAndOracleIds = esriJsonLineAndOracleIdsFrom(lineWithNavigationTypeAndIds);

    callback(null, { esriJsonLineAndOracleIds: esriJsonLineAndOracleIds, area: area });
    return;
  }

  for (const child of idToLineWithNavigationWrapper.values()) {
    const { line, navigationType, id } = child;
    // We want to process all the geodesic lines first and shift the start/end nodes of them and their connected lines.
    if (navigationType !== LineNavigationType.GEODESIC) {
      continue;
    }

    const { startPoint: childStartPoint, endPoint: childEndPoint } = getLineStartAndEndPoints(line);
    const parent = findParentLine(parentLineEsriJsonStrings, childStartPoint, childEndPoint);
    if (parent === undefined) {
      const errorMessage = 'Geodesic child line should have associated parent line but none were found';
      logger.error(errorMessage);
      callback(
        {
          code: grpc.status.INVALID_ARGUMENT,
          message: errorMessage,
        },
        null,
      );
      return;
    }

    const { nearestStartPoint, nearestEndPoint } = getNearestParentStartAndEndNodes(parent, childStartPoint, childEndPoint);
    const pointsNeedShifting = Math.abs(nearestStartPoint.distance) !== 0 || Math.abs(nearestEndPoint.distance) !== 0;
    if (!pointsNeedShifting) {
      logger.debug(
        `Start/end nodes don't need shifting. Child line: ${JSON.stringify(line.toJSON())} Parent line: ${JSON.stringify(parent.toJSON())}`,
      );
      child.line = mergeParentDensePointsIntoChildLine(parent, childStartPoint, childEndPoint, line.spatialReference);
      continue;
    }

    const newStartPoint = shiftNodeAndUpdateConnectedLine(
      childStartPoint,
      nearestStartPoint.coordinate,
      id,
      idToLineWithNavigationWrapper,
      parent,
      'start',
    );

    const newEndPoint = shiftNodeAndUpdateConnectedLine(
      childEndPoint,
      nearestEndPoint.coordinate,
      id,
      idToLineWithNavigationWrapper,
      parent,
      'end',
    );

    const newGeodesicLine = mergeParentDensePointsIntoChildLine(parent, newStartPoint, newEndPoint, line.spatialReference);
    child.line = newGeodesicLine;
    logger.debug(`New child line: ${newGeodesicLine.paths} Parent line: ${parent.paths} `);
  }

  fixDirectionOfAllLines(idToLineWithNavigationWrapper, geoJsonLineWrappers);

  const lineWithNavigationWrappers = Array.from(idToLineWithNavigationWrapper.values());
  const area = await densifyLoxodromesAndCalculateArea(lineWithNavigationWrappers, coordinateSystem);
  const esriJsonLineAndOracleIds = esriJsonLineAndOracleIdsFrom(lineWithNavigationWrappers);

  callback(null, { esriJsonLineAndOracleIds: esriJsonLineAndOracleIds, area: area });
};

function esriJsonLineAndOracleIdsFrom(lineWithNavigationWrappers: LineWithNavigationTypeAndId[]) {
  const convertedLines: { esriJsonString: string; oracleLineSsid: number }[] = [];
  lineWithNavigationWrappers.forEach((value) =>
    convertedLines.push({ esriJsonString: JSON.stringify(value.line), oracleLineSsid: value.id }),
  );
  return convertedLines;
}
