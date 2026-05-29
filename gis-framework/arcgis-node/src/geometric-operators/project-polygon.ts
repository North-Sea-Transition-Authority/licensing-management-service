import type Polygon from "@arcgis/core/geometry/Polygon.js";
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
export async function projectToWgs84(polygon: Polygon): Promise<Polygon> {
  if (!projectOperator.isLoaded()) {
    await projectOperator.load();
  }

  const wgs84 = new SpatialReference({ wkid: getCoordinateSystemWkid(CoordinateSystem.WGS84) });

  let projectOperatorOptionsOriginalSrsToWgs84 = {};
  if (polygon.spatialReference.wkid === getCoordinateSystemWkid(CoordinateSystem.ED50)) {
    projectOperatorOptionsOriginalSrsToWgs84 = { geographicTransformation: ed50ToWgs84Transformation };
  }

  return projectOperator.execute(polygon, wgs84, projectOperatorOptionsOriginalSrsToWgs84) as Polygon;
}
