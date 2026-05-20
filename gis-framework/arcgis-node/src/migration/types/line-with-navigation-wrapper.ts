import type { GeoJsonLineWrapper__Output } from "../../../generated/uk/co/fivium/grpc/gis/GeoJsonLineWrapper";
import Polyline from "@arcgis/core/geometry/Polyline.js";
import * as Terraformer from "@terraformer/arcgis";
import { LineNavigationType } from "../../../generated/uk/co/fivium/grpc/gis/LineNavigationType";

export type LineWithNavigationTypeAndId = {
  line: Polyline,
  navigationType: LineNavigationType,
  id: number,
};

/**
 * This method does a simple conversion of a list of {@link GeoJsonLineWrapper__Output} to a map of
 * {@link LineWithNavigationTypeAndId} where the key is the id of the line.
 * Importantly, the GeoJSON is converted to EsriJSON with no alterations.
 * @param geoJsonLineWrappers a list of GeoJSON lines and associated attributes
 * @param wkid the Well Known ID of the spatial reference that the lines should be in.
 * @returns a Map of the id on the {@link GeoJsonLineWrapper__Output} to a {@link LineWithNavigationTypeAndId} which is a wrapper
 * around the EsriJSON Polyline and contains the navigation type and id of the line.
 */
export function geoJsonLineInputToLinesWithNavigationTypeAndId(
  geoJsonLineWrappers: GeoJsonLineWrapper__Output[],
  wkid: number,
): Map<number, LineWithNavigationTypeAndId> {
  const idToLineWithNavigationWrapper: Map<number, LineWithNavigationTypeAndId> = new Map();
  geoJsonLineWrappers.forEach((geoJsonLineWrapper) => {
    const { oracleLineSsid, geoJsonString } = geoJsonLineWrapper;

    if (oracleLineSsid === null) {
      throw new Error("GeoJsonLineWrapper is missing required field: oracleLineSsid");
    }

    if (geoJsonString === null) {
      throw new Error(`GeoJsonLineWrapper with oracleLineSsid ${oracleLineSsid} is missing required field: geoJsonString`);
    }

    const line: Polyline = Polyline.fromJSON(Terraformer.geojsonToArcGIS(JSON.parse(geoJsonString)));
    line.spatialReference = { wkid };

    idToLineWithNavigationWrapper.set(oracleLineSsid, {
      line,
      navigationType: geoJsonLineWrapper.isGeodesic ? LineNavigationType.GEODESIC : LineNavigationType.LOXODROME,
      id: oracleLineSsid,
    });
  });
  return idToLineWithNavigationWrapper;
}
