import type { CoordinateSystem } from "../../generated/uk/co/fivium/grpc/gis/CoordinateSystem";

export function getCoordinateSystemWkid(coordinateSystem: CoordinateSystem): number {
  switch (coordinateSystem) {
    case "ED50":
      return 4230;
    case "BRITISH_NATIONAL_GRID":
      return 27700;
    case "WGS84":
      return 4326;
    case "ETRS89":
      return 4258;
    default:
      throw new Error(`Could not determine wkid for ${coordinateSystem}`);
  }
}
