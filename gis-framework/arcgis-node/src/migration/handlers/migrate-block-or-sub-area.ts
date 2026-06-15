import type { ArcGisServiceHandlers } from "../../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import type {
  MigrateBlockOrSubAreaResponse,
} from "../../../generated/uk/co/fivium/grpc/gis/MigrateBlockOrSubAreaResponse";
import type { LineWithNavigationTypeAndId } from "../types/line-with-navigation-wrapper";
import { status } from "@grpc/grpc-js";
import { LineNavigationType } from "../../../generated/uk/co/fivium/grpc/gis/LineNavigationType";
import { logger } from "../../config/logger";
import { densifyLoxodromesAndCalculateArea } from "../../geometric-operators/calculate-area-operator";
import { asyncHandler } from "../../handlers/async-handler";
import { GrpcError } from "../../handlers/grpc-error";
import { getCoordinateSystemWkid } from "../../util/coordinate-system-utils";
import { geoJsonLineInputToLinesWithNavigationTypeAndId } from "../types/line-with-navigation-wrapper";
import { fixDirectionOfAllLines } from "../utils/fix-direction-of-all-lines";
import { findParentLine, getLineStartAndEndPoints } from "../utils/migration-line-utils";
import {
  getNearestParentStartAndEndNodes,
  mergeParentDensePointsIntoChildLine,
  migrateBlock,
  shiftNodeAndUpdateConnectedLine,
} from "../utils/migration-utils";

export const migrateBlockOrSubarea: ArcGisServiceHandlers["migrateBlockOrSubarea"] = asyncHandler(async (call): Promise<MigrateBlockOrSubAreaResponse> => {
  const { geoJsonLineWrappers, coordinateSystem, parentLineEsriJsonStrings, shapeId } = call.request;
  const wkid = getCoordinateSystemWkid(coordinateSystem);
  logger.info(`Migrating ${geoJsonLineWrappers.length} lines, srs: ${wkid}, shapeId: ${shapeId}`);

  const idToLineWithNavigationWrapper = geoJsonLineInputToLinesWithNavigationTypeAndId(geoJsonLineWrappers, wkid);

  if (parentLineEsriJsonStrings.length === 0) {
    const lineWithNavigationTypeAndIds = await migrateBlock(Array.from(idToLineWithNavigationWrapper.values()));
    const area = await densifyLoxodromesAndCalculateArea(lineWithNavigationTypeAndIds, coordinateSystem);
    const esriJsonLineAndOracleIds = esriJsonLineAndOracleIdsFrom(lineWithNavigationTypeAndIds);

    return { esriJsonLineAndOracleIds, area };
  }

  for (const child of idToLineWithNavigationWrapper.values()) {
    const { line, navigationType, id } = child;
    // We want to process all the geodesic lines first and shift the start/end nodes of them and their connected lines.
    if (navigationType !== LineNavigationType.GEODESIC) {
      continue;
    }

    const { startPoint: childStartPoint, endPoint: childEndPoint } = getLineStartAndEndPoints(line);
    const parent = findParentLine(parentLineEsriJsonStrings, childStartPoint, childEndPoint, shapeId);
    if (parent === undefined) {
      const errorMessage = `Geodesic child line should have associated parent line but none were found. shapeId: ${shapeId}, lineSsid: ${id}`;
      logger.error(errorMessage);
      throw new GrpcError(status.INVALID_ARGUMENT, errorMessage);
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
      "start",
    );

    const newEndPoint = shiftNodeAndUpdateConnectedLine(
      childEndPoint,
      nearestEndPoint.coordinate,
      id,
      idToLineWithNavigationWrapper,
      parent,
      "end",
    );

    const newGeodesicLine = mergeParentDensePointsIntoChildLine(parent, newStartPoint, newEndPoint, line.spatialReference);
    child.line = newGeodesicLine;
    logger.debug(`New child line: ${newGeodesicLine.paths} Parent line: ${parent.paths} `);
  }

  fixDirectionOfAllLines(idToLineWithNavigationWrapper, geoJsonLineWrappers);

  const lineWithNavigationWrappers = Array.from(idToLineWithNavigationWrapper.values());
  const area = await densifyLoxodromesAndCalculateArea(lineWithNavigationWrappers, coordinateSystem);
  const esriJsonLineAndOracleIds = esriJsonLineAndOracleIdsFrom(lineWithNavigationWrappers);

  return { esriJsonLineAndOracleIds, area };
});

function esriJsonLineAndOracleIdsFrom(lineWithNavigationWrappers: LineWithNavigationTypeAndId[]) {
  const convertedLines: { esriJsonString: string, oracleLineSsid: number }[] = [];
  lineWithNavigationWrappers.forEach(value =>
    convertedLines.push({ esriJsonString: JSON.stringify(value.line), oracleLineSsid: value.id }),
  );
  return convertedLines;
}
