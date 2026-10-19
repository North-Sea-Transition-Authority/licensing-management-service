import type Feature from "ol/Feature";
import type { Geometry } from "ol/geom";

export interface DepthRange {
  startDepth: number,
  endDepth: number,
}

export function startDepthOf(feature: Feature<Geometry>): number {
  return Number(feature.get("startDepth") ?? 0);
}

export function endDepthOf(feature: Feature<Geometry>): number {
  return Number(feature.get("endDepth") ?? Number.NEGATIVE_INFINITY);
}

export function canSplitAt(depthRanges: DepthRange[], depth: number): boolean {
  return depthRanges.some(range => range.endDepth < depth && depth < range.startDepth);
}

export function formatDepthRanges(depthRanges: DepthRange[]): string {
  return depthRanges
    .map(range => `${range.startDepth} to ${Number.isFinite(range.endDepth) ? range.endDepth : "-infinity"}`)
    .join(", ");
}

export function depthRangesOf(features: Feature<Geometry>[], featureId: string): DepthRange[] {
  const depthRanges = new Map<string, DepthRange>();
  for (const feature of features) {
    if (feature.get("featureId") === featureId) {
      const depthRange = { startDepth: startDepthOf(feature), endDepth: endDepthOf(feature) };
      depthRanges.set(`${depthRange.startDepth}:${depthRange.endDepth}`, depthRange);
    }
  }
  return [...depthRanges.values()].sort((a, b) => b.startDepth - a.startDepth);
}
