import { describe, expect, it } from "vitest";
import { find2dOverlappingFeatures } from "../../src/geometric-operators/find-2d-overlapping-features-operator";

const BNG_WKID = 27700;
const ED50_WKID = 4230;
const ETRS89_WKID = 4258;

function rectangleEsriJson(
  minX: number,
  minY: number,
  maxX: number,
  maxY: number,
  wkid: number = BNG_WKID,
): string {
  const rings = [[
    [minX, minY],
    [minX, maxY],
    [maxX, maxY],
    [maxX, minY],
    [minX, minY],
  ]];

  return JSON.stringify({ rings, spatialReference: { wkid } });
}

function featureWithPolygons(featureId: string, esriJsonPolygons: string[]) {
  return {
    featureId,
    polygons: esriJsonPolygons.map((esriJsonPolygon, index) => ({ id: `${featureId}-${index}`, esriJsonPolygon })),
  };
}

describe("find2dOverlappingFeatures", () => {
  const shape = rectangleEsriJson(0, 0, 10, 10);

  it.each([
    ["FeaturePartlyOverShape", [rectangleEsriJson(8, 2, 14, 6)]],
    ["FeatureInsideShape", [rectangleEsriJson(2, 2, 4, 4)]],
    ["FeatureInsideShapeTouchingItsEdge", [rectangleEsriJson(6, 2, 10, 6)]],
    ["ShapeInsideFeature", [rectangleEsriJson(-5, -5, 15, 15)]],
    ["FeatureIdenticalToShape", [rectangleEsriJson(0, 0, 10, 10)]],
    ["OnlyOnePartOfFeatureOverlapsShape", [rectangleEsriJson(20, 20, 24, 24), rectangleEsriJson(8, 2, 14, 6)]],
  ])("find2dOverlappingFeatures_when%s_thenFeatureReturned", (_scenario, featurePolygons) => {
    const features = [featureWithPolygons("feature", featurePolygons)];

    const result = find2dOverlappingFeatures([shape], features);

    expect(result).toEqual(["feature"]);
  });

  it.each([
    ["FeatureOutsideShape", [rectangleEsriJson(20, 20, 24, 24)]],
    ["FeatureTouchesShapeAlongEdge", [rectangleEsriJson(10, 2, 14, 6)]],
    ["FeatureTouchesShapeAtCorner", [rectangleEsriJson(10, 10, 14, 14)]],
    ["FeatureHasNoPolygons", []],
  ])("find2dOverlappingFeatures_when%s_thenFeatureNotReturned", (_scenario, featurePolygons) => {
    const features = [featureWithPolygons("feature", featurePolygons)];

    const result = find2dOverlappingFeatures([shape], features);

    expect(result).toEqual([]);
  });

  it.each([
    ["BritishNationalGrid", BNG_WKID],
    ["Ed50", ED50_WKID],
    ["Etrs89", ETRS89_WKID],
  ])("find2dOverlappingFeatures_whenFeaturePartlyOverShapeIn%s_thenFeatureReturned", (_coordinateSystem, wkid) => {
    const features = [featureWithPolygons("feature", [rectangleEsriJson(1.5, 0.5, 3, 1.5, wkid)])];

    const result = find2dOverlappingFeatures([rectangleEsriJson(1, 1, 2, 2, wkid)], features);

    expect(result).toEqual(["feature"]);
  });

  it("find2dOverlappingFeatures_whenShapeHasSeveralPolygons_thenFeatureComparedWithTheirUnion", () => {
    const shapePolygons = [rectangleEsriJson(0, 0, 10, 10), rectangleEsriJson(10, 0, 20, 10)];
    const features = [featureWithPolygons("feature", [rectangleEsriJson(8, 2, 12, 6)])];

    const result = find2dOverlappingFeatures(shapePolygons, features);

    expect(result).toEqual(["feature"]);
  });

  it("find2dOverlappingFeatures_whenFeatureInsideHoleOfShape_thenFeatureNotReturned", () => {
    const shapeWithHole = JSON.stringify({
      rings: [
        [[0, 0], [0, 10], [10, 10], [10, 0], [0, 0]],
        [[3, 3], [7, 3], [7, 7], [3, 7], [3, 3]],
      ],
      spatialReference: { wkid: BNG_WKID },
    });
    const features = [featureWithPolygons("feature", [rectangleEsriJson(4, 4, 6, 6)])];

    const result = find2dOverlappingFeatures([shapeWithHole], features);

    expect(result).toEqual([]);
  });

  it("find2dOverlappingFeatures_whenSeveralFeatures_thenOnlyOverlappingFeaturesReturnedInOrder", () => {
    const features = [
      featureWithPolygons("inside", [rectangleEsriJson(2, 2, 4, 4)]),
      featureWithPolygons("outside", [rectangleEsriJson(20, 20, 24, 24)]),
      featureWithPolygons("partlyOver", [rectangleEsriJson(8, 2, 14, 6)]),
    ];

    const result = find2dOverlappingFeatures([shape], features);

    expect(result).toEqual(["inside", "partlyOver"]);
  });
});
