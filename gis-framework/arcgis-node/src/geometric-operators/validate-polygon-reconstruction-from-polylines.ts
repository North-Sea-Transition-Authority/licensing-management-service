import type Polygon from "@arcgis/core/geometry/Polygon.js";
import type Polyline from "@arcgis/core/geometry/Polyline.js";
import * as linesToPolygonsOperator from "@arcgis/core/geometry/operators/linesToPolygonsOperator.js";
import { logger } from "../config/logger";
import { polygonsAreTopologicallyEqual } from "./polygon-equality-operator";

/**
 * Validates that a polygon can be reconstructed from a list of polylines. It also verifies that the
 * reconstructed polygon is spatially equal to the original polygon.
 * @param polylines List of polylines that represent the polygon.
 * @param originalPolygonEsriJson The polygon esriJSON used to compare the constructed polygon against.
 */
export function validatePolygonReconstructionFromPolylines(
  polylines: Polyline[],
  originalPolygonEsriJson: string,
): boolean {
  let reconstructedPolygon: Polygon | undefined;
  try {
    reconstructedPolygon = linesToPolygonsOperator.executeMany(polylines)[0];
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
