import Polyline from '@arcgis/core/geometry/Polyline.js';
import * as unionOperator from '@arcgis/core/geometry/operators/unionOperator.js';
import * as containsOperator from '@arcgis/core/geometry/operators/containsOperator.js';
import * as multiPartToSinglePartOperator from '@arcgis/core/geometry/operators/multiPartToSinglePartOperator.js';

export interface ParentLine {
  id: string;
  polyline: Polyline;
}

export interface FindParentLinesResult {
  lines: ParentLine[];
  orphanedLines: Polyline[];
}

/**
 * Finds the parent lines for a list of child lines.
 * @param parentLines List of parent lines.
 * @param childrenLines List of child lines.
 * @return An object containing the children lines grouped by their parent line, and any orphaned lines that did not
 * match a parent. Grouped lines are merged into a single geometry unless they contain multiple paths, then they are
 * separated into multiple single-path lines.
 */
export function findParentLines(parentLines: ParentLine[], childrenLines: Polyline[]): FindParentLinesResult {
  // Map<ParentID, Polyline[]>
  const parentIdToLines = new Map<string, Polyline[]>();
  const orphans: Polyline[] = [];

  // Match each child line to a parent line
  childrenLines.forEach((childLine) => {
    let foundParentId = null;

    for (const parent of parentLines) {
      if (containsOperator.execute(parent.polyline, childLine)) {
        foundParentId = parent.id;
        break;
      }
    }

    if (foundParentId) {
      if (!parentIdToLines.has(foundParentId)) {
        parentIdToLines.set(foundParentId, []);
      }
      parentIdToLines.get(foundParentId).push(childLine);
    } else {
      orphans.push(childLine);
    }
  });

  // Merge grouped polylines into a single geometry
  const reconstructedLines: ParentLine[] = [];

  parentIdToLines.forEach((childPolylines, parentId) => {
    const mergedPolyline = unionOperator.executeMany(childPolylines) as Polyline;

    // Separate lines with multiple paths into single-path lines
    const singlePathPolylines = multiPartToSinglePartOperator.executeMany([mergedPolyline]) as Polyline[];

    if (singlePathPolylines) {
      singlePathPolylines.forEach((polyline) => {
        reconstructedLines.push({
          id: parentId,
          polyline: polyline,
        });
      });
    }
  });

  return {
    lines: reconstructedLines,
    orphanedLines: orphans,
  };
}
