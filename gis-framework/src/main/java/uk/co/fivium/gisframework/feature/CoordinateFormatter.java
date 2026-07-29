package uk.co.fivium.gisframework.feature;

import java.util.Locale;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.gov.dstl.geo.osgb.NationalGrid;

/**
 * Formats a feature's native coordinates for the textual description. Onshore features
 * ({@link CoordinateSystem#BRITISH_NATIONAL_GRID}) are shown as an OS grid reference (e.g. {@code TG 5140 1317});
 * offshore features (geographic datums such as {@link CoordinateSystem#ED50}) are shown as degrees/minutes/seconds
 * with the seconds rounded to 3 decimal places.
 */
public class CoordinateFormatter {

  /**
   * Degrees/minutes/seconds pattern for Apache SIS {@link AngleFormat}: unpadded degrees, two-digit minutes and
   * seconds, and three decimal places of seconds, producing e.g. {@code 53°30′00.000″N}. Uses the degree (°),
   * prime (′) and double-prime (″) symbols.
   */
  private static final String DMS_PATTERN = "D°MM′SS.sss″";

  private CoordinateFormatter() {
    throw new IllegalStateException("Util class should not be instantiated");
  }

  /**
   * Formats a coordinate for display. British National Grid coordinates become a single OS grid
   * reference; geographic datums become the latitude followed by the longitude, separated by a space.
   *
   * @param coordinateSystem the feature's coordinate system, determining the output style
   * @param x                the native x ordinate (longitude for geographic datums, easting for British National Grid)
   * @param y                the native y ordinate (latitude for geographic datums, northing for British National Grid)
   * @return the formatted coordinate
   */
  public static String formatCoordinate(CoordinateSystem coordinateSystem, double x, double y) {
    return switch (coordinateSystem) {
      case BRITISH_NATIONAL_GRID -> toGridReference(x, y);
      case ED50, WGS84, ETRS89 -> {
        var format = new AngleFormat(DMS_PATTERN, Locale.ROOT);
        yield "%s %s".formatted(format.format(new Latitude(y)), format.format(new Longitude(x)));
      }
      case COORDINATE_SYSTEM_UNSPECIFIED, UNRECOGNIZED ->
          throw new IllegalArgumentException("Unknown coordinate system '" + coordinateSystem + "'");
    };
  }

  /**
   * Converts a British National Grid easting/northing (in metres) to a spaced OS grid reference, e.g.
   * {@code TG 5140 1317}. {@link NationalGrid#toNationalGrid} yields the square letters plus a five-digit
   * easting and northing (1 m precision, e.g. {@code TG 51409 13178}); each group is reduced to four
   * digits for a 10 m grid reference, matching the frontend's {@code OsGridRef.toString(8)} in
   * {@code grid-utils.ts}.
   */
  private static String toGridReference(double easting, double northing) {
    var reference = NationalGrid.toNationalGrid(new double[] {easting, northing})
        .orElseThrow(() -> new IllegalArgumentException(
            "Coordinate (%s, %s) is outside the British National Grid".formatted(easting, northing)));
    var parts = reference.split(" ");
    return "%s %s %s".formatted(parts[0], parts[1].substring(0, 4), parts[2].substring(0, 4));
  }
}
