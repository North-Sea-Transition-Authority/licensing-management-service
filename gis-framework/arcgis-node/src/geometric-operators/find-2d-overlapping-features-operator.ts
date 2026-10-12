import type Polygon from "@arcgis/core/geometry/Polygon.js";
import type { FeaturePolygons } from "../types/feature-polygons";
import * as containsOperator from "@arcgis/core/geometry/operators/containsOperator.js";
import * as overlapsOperator from "@arcgis/core/geometry/operators/overlapsOperator.js";
import { esriJsonToPolygon } from "../util/esrijson-util";
import { unionPolygonsOperator } from "./union-polygons-operator";

/**
 * Finds the features that share any area with the union of the given polygons, in two dimensions. A feature that only
 * touches the polygons along an edge or at a corner does not count.
 * @param esriJsonPolygons The polygons that together form the shape to compare against.
 * @param featuresToCompare The features to compare, each with its polygons.
 * @return The ids of the features whose unioned polygons share any area with the shape, in the order they were given.
 */
export function find2dOverlappingFeatures(
  esriJsonPolygons: string[],
  featuresToCompare: FeaturePolygons[],
): string[] {
  const shape = unionPolygonsOperator(esriJsonPolygons.map(esriJsonToPolygon));

  return featuresToCompare
    .filter(feature => feature.polygons.length > 0)
    .filter(feature => sharesArea(shape, unionFeaturePolygons(feature)))
    .map(feature => feature.featureId);
}

function unionFeaturePolygons(feature: FeaturePolygons): Polygon {
  return unionPolygonsOperator(feature.polygons.map(polygon => esriJsonToPolygon(polygon.esriJsonPolygon)));
}

function sharesArea(polygon: Polygon, otherPolygon: Polygon): boolean {
  return overlapsOperator.execute(polygon, otherPolygon)
    || containsOperator.execute(polygon, otherPolygon)
    || containsOperator.execute(otherPolygon, polygon);
}
