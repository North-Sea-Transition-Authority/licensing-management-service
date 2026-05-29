import type Polygon from "@arcgis/core/geometry/Polygon";
import * as equalsOperator from "@arcgis/core/geometry/operators/equalsOperator";
import * as projectOperator from "@arcgis/core/geometry/operators/projectOperator.js";
import SpatialReference from "@arcgis/core/geometry/SpatialReference";
import { describe, expect, it } from "vitest";
import { CoordinateSystem } from "../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";
import { projectToWgs84 } from "../../src/geometric-operators/project-polygon";
import { getCoordinateSystemWkid } from "../../src/util/coordinate-system-utils";
import { ed50ToWgs84Transformation } from "../../src/util/projection-utils";
import { makePolygon } from "../test-utils/esrijson-test-util";

const wgs84 = new SpatialReference({ wkid: getCoordinateSystemWkid(CoordinateSystem.WGS84) });

describe("projectToWgs84", () => {
  it("should use the ED50 to WGS84 transformation when projecting an ED50 polygon", async () => {
    const polygon = makePolygon(
      [
        [
          [0, 0],
          [1, 0],
          [1, 1],
          [0, 0],
        ],
      ],
      getCoordinateSystemWkid(CoordinateSystem.ED50),
    );

    const result = await projectToWgs84(polygon);
    const expectedResult = projectOperator
      .execute(polygon, wgs84, { geographicTransformation: ed50ToWgs84Transformation }) as Polygon;

    expect(equalsOperator.execute(result, expectedResult)).toBe(true);
  });

  it("should not use a geographic transformation when projecting a non-ED50 polygon", async () => {
    const polygon = makePolygon(
      [
        [
          [0, 0],
          [1, 0],
          [1, 1],
          [0, 0],
        ],
      ],
      getCoordinateSystemWkid(CoordinateSystem.BRITISH_NATIONAL_GRID),
    );

    const result = await projectToWgs84(polygon);
    const expectedResult = projectOperator
      .execute(polygon, wgs84) as Polygon;

    expect(equalsOperator.execute(result, expectedResult)).toBe(true);
  });
});
