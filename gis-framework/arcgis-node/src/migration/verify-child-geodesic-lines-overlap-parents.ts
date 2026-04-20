import * as containsOperator from '@arcgis/core/geometry/operators/containsOperator.js';

import { logger } from '../config/logger';
import { findParentLine, getLineStartAndEndPoints } from './migration-line-utils';
import { esriJsonToPolyline } from '../util/esrijson-util';

type Line = {
  esriJsonPolyline: string;
  isGeodesic: boolean;
};

/**
 * Verifies that all child geodesic lines overlap with a parent geodesic line.
 * @param parentLines
 * @param childLines
 * @return true if all child geodesic lines overlap with a parent geodesic line, false otherwise.
 */
export function childGeodesicLinesOverlapParents(parentLines: Line[], childLines: Line[]): boolean {
  const parentGeodesicLines = parentLines.filter((line) => line.isGeodesic).map((line) => line.esriJsonPolyline);

  const orphanedChildLinesJson: string[] = [];
  const nonOverlappingChildLinesJson: string[] = [];

  childLines
    .filter((childLine) => childLine.isGeodesic)
    .forEach((childLine) => {
      const child = esriJsonToPolyline(childLine.esriJsonPolyline);
      const { startPoint, endPoint } = getLineStartAndEndPoints(child);
      const parent = findParentLine(parentGeodesicLines, startPoint, endPoint);

      if (parent === undefined) {
        orphanedChildLinesJson.push(childLine.esriJsonPolyline);
        return;
      }

      if (!containsOperator.execute(parent, child)) {
        nonOverlappingChildLinesJson.push(childLine.esriJsonPolyline);
      }
    });

  orphanedChildLinesJson.forEach((line) =>
    logger.warn({ orphanedLineJson: line }, 'Parent geodesic not found for child geodesic line'),
  );
  nonOverlappingChildLinesJson.forEach((line) =>
    logger.warn({ nonOverlappingLineJson: line }, 'Non-overlapping child geodesic line'),
  );

  return orphanedChildLinesJson.length === 0 && nonOverlappingChildLinesJson.length === 0;
}
