import type { IntersectionStatus__Output } from "../../generated/uk/co/fivium/grpc/gis/IntersectionStatus";
import { IntersectionStatus } from "../../generated/uk/co/fivium/grpc/gis/IntersectionStatus";
import { esriJsonToPolygon } from "../util/esrijson-util";
import { intersectPolygons } from "./intersection-operator";
import { polygonsAreTopologicallyEqual } from "./polygon-equality-operator";
import { unionPolygonsOperator } from "./union-polygons-operator";

export interface EsriJsonPolygonWithId {
  id: string,
  esriJsonPolygon: string,
}

export interface FeaturePolygons {
  featureId: string,
  polygons: EsriJsonPolygonWithId[],
}

export interface PolygonCropResult {
  polygonId: string,
  status: IntersectionStatus__Output,
  esriJsonCroppedPolygon: string,
}

export interface FeatureCropResult {
  originalFeatureId: string,
  intersectionStatus: IntersectionStatus__Output,
  polygonCropResults: PolygonCropResult[],
}

/**
 * Crops each feature's polygons to the union of the boundary polygons.
 * @param esriJsonBoundaryPolygons The polygons that together form the boundary to crop to.
 * @param featuresToBeCropped The features to crop, each with its polygons.
 * @return Per feature, whether it lies fully outside, fully inside or across the boundary, along with every polygon
 *         that does not lie fully inside it. A cropped polygon carries the part of it inside the boundary.
 */
export function cropToBoundary(
  esriJsonBoundaryPolygons: string[],
  featuresToBeCropped: FeaturePolygons[],
): FeatureCropResult[] {
  const boundary = unionPolygonsOperator(esriJsonBoundaryPolygons.map(esriJsonToPolygon));
  const esriJsonBoundary = JSON.stringify(boundary.toJSON());

  return featuresToBeCropped.map(feature => cropFeature(esriJsonBoundary, feature));
}

function cropFeature(
  esriJsonBoundary: string,
  feature: FeaturePolygons,
): FeatureCropResult {
  const polygonResults = feature.polygons.map(polygon => cropPolygon(esriJsonBoundary, polygon));

  return {
    originalFeatureId: feature.featureId,
    intersectionStatus: featureStatus(polygonResults.map(result => result.status)),
    polygonCropResults: polygonResults.filter(result => result.status !== IntersectionStatus.FULLY_INSIDE),
  };
}

function cropPolygon(
  esriJsonBoundary: string,
  polygon: EsriJsonPolygonWithId,
): PolygonCropResult {
  const intersection = intersectPolygons(esriJsonBoundary, polygon.esriJsonPolygon);

  if (intersection === null) {
    return { polygonId: polygon.id, status: IntersectionStatus.FULLY_OUTSIDE, esriJsonCroppedPolygon: "" };
  }

  const esriJsonIntersection = JSON.stringify(intersection.toJSON());

  if (polygonsAreTopologicallyEqual(esriJsonIntersection, polygon.esriJsonPolygon)) {
    return { polygonId: polygon.id, status: IntersectionStatus.FULLY_INSIDE, esriJsonCroppedPolygon: "" };
  }

  return { polygonId: polygon.id, status: IntersectionStatus.CROPPED, esriJsonCroppedPolygon: esriJsonIntersection };
}

function featureStatus(polygonStatuses: IntersectionStatus__Output[]): IntersectionStatus__Output {
  if (polygonStatuses.every(status => status === IntersectionStatus.FULLY_OUTSIDE)) {
    return IntersectionStatus.FULLY_OUTSIDE;
  }

  if (polygonStatuses.every(status => status === IntersectionStatus.FULLY_INSIDE)) {
    return IntersectionStatus.FULLY_INSIDE;
  }

  return IntersectionStatus.CROPPED;
}
