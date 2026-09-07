import * as generalizeOperator from "@arcgis/core/geometry/operators/generalizeOperator.js";
import * as unionOperator from "@arcgis/core/geometry/operators/unionOperator.js";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import { GENERALIZE_TOLERANCE_DEGREES } from "./generalize-polygon";

export function mergeAndGeneralizeLines(esriPolylinesJson: string[]): string {
  const polylines = esriPolylinesJson.map(line => Polyline.fromJSON(JSON.parse(line)));
  const result = unionOperator.executeMany(polylines) as Polyline;

  // remove vertices that are on straight lines.
  const cleanResult = generalizeOperator.execute(result, GENERALIZE_TOLERANCE_DEGREES) as Polyline;
  return JSON.stringify(cleanResult.toJSON());
}
