import Polygon from "@arcgis/core/geometry/Polygon.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";

/**
 * Converts an esriJson polygon to a Polygon
 * @param esriJson
 * @return arcGis Polygon
 */
export function esriJsonToPolygon(esriJson: string): Polygon {
  return Polygon.fromJSON(JSON.parse(esriJson)) as Polygon;
}

/**
 * Converts an esriJson polyline to a Polyline
 * @param esriJson
 * @return arcGis Polyline
 */
export function esriJsonToPolyline(esriJson: string): Polyline {
  return Polyline.fromJSON(JSON.parse(esriJson)) as Polyline;
}
