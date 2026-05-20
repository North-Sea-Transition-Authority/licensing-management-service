import type Point from "@arcgis/core/geometry/Point.js";
import type Polyline from "@arcgis/core/geometry/Polyline.js";
import * as simplifyOperator from "@arcgis/core/geometry/operators/simplifyOperator.js";
import Polygon from "@arcgis/core/geometry/Polygon.js";
import { logger } from "../config/logger";
import { polygonsAreTopologicallyEqual } from "./polygon-equality-operator";

export interface OrderedPolyline {
  polyline: Polyline,
  ringNumber: number,
  connectionOrder: number,
}

/**
 * Validates that a polygon can be reconstructed from a list of ordered polylines. It also verifies that the
 * reconstructed polygon is spatially equal to the original polygon.
 * It uses a custom algorithm to reconstruct the polygon from the ordered lines instead on using the built-in linesToPolygons
 * to verify the order and line continuity is correct.
 * @param orderedLines List of ordered polylines that represent the polygon. The connection order is used to reconstruct
 * the polygon rings.
 * @param originalPolygonEsriJson The polygon esriJSON used to compare the constructed polygon against.
 */
export function validatePolygonReconstructionFromPolylines(
  orderedLines: OrderedPolyline[],
  originalPolygonEsriJson: string,
): boolean {
  let reconstructedPolygon: Polygon | undefined;
  try {
    reconstructedPolygon = reconstructPolygonFromLines(orderedLines);
  } catch (e) {
    logger.error({ error: e }, "Error reconstructing polygon from lines:");
    return false;
  }

  if (!reconstructedPolygon) {
    logger.error("Cannot reconstruct polygon from lines.");
    return false;
  }

  const areSpatiallyEqual = polygonsAreTopologicallyEqual(JSON.stringify(reconstructedPolygon.toJSON()), originalPolygonEsriJson);
  if (!areSpatiallyEqual) {
    logger.error(
      {
        originalPolygon: originalPolygonEsriJson,
        reconstructedPolygon: JSON.stringify(reconstructedPolygon.toJSON()),
      },
      "Polygon reconstructed from lines is not spatially equal to the original polygon",
    );
  }
  return areSpatiallyEqual;
}

function reconstructPolygonFromLines(orderedLines: OrderedPolyline[]): Polygon | undefined {
  const wkid = orderedLines[0].polyline.spatialReference.wkid;
  if (!wkid) {
    logger.error("Spatial reference WKID is missing from the first polyline.");
    throw new Error("Spatial reference WKID is missing from the first polyline.");
  }

  const ringToLines = getRingNumberToOrderedLines(orderedLines);

  const constructedRings = [];
  for (const [ringNumber, ringLines] of ringToLines) {
    const ringPath = constructRingPath(ringNumber, ringLines);
    if (!ringPath) {
      return undefined;
    }

    constructedRings.push(ringPath);
  }

  return simplifyConstructedPolygon(constructedRings, wkid);
}

function constructRingPath(ringNumber: number, ringLines: OrderedPolyline[]): number[][] | undefined {
  ringLines.sort((a, b) => a.connectionOrder - b.connectionOrder);

  const ringPath: number[][] = [];
  let previousEndPoint: Point | undefined;

  for (let i = 0; i < ringLines.length; i++) {
    const currentLine = ringLines[i];
    const points = currentLine.polyline.paths[0]; // line should have 1 path

    if (!isContinuousRingLine(i, points, previousEndPoint, ringNumber)) {
      return undefined;
    }

    appendLinePoints(ringPath, points, i);
    previousEndPoint = getLineEndPoint(currentLine.polyline);
  }

  return ringPath;
}

function isContinuousRingLine(
  lineIndex: number,
  points: number[][],
  previousEndPoint: Point | undefined,
  ringNumber: number,
): boolean {
  if (lineIndex === 0 || !previousEndPoint) {
    return true;
  }

  const currentLineStart = points[0]; // [x, y]
  if (currentLineStart[0] === previousEndPoint.x && currentLineStart[1] === previousEndPoint.y) {
    return true;
  }

  logger.error(
    {
      currentLine: points[0],
      previousEndPoint: JSON.stringify(previousEndPoint.toJSON()),
      ringNumber,
    },
    "Gap in ring continuity",
  );
  return false;
}

function appendLinePoints(ringPath: number[][], points: number[][], lineIndex: number): void {
  // Remove the first coordinate from each line because it is the same as the previous line's last coordinate.
  const pointsToAppend = lineIndex === 0 ? points : points.slice(1);
  ringPath.push(...pointsToAppend);
}

function getLineEndPoint(polyline: Polyline): Point {
  return polyline.getPoint(0, polyline.paths[0].length - 1) as Point;
}

function simplifyConstructedPolygon(constructedRings: number[][][], wkid: number): Polygon | undefined {
  const polygon = new Polygon({
    rings: constructedRings,
    spatialReference: { wkid },
  });

  const simplifiedPolygon = simplifyOperator.execute(polygon) as Polygon;
  // Check if it exists and has at least one ring with points
  if (!simplifiedPolygon?.rings?.length) {
    logger.error({ reconstructedPolygon: JSON.stringify(polygon.toJSON()) }, "Polygon is invalid after simplification");
    return undefined;
  }

  return simplifiedPolygon;
}

function getRingNumberToOrderedLines(orderedLines: OrderedPolyline[]): Map<number, OrderedPolyline[]> {
  const ringToLines = new Map<number, OrderedPolyline[]>();
  for (const line of orderedLines) {
    if (!ringToLines.has(line.ringNumber)) {
      ringToLines.set(line.ringNumber, []);
    }
    ringToLines.get(line.ringNumber)!.push(line);
  }
  return ringToLines;
}
