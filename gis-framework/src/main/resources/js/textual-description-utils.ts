import type { JsonFeatureOutlineNodes, JsonOutlineNode } from "./api/features.api";

export type TextPoint = {
  id: string;
  text: string,
  coordinates: [number, number],
};

/**
 * Groups outline nodes into renderable labels. Each ring is treated separately: the start/end nodes of
 * the same ring (same polygon and ring number) that share a coordinate are merged into one label, while
 * nodes sharing a coordinate across a different ring, polygon or feature keep their own labels.
 */
export function jsonFeatureNodesToTextPoints(featureNodes: JsonFeatureOutlineNodes[]): TextPoint[] {
  return featureNodes.flatMap((feature) => {
    const nodesByRingCoordinate = new Map<string, JsonOutlineNode[]>();
    for (const node of feature.nodes) {
      const key = `${node.polygonId}|${node.ringNumber}|${node.x},${node.y}`;
      const group = nodesByRingCoordinate.get(key) ?? [];
      group.push(node);
      nodesByRingCoordinate.set(key, group);
    }

    return [...nodesByRingCoordinate.values()].map((nodes) => {
      const displayOrders = nodes
        .map(node => node.displayOrder)
        .sort((a, b) => a - b);
      return {
        id: `${nodes[0].lineId}|${displayOrders[0]}`,
        text: `(${displayOrders.join(", ")})`,
        coordinates: [nodes[0].x, nodes[0].y],
      };
    });
  });
}
