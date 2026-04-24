import Polyline from '@arcgis/core/geometry/Polyline';
import { LineWithNavigationTypeAndId } from '../types/line-with-navigation-wrapper';
import { logger } from '../../config/logger';
import { GeoJsonLineWrapper__Output } from '../../../generated/uk/co/fivium/grpc/gis/GeoJsonLineWrapper';
import { isApproximatelyEqual } from './migration-utils';
import { getLineStartAndEndPoints } from './migration-line-utils';

/**
 * Orients all lines so they form a continuous chain within each ring.
 * The first line's direction is determined by its connection to the second line,
 * then each subsequent line is oriented to match the previous line's end point.
 * @param idToLineWithNavigationWrapper A map of line IDs to their corresponding {@link LineWithNavigationTypeAndId} wrappers.
 * @param linesWithType A list of {@link GeoJsonLineWrapper__Output} which contains the line IDs, ring numbers, and connection orders.
 * @returns Nothing, as it will update the lines in idToLineWithNavigationWrapper in place.
 */
export function fixDirectionOfAllLines(
  idToLineWithNavigationWrapper: Map<number, LineWithNavigationTypeAndId>,
  linesWithType: GeoJsonLineWrapper__Output[],
) {
  for (const [ringNumber, lines] of getRingToLineIdAndConnectionOrder(linesWithType)) {
    lines.sort((a, b) => a.connectionOrder - b.connectionOrder);
    if (lines.length <= 1) {
      continue;
    }

    fixDirectionOfFirstLine(lines, idToLineWithNavigationWrapper);
    fixDirectionOfSubsequentLines(lines, idToLineWithNavigationWrapper);

    logger.debug(`Ring ${ringNumber}: All ${lines.length} lines oriented successfully`);
  }
}

function fixDirectionOfFirstLine(
  lines: { connectionOrder: number; id: number }[],
  idToLineWithNavigationWrapper: Map<number, LineWithNavigationTypeAndId>,
): void {
  const first = getWrapperOrThrow(lines[0].id, idToLineWithNavigationWrapper);
  const second = getWrapperOrThrow(lines[1].id, idToLineWithNavigationWrapper);

  const { endPoint: firstEnd } = getLineStartAndEndPoints(first.line);
  const { startPoint: secondStart, endPoint: secondEnd } = getLineStartAndEndPoints(second.line);

  const firstLineEndNodeConnectsToEitherNextNode =
    isApproximatelyEqual(firstEnd, secondStart) || isApproximatelyEqual(firstEnd, secondEnd);
  if (firstLineEndNodeConnectsToEitherNextNode) {
    return;
  }

  const { startPoint: firstStart } = getLineStartAndEndPoints(first.line);
  const firstLineStartNodeConnectsToEitherNextNode =
    isApproximatelyEqual(firstStart, secondStart) || isApproximatelyEqual(firstStart, secondEnd);

  if (firstLineStartNodeConnectsToEitherNextNode) {
    reverseLine(first);
    return;
  }

  throw new Error(`First line ${lines[0].id} does not connect to second line ${lines[1].id} in either orientation`);
}

function fixDirectionOfSubsequentLines(
  lines: { connectionOrder: number; id: number }[],
  idToLineWithNavigationWrapper: Map<number, LineWithNavigationTypeAndId>,
): void {
  for (let i = 0; i < lines.length - 1; i++) {
    const current = getWrapperOrThrow(lines[i].id, idToLineWithNavigationWrapper);
    const next = getWrapperOrThrow(lines[i + 1].id, idToLineWithNavigationWrapper);

    const { endPoint: currentEnd } = getLineStartAndEndPoints(current.line);
    const { startPoint: nextStart, endPoint: nextEnd } = getLineStartAndEndPoints(next.line);

    if (isApproximatelyEqual(currentEnd, nextStart)) {
      continue;
    }

    if (isApproximatelyEqual(currentEnd, nextEnd)) {
      reverseLine(next);
      continue;
    }

    throw new Error(`Line ${lines[i + 1].id} does not connect to line ${lines[i].id} in either orientation`);
  }
}

function getWrapperOrThrow(id: number, map: Map<number, LineWithNavigationTypeAndId>): LineWithNavigationTypeAndId {
  const wrapper = map.get(id);
  if (wrapper === undefined) {
    throw new Error(`Line with id ${id} not found in idToLineWithNavigationWrapper`);
  }
  return wrapper;
}

function reverseLine(wrapper: LineWithNavigationTypeAndId): void {
  const reversedPath = [...wrapper.line.paths[0]].reverse();
  wrapper.line = new Polyline({
    paths: [reversedPath],
    spatialReference: wrapper.line.spatialReference,
  });
}

function getRingToLineIdAndConnectionOrder(
  linesWithType: GeoJsonLineWrapper__Output[],
): Map<number, { connectionOrder: number; id: number }[]> {
  const ringToLines = new Map<number, { connectionOrder: number; id: number }[]>();
  for (const line of linesWithType) {
    const ringNumber = Number(line.ringNumber);
    if (!ringToLines.has(ringNumber)) {
      ringToLines.set(ringNumber, []);
    }
    ringToLines.get(ringNumber)!.push({
      connectionOrder: Number(line.connectionOrder),
      id: line.oracleLineSsid,
    });
  }
  return ringToLines;
}
