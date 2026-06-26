import type Polyline from "@arcgis/core/geometry/Polyline";
import { logger } from "../config/logger";
import { projectPointToWgs84 } from "./project-polygon";

export interface PolylineWithId {
  id: string,
  polyline: Polyline,
}

export interface LineIdWithStartAndEndPoints {
  lineId: string,
  startPoint: {
    x: number,
    y: number,
  },
  endPoint: {
    x: number,
    y: number,
  },
}

export async function getLineStartAndEndPoints(
  lines: PolylineWithId[],
  shouldProjectToWgs84: boolean,
): Promise<LineIdWithStartAndEndPoints[]> {
  const lineIdWithStartAndEndPoints = [];
  for (const line of lines) {
    const path = line.polyline.paths[0];

    if (!path || path.length === 0) {
      logger.error(`Line ${line.id} has no path`);
      throw new Error(`Line ${line.id} has no path`);
    }

    let startPoint = line.polyline.getPoint(0, 0);
    let endPoint = line.polyline.getPoint(0, path.length - 1);

    if (!startPoint || !endPoint) {
      logger.error(`Could not get start and end points for line ${line.id}`);
      throw new Error(`Could not get start and end points for line ${line.id}`);
    }

    if (shouldProjectToWgs84) {
      startPoint = await projectPointToWgs84(startPoint);
      endPoint = await projectPointToWgs84(endPoint);
    }

    const pointResponse = {
      lineId: line.id,
      startPoint: {
        x: startPoint.x,
        y: startPoint.y,
      },
      endPoint: {
        x: endPoint.x,
        y: endPoint.y,
      },
    };
    lineIdWithStartAndEndPoints.push(pointResponse);
  }
  return lineIdWithStartAndEndPoints;
}
