import type { UnionPolygonsHandler } from "./handler-types";
import { unionPolygonsOperator } from "../geometric-operators/union-polygons-operator";
import { esriJsonToPolygon } from "../util/esrijson-util";
import { asyncHandler } from "./async-handler";

export const unionPolygonsHandler: UnionPolygonsHandler = asyncHandler(async (call) => {
  const polygons = call.request.esriJsonPolygons.map(esriJsonToPolygon);
  const unioned = unionPolygonsOperator(polygons);
  return { esriJsonPolygon: JSON.stringify(unioned.toJSON()) };
});
