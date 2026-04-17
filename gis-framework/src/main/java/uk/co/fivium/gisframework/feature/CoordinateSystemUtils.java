package uk.co.fivium.gisframework.feature;

import uk.co.fivium.grpc.CoordinateSystem;

public class CoordinateSystemUtils {

  private CoordinateSystemUtils() {
    throw new IllegalStateException("Util class should not be instantiated");
  }

  public static int getWkid(CoordinateSystem coordinateSystem) {
    return switch (coordinateSystem) {
      case ED50 -> 4230;
      case BRITISH_NATIONAL_GRID -> 27700;
      case WGS84 -> 4326;
      case COORDINATE_SYSTEM_UNSPECIFIED, UNRECOGNIZED ->
          throw new IllegalArgumentException("Unknown coordinate system '" + coordinateSystem + "'");
    };
  }
}
