package uk.co.fivium.gisframework.feature;

import uk.co.fivium.grpc.gis.CoordinateSystem;

public class CoordinateSystemUtils {

  private CoordinateSystemUtils() {
    throw new IllegalStateException("Util class should not be instantiated");
  }

  public static int getWkid(CoordinateSystem coordinateSystem) {
    return switch (coordinateSystem) {
      case ED50 -> 4230;
      case BRITISH_NATIONAL_GRID -> 27700;
      case WGS84 -> 4326;
      case ETRS89 -> 4258;
      case COORDINATE_SYSTEM_UNSPECIFIED, UNRECOGNIZED ->
          throw new IllegalArgumentException("Unknown coordinate system '" + coordinateSystem + "'");
    };
  }

  public static String getDisplayName(CoordinateSystem coordinateSystem) {
    return switch (coordinateSystem) {
      case ED50 -> "European Datum 1950";
      case BRITISH_NATIONAL_GRID -> "British National Grid";
      case WGS84 -> "WGS 84";
      case ETRS89 -> "European Terrestrial Reference System 1989";
      case COORDINATE_SYSTEM_UNSPECIFIED, UNRECOGNIZED ->
          throw new IllegalArgumentException("Unknown coordinate system %s".formatted(coordinateSystem));
    };
  }
}
