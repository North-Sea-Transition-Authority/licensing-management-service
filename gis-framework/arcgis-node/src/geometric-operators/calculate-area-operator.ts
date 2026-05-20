import type Polygon from "@arcgis/core/geometry/Polygon.js";
import type Polyline from "@arcgis/core/geometry/Polyline.js";
import * as areaOperator from "@arcgis/core/geometry/operators/areaOperator.js";
import * as densifyOperator from "@arcgis/core/geometry/operators/densifyOperator.js";
import * as geodeticAreaOperator from "@arcgis/core/geometry/operators/geodeticAreaOperator.js";
import { CoordinateSystem } from "../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";
import { LineNavigationType } from "../../generated/uk/co/fivium/grpc/gis/LineNavigationType";
import { getCoordinateSystemWkid } from "../util/coordinate-system-utils";
import { linesToSinglePolygon } from "./lines-to-single-polygon-operator";

export interface LineWithNavigationType {
  line: Polyline,
  navigationType: LineNavigationType,
}

/**
 * Calculates the area of a polygon based on the provided coordinate system.
 *
 * For the British National Grid coordinate system, a planar area calculation is performed, as we don't need to take into account
 * the curvature of the earth.
 * For the ED50 coordinate system, a geodetic area calculation is performed using a geodesic curve type, so that we can take into
 * account the curvature of the earth
 *
 * @param polygon - The {@link Polygon} geometry for which to calculate the area.
 * @param coordinateSystem - The {@link CoordinateSystem} to use for the calculation.
 * @returns A promise that resolves to the calculated area in square meters.
 */
export async function calculateArea(polygon: Polygon, coordinateSystem: CoordinateSystem): Promise<number> {
  let area: number;

  if (coordinateSystem === CoordinateSystem.BRITISH_NATIONAL_GRID) {
    area = areaOperator.execute(polygon, { unit: "square-meters" });
  } else {
    if (!geodeticAreaOperator.isLoaded()) {
      await geodeticAreaOperator.load();
    }

    area = geodeticAreaOperator.execute(polygon, { curveType: "geodesic" });
  }

  return Math.abs(area);
}

/**
 * Densifies loxodrome lines and calculates the total area of the resulting polygon.
 *
 * Lines that are of type {@link LineNavigationType.LOXODROME} and are not in the
 * {@link CoordinateSystem.BRITISH_NATIONAL_GRID} are densified before being converted
 * to a single polygon for area calculation.
 *
 * We do this densification so that the geodetic area operator calculates a more accurate area.
 * By densifying the line, the length between each point is similar when measuring as geodesic or loxodrome.
 * Without the densification the difference between lengths can be quite drastic over a long distance and change the area
 * quite a bit.
 *
 * //TODO EPGF-85: link to our docs which might have a visual explanation of this.
 *
 * @param lineWithNavigationType - An array of {@link LineWithNavigationType} objects representing the boundaries.
 * @param coordinateSystem - The {@link CoordinateSystem} of the input lines and the target for calculation.
 * @returns A promise that resolves to the calculated area in square meters.
 */
export async function densifyLoxodromesAndCalculateArea(
  lineWithNavigationType: LineWithNavigationType[],
  coordinateSystem: CoordinateSystem,
): Promise<number> {
  const processed = [];

  for (const lineWrapper of lineWithNavigationType) {
    if (
      lineWrapper.navigationType === LineNavigationType.LOXODROME
      && coordinateSystem !== CoordinateSystem.BRITISH_NATIONAL_GRID
    ) {
      // The max segment length is derived from the spatial reference unit, which for ED50 is degrees.
      // There are 3600 arc seconds in 1 degrees. So this equates to 0.0055... degrees.
      // We fix it to 11 digits because as that equates to fractions of a milimeter.
      const polyline = densifyOperator.execute(lineWrapper.line, Number.parseFloat((20 / 3600).toFixed(11))) as Polyline;

      processed.push(polyline);
    } else {
      processed.push(lineWrapper.line);
    }
  }

  return await calculateArea(linesToSinglePolygon(processed, getCoordinateSystemWkid(coordinateSystem)), coordinateSystem);
}
