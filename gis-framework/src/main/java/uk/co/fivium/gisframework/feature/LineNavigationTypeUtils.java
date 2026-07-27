package uk.co.fivium.gisframework.feature;

import uk.co.fivium.grpc.gis.LineNavigationType;

public class LineNavigationTypeUtils {

  private LineNavigationTypeUtils() {
    throw new IllegalStateException("Util class should not be instantiated");
  }

  public static String getDisplayName(LineNavigationType navigationType) {
    return switch (navigationType) {
      case LOXODROME -> "loxodrome";
      case GEODESIC -> "geodesic";
      case CARTESIAN -> "cartesian";
      case LINE_NAVIGATION_TYPE_UNSPECIFIED, UNRECOGNIZED ->
          throw new IllegalArgumentException("Unknown line navigation type %s".formatted(navigationType));
    };
  }
}
