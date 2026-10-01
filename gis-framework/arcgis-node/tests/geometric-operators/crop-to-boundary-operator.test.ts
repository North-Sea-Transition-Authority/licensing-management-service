import { describe, expect, it } from "vitest";
import { cropToBoundary } from "../../src/geometric-operators/crop-to-boundary-operator";
import { polygonsAreTopologicallyEqual } from "../../src/geometric-operators/polygon-equality-operator";

const BNG_WKID = 27700;

function rectangleEsriJson(
  minX: number,
  minY: number,
  maxX: number,
  maxY: number,
): string {
  const rings = [[
    [minX, minY],
    [minX, maxY],
    [maxX, maxY],
    [maxX, minY],
    [minX, minY],
  ]];

  return JSON.stringify({ rings, spatialReference: { wkid: BNG_WKID } });
}

describe("cropToBoundary", () => {
  const boundary = rectangleEsriJson(0, 0, 10, 10);

  it("cropToBoundary_whenFeatureFullyInsideBoundary_thenFullyInsideWithNoPolygons", () => {
    const features = [{ featureId: "feature", polygons: [{ id: "polygon", esriJsonPolygon: rectangleEsriJson(2, 2, 4, 4) }] }];

    const result = cropToBoundary([boundary], features);

    expect(result).toEqual([{ originalFeatureId: "feature", intersectionStatus: "FULLY_INSIDE", polygonCropResults: [] }]);
  });

  it("cropToBoundary_whenFeatureFullyInsideButEdgeTouchesBoundary_thenFullyInside", () => {
    const features = [{ featureId: "feature", polygons: [{ id: "polygon", esriJsonPolygon: rectangleEsriJson(6, 2, 10, 6) }] }];

    const result = cropToBoundary([boundary], features);

    expect(result).toEqual([{ originalFeatureId: "feature", intersectionStatus: "FULLY_INSIDE", polygonCropResults: [] }]);
  });

  it("cropToBoundary_whenFeatureFullyOutsideBoundary_thenFullyOutside", () => {
    const features = [{ featureId: "feature", polygons: [{ id: "polygon", esriJsonPolygon: rectangleEsriJson(20, 20, 24, 24) }] }];

    const result = cropToBoundary([boundary], features);

    expect(result).toEqual([{
      originalFeatureId: "feature",
      intersectionStatus: "FULLY_OUTSIDE",
      polygonCropResults: [{ polygonId: "polygon", status: "FULLY_OUTSIDE", esriJsonCroppedPolygon: "" }],
    }]);
  });

  it("cropToBoundary_whenFeatureOutsideButEdgeTouchesBoundary_thenFullyOutside", () => {
    const features = [{ featureId: "feature", polygons: [{ id: "polygon", esriJsonPolygon: rectangleEsriJson(10, 2, 14, 6) }] }];

    const result = cropToBoundary([boundary], features);

    expect(result).toEqual([{
      originalFeatureId: "feature",
      intersectionStatus: "FULLY_OUTSIDE",
      polygonCropResults: [{ polygonId: "polygon", status: "FULLY_OUTSIDE", esriJsonCroppedPolygon: "" }],
    }]);
  });

  it("cropToBoundary_whenFeatureStraddlesBoundary_thenCroppedToOverlappingPortion", () => {
    const features = [{ featureId: "feature", polygons: [{ id: "polygon", esriJsonPolygon: rectangleEsriJson(8, 2, 14, 6) }] }];

    const result = cropToBoundary([boundary], features);

    expect(result).toEqual([{
      originalFeatureId: "feature",
      intersectionStatus: "CROPPED",
      polygonCropResults: [{ polygonId: "polygon", status: "CROPPED", esriJsonCroppedPolygon: expect.any(String) }],
    }]);
    expect(polygonsAreTopologicallyEqual(result[0].polygonCropResults[0].esriJsonCroppedPolygon, rectangleEsriJson(8, 2, 10, 6))).toBe(true);
  });

  it("cropToBoundary_whenFeaturePolygonsInsideAndOutsideBoundary_thenCroppedWithOnlyOutsidePolygonReturned", () => {
    const features = [{
      featureId: "feature",
      polygons: [
        { id: "inside", esriJsonPolygon: rectangleEsriJson(2, 2, 4, 4) },
        { id: "outside", esriJsonPolygon: rectangleEsriJson(20, 20, 24, 24) },
      ],
    }];

    const result = cropToBoundary([boundary], features);

    expect(result).toEqual([{
      originalFeatureId: "feature",
      intersectionStatus: "CROPPED",
      polygonCropResults: [{ polygonId: "outside", status: "FULLY_OUTSIDE", esriJsonCroppedPolygon: "" }],
    }]);
  });

  it("cropToBoundary_whenBoundaryHasSeveralPolygons_thenFeatureCroppedToTheirUnion", () => {
    const boundaryPolygons = [rectangleEsriJson(0, 0, 10, 10), rectangleEsriJson(10, 0, 20, 10)];
    const features = [{ featureId: "feature", polygons: [{ id: "polygon", esriJsonPolygon: rectangleEsriJson(8, 2, 12, 6) }] }];

    const result = cropToBoundary(boundaryPolygons, features);

    expect(result).toEqual([{ originalFeatureId: "feature", intersectionStatus: "FULLY_INSIDE", polygonCropResults: [] }]);
  });

  it("cropToBoundary_whenSeveralFeatures_thenResultReturnedForEachFeature", () => {
    const features = [
      { featureId: "inside", polygons: [{ id: "inside-polygon", esriJsonPolygon: rectangleEsriJson(2, 2, 4, 4) }] },
      { featureId: "outside", polygons: [{ id: "outside-polygon", esriJsonPolygon: rectangleEsriJson(20, 20, 24, 24) }] },
    ];

    const result = cropToBoundary([boundary], features);

    expect(result).toEqual([
      { originalFeatureId: "inside", intersectionStatus: "FULLY_INSIDE", polygonCropResults: [] },
      {
        originalFeatureId: "outside",
        intersectionStatus: "FULLY_OUTSIDE",
        polygonCropResults: [{ polygonId: "outside-polygon", status: "FULLY_OUTSIDE", esriJsonCroppedPolygon: "" }],
      },
    ]);
  });
});
