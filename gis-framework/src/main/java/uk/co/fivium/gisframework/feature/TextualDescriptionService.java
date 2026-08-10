package uk.co.fivium.gisframework.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.LineNavigationType;

@Service
public class TextualDescriptionService {

  private static final String LAYER_ATTRIBUTE = "LAYER";
  private static final String NAME_ATTRIBUTE = "NAME";
  private static final String SCRIBE_DESCRIPTION_ATTRIBUTE = "SCRIBE_DESCRIPTION";
  private static final String BOUNDARY_LINE_SCRIBE_DESCRIPTION_ATTRIBUTE = "BOUNDARY_LINE_SCRIBE_DESCRIPTION";

  /**
   * The stylesheet shipped as part of the description, so consuming services get the layout without any
   * extra CSS.
   */
  private static final String STYLE = """
      <style>
      .gis-textual-description { font-family: "GDS Transport"; font-size: 16px }
      .gis-textual-description p { margin: 0 0 1em; }
      .gis-textual-description .govuk-list--number { list-style-position: inside; }
      </style>""";

  private final LineService lineService;

  public TextualDescriptionService(LineService lineService) {
    this.lineService = lineService;
  }

  /**
   * Builds the textual description of every given feature as a single self-contained HTML fragment
   * (leading {@code <style>} block, then one block per feature), ready to render.
   */
  public String getTextualDescription(Collection<Feature> features) {
    var featuresById = features.stream()
        .collect(Collectors.toMap(Feature::getId, Function.identity()));

    var featureBlocks = lineService.getOrderedLinesByFeatureId(features, false).entrySet()
        .stream()
        .map(entry -> buildDescription(
            featuresById.get(entry.getKey()),
            lineService.getOutlineNumberedNodes(entry.getValue())))
        .collect(Collectors.joining("\n"));

    return "%s\n<div class=\"gis-textual-description\">\n%s\n</div>".formatted(STYLE, featureBlocks);
  }

  private String buildDescription(Feature feature, List<NumberedNode> nodes) {
    var rings = groupNodesByRing(nodes);
    var fullShapeName = buildFullShapeName(feature);

    var regionCount = (int) rings.stream()
        .filter(Ring::outerRing)
        .count();
    var innerRegionCount = rings.size() - regionCount;

    var sections = new ArrayList<String>();
    if (regionCount > 1 || innerRegionCount > 0) {
      sections.add(buildIntro(capitalise(fullShapeName), regionCount, innerRegionCount));
    }
    sections.addAll(buildRegionBlocks(feature, fullShapeName, rings, regionCount));
    sections.add(buildFooter(feature.getCoordinateSystem(), nodes));

    return "<div class=\"gis-textual-description__feature\">\n%s\n</div>".formatted(String.join("\n", sections));
  }

  /**
   * Generates the start of the textual description defining how many regions (polygons) exist.
   * If there is only 1 ring, then this text is not needed.
   *
   * @param fullShapeName    The full name of the shape, formatted as "[layer] [name attribute] ([feature_name])"
   * @param regionCount      Total number of regions (polygons)
   * @param innerRegionCount Total number of inner regions (holes)
   * @return The intro text.
   */
  private String buildIntro(String fullShapeName, int regionCount, int innerRegionCount) {
    var excludeClause = innerRegionCount > 0
        ? ", excluding %d inner %s".formatted(innerRegionCount, pluralise("region", innerRegionCount))
        : "";
    return "<p class=\"govuk-body\">%s is defined as %d %s%s:</p>".formatted(
        fullShapeName, regionCount, pluralise("region", regionCount), excludeClause);
  }

  /**
   * This generates the main bulk of the textual description, defining what coordinates link to what region.
   *
   * @param feature       The feature the textual description is about.
   * @param fullShapeName The full name of the shape, formatted as "[layer] [name attribute] ([feature_name])"
   * @param rings         A list of records which group all the lines per region together
   * @param regionCount   The number of regions (polygons)
   * @return List of HTML blocks for each region. Each block is a "bounded by" paragraph followed by a
   *     numbered list of the region's coordinates, and any legal boundary text. If one of the lines follows a
   *     treaty boundary, that is noted between the relevant coordinate rows.
   */
  private List<String> buildRegionBlocks(
      Feature feature,
      String fullShapeName,
      List<Ring> rings,
      int regionCount
  ) {
    var blocks = new ArrayList<String>();
    int regionCounter = 0;
    int excludedCounter = 0;

    for (var ring : rings) {
      var isSingleOuterRegion = ring.outerRing() && regionCount == 1;
      var regionLabel = ring.outerRing()
          ? "Region %d".formatted(++regionCounter)
          : "Excluded region %d".formatted(++excludedCounter);

      var heightLimit = buildHeightLimit(ring.polygon());
      String boundedByClause;
      if (isSingleOuterRegion) {
        boundedByClause = heightLimit == null
            ? "%s is bounded by the following coordinates:".formatted(capitalise(fullShapeName))
            : "%s is a strata ranging from %s and is bounded by the following coordinates:".formatted(
            capitalise(fullShapeName), heightLimit);
      } else {
        boundedByClause = heightLimit == null
            ? "%s of %s is bounded by the following coordinates:".formatted(regionLabel, fullShapeName)
            : "%s of %s is a strata ranging from %s and is bounded by the following coordinates:".formatted(
            regionLabel, fullShapeName, heightLimit);
      }

      var block = new StringBuilder()
          .append("<p class=\"govuk-body\">").append(boundedByClause).append("</p>\n")
          .append(buildCoordinateBlock(feature.getCoordinateSystem(), ring.nodes()));

      var legalText = buildLegalText(ring.nodes());
      if (legalText != null) {
        block.append('\n').append(legalText);
      }

      blocks.add(block.toString());
    }

    return blocks;
  }

