import type { FeaturePolylines__Output } from "../../generated/uk/co/fivium/grpc/gis/FeaturePolylines";
import type { PolygonPolylines__Output } from "../../generated/uk/co/fivium/grpc/gis/PolygonPolylines";
import type { EsriJsonPolygonWithId, FeaturePolygons } from "../types/feature-polygons";
import type { BuildFeaturePolygonsHandler } from "./handler-types";
import { buildPolygon } from "../geometric-operators/build-polygon";
import { esriJsonToPolyline } from "../util/esrijson-util";
import { syncHandler } from "./sync-handler";

/**
 * Builds the polygons of many features from their polyline EsriJSONs in a single call.
 * @param request GRPC request with, per feature, its coordinate system WKID and the polylines of each of its polygons as EsriJSON strings.
 * @returns Per feature, each of its polygons as an EsriJSON string, keyed by polygon id.
 */
export const buildFeaturePolygonsHandler: BuildFeaturePolygonsHandler = syncHandler(({ request }) => {
  const features = request.features.map(buildFeaturePolygons);
  return { features };
});

function buildFeaturePolygons(feature: FeaturePolylines__Output): FeaturePolygons {
  return {
    featureId: feature.featureId,
    polygons: feature.polygons.map(polygon => buildPolygonWithId(polygon, feature.coordinateSystemWkid)),
  };
}

function buildPolygonWithId(
  polygon: PolygonPolylines__Output,
  coordinateSystemWkid: number,
): EsriJsonPolygonWithId {
  const polylines = polygon.esriJsonPolylines.map(esriJsonToPolyline);
  const builtPolygon = buildPolygon(polylines, coordinateSystemWkid);

  if (!builtPolygon) {
    throw new Error(`No polygon could be built from the polylines of polygon ${polygon.polygonId}`);
  }

  return { id: polygon.polygonId, esriJsonPolygon: JSON.stringify(builtPolygon.toJSON()) };
}
