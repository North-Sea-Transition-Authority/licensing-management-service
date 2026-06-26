import type Point from "@arcgis/core/geometry/Point.js";
import type Polygon from "@arcgis/core/geometry/Polygon.js";
import type { GeometryUnion, GeometryWithoutMeshUnion } from "@arcgis/core/geometry/types";
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference.js";
import { CoordinateSystem } from "../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";
import { getCoordinateSystemWkid } from "../util/coordinate-system-utils";
import { ed50ToWgs84Transformation } from "../util/projection-utils";

/**
 * Projects a polygon to WGS84 using the NSTA's recommended transformation when possible.
 * For a more detailed explanation see the documentation `displaying-features-on-map.md` on the root folder.
 * @param polygon The polygon to project.
 * @returns The projected polygon.
 */
export async function projectPolygonToWgs84(polygon: Polygon): Promise<Polygon> {
  return await projectToWgs84(polygon) as Polygon;
}

/**
 * Projects a point to WGS84 using the NSTA's recommended transformation when possible.
 * For a more detailed explanation see the documentation `displaying-features-on-map.md` on the root folder.
 * @param point The point to project.
 * @returns The projected point.
 */
export async function projectPointToWgs84(point: Point): Promise<Point> {
  return await projectToWgs84(point) as Point;
}

async function projectToWgs84(geometry: GeometryUnion): Promise<GeometryWithoutMeshUnion> {
  if (!projectOperator.isLoaded()) {
    await projectOperator.load();
  }

  const wgs84 = new SpatialReference({ wkid: getCoordinateSystemWkid(CoordinateSystem.WGS84) });

  let projectOperatorOptionsOriginalSrsToWgs84 = {};
  if (geometry.spatialReference.wkid === getCoordinateSystemWkid(CoordinateSystem.ED50)) {
    projectOperatorOptionsOriginalSrsToWgs84 = { geographicTransformation: ed50ToWgs84Transformation };
  }

  return projectOperator.execute(geometry, wgs84, projectOperatorOptionsOriginalSrsToWgs84) as GeometryWithoutMeshUnion;
}