  /**
   * This generates the coordinate block for a region. Coordinates are formatted according to their
   * Coordinate System and split into their two ordinate components so each column lines up. Where a line
   * follows a treaty boundary, it is noted between which two nodes this occurs
   *
   * @param coordinateSystem The coordinate system of the feature.
   * @param nodes            A list of the key outline nodes that define the region.
   * @return One or more coordinate lists, separated by any treaty boundary notes.
   */
  private String buildCoordinateBlock(CoordinateSystem coordinateSystem, List<NumberedNode> nodes) {
    var parts = new ArrayList<String>();
    var rows = new ArrayList<String>();
    var startNumber = nodes.getFirst().displayOrder();

    for (var i = 0; i < nodes.size(); i++) {
      var node = nodes.get(i);
      rows.add(formatCoordinateRow(coordinateSystem, node));

      var boundaryLineDescription = getStringAttribute(node.line().getAttributes(), BOUNDARY_LINE_SCRIBE_DESCRIPTION_ATTRIBUTE);
      if (boundaryLineDescription == null || i + 1 >= nodes.size()) {
        continue;
      }

      // The described boundary runs from this node to the next. Close the current list, emit the note as
      // its own paragraph, then start a new list for the following coordinate so the note text does not
      // interrupt the numbering.
      parts.add(coordinateList(rows, startNumber));
      rows = new ArrayList<>();
      startNumber = nodes.get(i + 1).displayOrder();
      parts.add("<p class=\"govuk-body\">thence following %s until coordinate:</p>"
          .formatted(escape(boundaryLineDescription)));
    }

    parts.add(coordinateList(rows, startNumber));
    return String.join("\n", parts);
  }

  /**
   * Renders the coordinate rows as an ordered list, numbered from {@code startNumber} so the numbers shown
   * continue across regions and boundary notes, matching the node numbers labelled on the map.
   */
  private String coordinateList(List<String> rows, int startNumber) {
    return "<ol class=\"govuk-list govuk-list--number\" start=\"%d\">%s</ol>"
        .formatted(startNumber, String.join("\n", rows));
  }

  private String buildFooter(CoordinateSystem coordinateSystem, List<NumberedNode> nodes) {
    var lines = new ArrayList<String>();
    lines.add("The above coordinates were specified using \"%s\"."
        .formatted(CoordinateSystemUtils.getDisplayName(coordinateSystem)));
    lines.addAll(buildNavigationClauses(nodes));
    return "<p class=\"govuk-body\">%s</p>".formatted(String.join("<br>\n", lines));
  }

  /**
   * Aggregates contiguous runs of lines that share a navigation type into one clause each, e.g. "the lines
   * joining coordinates (1) to (5) are navigated as loxodromes". Closing nodes are skipped as they are not the
   * start of a segment; a break in the running node number marks a ring boundary and ends the current run.
   */
  private List<String> buildNavigationClauses(List<NumberedNode> nodes) {
    var clauses = new ArrayList<String>();
    Integer runStart = null;
    int runEnd = 0;
    LineNavigationType runType = null;
    Integer previousNumber = null;

    for (var node : nodes) {
      if (node.ringClosingNode()) {
        continue;
      }
      var type = node.line().getNavigationType();
      var number = node.displayOrder();
      var contiguous = previousNumber != null && number == previousNumber + 1;

      if (runType != null && Objects.equals(runType, type) && contiguous) {
        runEnd = number + 1;
      } else {
        if (runType != null) {
          clauses.add(navigationClause(runStart, runEnd, runType));
        }
        runStart = number;
        runEnd = number + 1;
        runType = type;
      }
      previousNumber = number;
    }

    if (runType != null) {
      clauses.add(navigationClause(runStart, runEnd, runType));
    }
    return clauses;
  }

