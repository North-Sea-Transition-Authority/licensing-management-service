import type { JsonFeatureOutlineNodes } from "./api/features.api";

export type TextPoint = {
  id: string;
  text: string,
  coordinates: [number, number],
};

export function jsonFeatureNodesToTextPoints(featureNodes: JsonFeatureOutlineNodes[]): TextPoint[] {
  return featureNodes.flatMap((feature) => {
    const seenRingCoordinates = new Set<string>();
    return feature.nodes
      .filter((node) => {
        const key = `${node.polygonId}|${node.ringNumber}|${node.x},${node.y}`;
        if (seenRingCoordinates.has(key)) {
          return false;
        }
        seenRingCoordinates.add(key);
        return true;
      })
      .map((node) => ({
        id: `${node.lineId}|${node.displayOrder}`,
        text: node.mapText,
        coordinates: [node.x, node.y] as [number, number],
      }));
  });
}
