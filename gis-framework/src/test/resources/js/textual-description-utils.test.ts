import type { JsonOutlineNode } from "../../../main/resources/js/api/features.api";
import { describe, expect, it } from "vitest";
import { jsonFeatureNodesToTextPoints } from "../../../main/resources/js/textual-description-utils";

function node(
  polygonId: string,
  ringNumber: number,
  displayOrder: number,
  x: number,
  y: number,
  lineId: string,
  mapText: string,
): JsonOutlineNode {
  return { polygonId, lineId, ringNumber, displayOrder, x, y, mapText };
}

describe("textualDescriptionsUtils", () => {
  it("jsonFeatureNodesToTextPoints_whenNodesShareRingCoordinate_thenDeduplicatedKeepingFirst", () => {
    const featureNodes = [
      {
        featureId: "feature-1",
        nodes: [
          node("polygon-1", 1, 1, 0, 0, "line-1", "(1, 4)"),
          node("polygon-1", 1, 2, 10, 0, "line-2", "(2)"),
          node("polygon-1", 1, 4, 0, 0, "line-4", "(1, 4)"),
        ],
      },
    ];

    const result = jsonFeatureNodesToTextPoints(featureNodes);

    expect(result).toEqual([
      { id: "line-1|1", text: "(1, 4)", coordinates: [0, 0] },
      { id: "line-2|2", text: "(2)", coordinates: [10, 0] },
    ]);
  });

  it("jsonFeatureNodesToTextPoints_whenNodesShareCoordinateAcrossRingPolygonOrFeature_thenKeptSeparate", () => {
    const featureNodes = [
      {
        featureId: "feature-1",
        nodes: [
          node("polygon-1", 1, 1, 0, 0, "line-1", "(1)"),
          node("polygon-1", 2, 2, 0, 0, "line-2", "(2)"),
          node("polygon-2", 1, 3, 0, 0, "line-3", "(3)"),
        ],
      },
      {
        featureId: "feature-2",
        nodes: [
          node("polygon-3", 1, 1, 0, 0, "line-4", "(1)"),
        ],
      },
    ];

    const result = jsonFeatureNodesToTextPoints(featureNodes);

    expect(result).toEqual([
      { id: "line-1|1", text: "(1)", coordinates: [0, 0] },
      { id: "line-2|2", text: "(2)", coordinates: [0, 0] },
      { id: "line-3|3", text: "(3)", coordinates: [0, 0] },
      { id: "line-4|1", text: "(1)", coordinates: [0, 0] },
    ]);
  });
});
