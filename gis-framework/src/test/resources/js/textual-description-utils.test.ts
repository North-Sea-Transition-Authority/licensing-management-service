import type { JsonOutlineNode } from "../../../main/resources/js/api/features.api";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { jsonFeatureNodesToTextPoints } from "../../../main/resources/js/textual-description-utils";

const mocks = vi.hoisted(() => ({
  toWgs84: vi.fn((x: number, y: number) => [x, y]),
}));

vi.mock("../../../main/resources/js/coordinate-system-utils", () => ({
  toWgs84: mocks.toWgs84,
}));

function node(
  polygonId: string,
  ringNumber: number,
  displayOrder: number,
  x: number,
  y: number,
  lineId: string,
): JsonOutlineNode {
  return { polygonId, lineId, ringNumber, displayOrder, x, y };
}

describe("textualDescriptionsUtils", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("jsonFeatureNodesToTextPoints", () => {
    const featureNodes = [
      {
        featureId: "feature-1",
        nodes: [
          node("polygon-1", 1, 1, 0, 0, "line-1"),
          node("polygon-1", 1, 2, 10, 0, "line-2"),
          node("polygon-1", 1, 3, 10, 10, "line-3"),
          node("polygon-1", 1, 4, 0, 0, "line-4"),
          node("polygon-1", 2, 5, 0, 0, "line-5"),
          node("polygon-2", 1, 6, 0, 0, "line-6"),
        ],
      },
      {
        featureId: "feature-2",
        nodes: [
          node("polygon-3", 1, 1, 0, 0, "line-7"),
        ],
      },
    ];

    const result = jsonFeatureNodesToTextPoints(featureNodes);

    expect(result).toEqual([
      { id: "line-1|1", text: "(1, 4)", coordinates: [0, 0] },
      { id: "line-2|2", text: "(2)", coordinates: [10, 0] },
      { id: "line-3|3", text: "(3)", coordinates: [10, 10] },
      { id: "line-5|5", text: "(5)", coordinates: [0, 0] },
      { id: "line-6|6", text: "(6)", coordinates: [0, 0] },
      { id: "line-7|1", text: "(1)", coordinates: [0, 0] },
    ]);
  });
});