  private String navigationClause(int fromNumber, int toNumber, LineNavigationType navigationType) {
    return "The lines joining coordinates (%d) to (%d) are navigated as %ss.".formatted(
        fromNumber,
        toNumber,
        LineNavigationTypeUtils.getDisplayName(navigationType)
    );
  }

  /**
   * Groups consecutive nodes that share a polygon and ring number into a {@link Ring}. A ring is an outer ring
   * when it is the first ring of its polygon; the rings that follow within the same polygon are inner rings.
   */
  private List<Ring> groupNodesByRing(List<NumberedNode> nodes) {
    var rings = new ArrayList<Ring>();
    Line previousLine = null;

    for (var node : nodes) {
      var line = node.line();
      var samePolygon = previousLine != null && line.getPolygon().getId().equals(previousLine.getPolygon().getId());
      var sameRing = samePolygon && Objects.equals(line.getRingNumber(), previousLine.getRingNumber());

      if (!sameRing) {
        rings.add(new Ring(line.getPolygon(), !samePolygon, new ArrayList<>()));
      }
      rings.getLast().nodes().add(node);
      previousLine = line;
    }
    return rings;
  }

  private record Ring(Polygon polygon, boolean outerRing, List<NumberedNode> nodes) {
  }

  /**
   * Constructs the full name of the feature to be used in the textual description.
   *
   * @param feature The feature being described.
   * @return The full shape name in the format [layer] [name attribute] (feature_name) e.g. "subarea Subarea A (SHAPE 4)"
   */
  private String buildFullShapeName(Feature feature) {
    var layer = getLayer(feature);
    var name = getStringAttribute(feature.getAttributes(), NAME_ATTRIBUTE);

    var identifier = name != null
        ? "%s (%s)".formatted(escape(name), escape(feature.getFeatureName()))
        : escape(feature.getFeatureName());
    return layer == null
        ? identifier
        : "%s %s".formatted(lowercaseFirst(layer), identifier);
  }

  private String getLayer(Feature feature) {
    var layer = getStringAttribute(feature.getAttributes(), LAYER_ATTRIBUTE);
    if (layer == null) {
      return null;
    }
    try {
      return Layer.valueOf(layer).getDisplayName();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * This generates the correct text region introductory text depending on the depths of the region.
   * If the region has no defined depths, then no text is needed.
   *
   * @param polygon The region we want to generate the text for.
   * @return A description defining what depths the region spans.
   */
  private String buildHeightLimit(Polygon polygon) {
    if (polygon.getStartDepth() == null && polygon.getEndDepth() == null) {
      return null;
    }
    if (polygon.getStartDepth() == null) {
      return "infinity from mean sea level to %dm from mean sea level".formatted(polygon.getEndDepth());
    }
    if (polygon.getEndDepth() == null) {
      return "%dm from mean sea level to infinity".formatted(polygon.getStartDepth());
    }
    return "%dm from mean sea level to %dm from mean sea level".formatted(
        polygon.getStartDepth(), polygon.getEndDepth());
  }

  private String formatCoordinateRow(CoordinateSystem coordinateSystem, NumberedNode node) {
    return "<li class=\"govuk-!-font-tabular-numbers\">%s</li>"
        .formatted(CoordinateFormatter.formatCoordinate(coordinateSystem, node.x(), node.y()));
  }

  /**
   * This combines the legal text for each line into a paragraph.
   *
   * @param ring a list of all the outline nodes for a ring.
   * @return The combined legal text as an HTML paragraph, or null when there is none.
   */
  private String buildLegalText(List<NumberedNode> ring) {
    var legalText = ring.stream()
        .map(node -> getStringAttribute(node.line().getAttributes(), SCRIBE_DESCRIPTION_ATTRIBUTE))
        .filter(Objects::nonNull)
        .distinct()
        .map(this::escape)
        .collect(Collectors.joining("<br>\n"));
    return legalText.isBlank() ? null : "<p class=\"govuk-body\">%s</p>".formatted(legalText);
  }

  /**
   * Returns the given attribute as a string, or null when the attribute map is null or the attribute is
   * absent or blank.
   */
  private String getStringAttribute(Map<String, ?> attributes, String key) {
    if (attributes == null) {
      return null;
    }
    var value = attributes.get(key);
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return value.toString();
  }

  private String escape(String text) {
    return HtmlUtils.htmlEscape(text);
  }

  private String pluralise(String word, int count) {
    return count == 1 ? word : word + "s";
  }

  private String lowercaseFirst(String text) {
    return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
  }

  private String capitalise(String text) {
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }
}
