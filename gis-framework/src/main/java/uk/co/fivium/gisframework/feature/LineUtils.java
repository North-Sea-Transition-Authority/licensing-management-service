package uk.co.fivium.gisframework.feature;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class LineUtils {

  private LineUtils() {
    throw new IllegalStateException("Util class should not be instantiated");
  }

  /**
   * Gets the EsriJSON of lines in display order, ready to be combined into a polygon.
   *
   * @param lines the lines of a single polygon.
   * @return the EsriJSON of each line, sorted by display order.
   */
  public static List<String> getEsriJsonsInDisplayOrder(Collection<Line> lines) {
    return lines.stream()
        .sorted(Comparator.comparing(Line::getDisplayOrder))
        .map(Line::getEsriJson)
        .toList();
  }
}
