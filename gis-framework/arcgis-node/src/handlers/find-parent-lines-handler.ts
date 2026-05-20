import type Polyline from "@arcgis/core/geometry/Polyline.js";
import type { ArcGisServiceHandlers } from "../../generated/uk/co/fivium/grpc/gis/ArcGisService";
import type { ParentLine } from "../geometric-operators/find-parent-lines";
import { logger } from "../config/logger";
import { findParentLines } from "../geometric-operators/find-parent-lines";
import { esriJsonToPolyline } from "../util/esrijson-util";
import { toGrpcInternalError } from "./grpc-error";

/**
 * Finds the parent lines for a list of child lines.
 * @param call GRPC call with a list of parent lines and a list of child lines.
 * @param callback Response callback. Contains the parent lines and orphaned child lines, returned as Esri JSON strings.
 */
export const findParentLinesHandler: ArcGisServiceHandlers["findParentLines"] = (call, callback) => {
  try {
    const parentLines: ParentLine[] = call.request.parentLines.map((parentLine) => {
      return {
        id: parentLine.id,
        polyline: esriJsonToPolyline(parentLine.esriJsonPolyline),
      };
    });
    const childrenLines: Polyline[] = call.request.childrenEsriJsonPolylines.map(childLine => esriJsonToPolyline(childLine));

    const result = findParentLines(parentLines, childrenLines);
    const linesWithParentMatch = result.lines.map((line) => {
      return {
        parentId: line.id,
        childEsriJsonPolyline: JSON.stringify(line.polyline.toJSON()),
      };
    });
    const orphanedLines = result.orphanedLines.map(line => JSON.stringify(line.toJSON()));

    callback(null, {
      linesWithParentMatch,
      orphanedChildrenEsriJsonPolylines: orphanedLines,
    });
  } catch (error) {
    logger.error({ error }, "Error finding parent lines");
    callback(toGrpcInternalError(error), null);
  }
};
