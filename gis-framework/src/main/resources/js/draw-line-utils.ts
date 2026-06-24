import type {LinePoint} from "./grid-utils";

/**
 * Check if two points are orthogonal to each other (horizontal or vertical)
 * @param a 1st point
 * @param b 2nd point
 */
export function isOrthogonalSegment(a: LinePoint, b: LinePoint): boolean {
    const EPSILON = 1e-9;
    const sameX = Math.abs(a.originalSrsCoordinates[0] - b.originalSrsCoordinates[0]) < EPSILON;
    const sameY = Math.abs(a.originalSrsCoordinates[1] - b.originalSrsCoordinates[1]) < EPSILON;
    return sameX || sameY;
}