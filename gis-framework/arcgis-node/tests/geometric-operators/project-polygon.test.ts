import type Point from "@arcgis/core/geometry/Point";
import type Polygon from "@arcgis/core/geometry/Polygon";
import * as equalsOperator from "@arcgis/core/geometry/operators/equalsOperator";
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference";
import { describe, expect, it } from "vitest";
import { CoordinateSystem } from "../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";
import { projectPointToWgs84, projectPolygonToWgs84 } from "../../src/geometric-operators/project-polygon";
import { getCoordinateSystemWkid } from "../../src/util/coordinate-system-utils";
import { ed50ToWgs84Transformation } from "../../src/util/projection-utils";
import { makePoint, makePolygon } from "../test-utils/esrijson-test-util";

const wgs84 = new SpatialReference({ wkid: getCoordinateSystemWkid(CoordinateSystem.WGS84) });

describe("projectPolygonToWgs84", () => {
  it.each([
    {
      description: "should use the ED50 to WGS84 transformation when projecting an ED50 polygon",
      coordinateSystem: CoordinateSystem.ED50,
      projectOptions: { geographicTransformation: ed50ToWgs84Transformation },
    },
    {
      description: "should not use a geographic transformation when projecting a non-ED50 polygon",
      coordinateSystem: CoordinateSystem.BRITISH_NATIONAL_GRID,
      projectOptions: {},
    },
  ])("$description", async ({ coordinateSystem, projectOptions }) => {
    const polygon = makePolygon(
      [
        [
          [0, 0],
          [1, 0],
          [1, 1],
          [0, 0],
        ],
      ],
      getCoordinateSystemWkid(coordinateSystem),
    );

    const result = await projectPolygonToWgs84(polygon);
    const expectedResult = projectOperator.execute(polygon, wgs84, projectOptions) as Polygon;

    expect(equalsOperator.execute(result, expectedResult)).toBe(true);
  });
});

describe("projectPointToWgs84", () => {
  it.each([
    {
      description: "should use the ED50 to WGS84 transformation when projecting an ED50 point",
      coordinateSystem: CoordinateSystem.ED50,
      projectOptions: { geographicTransformation: ed50ToWgs84Transformation },
    },
    {
      description: "should not use a geographic transformation when projecting a non-ED50 point",
      coordinateSystem: CoordinateSystem.BRITISH_NATIONAL_GRID,
      projectOptions: {},
    },
  ])("$description", async ({ coordinateSystem, projectOptions }) => {
    const point = makePoint(1, 1, getCoordinateSystemWkid(coordinateSystem));

    const result = await projectPointToWgs84(point);
    const expectedResult = projectOperator.execute(point, wgs84, projectOptions) as Point;

    expect(equalsOperator.execute(result, expectedResult)).toBe(true);
  });
});
