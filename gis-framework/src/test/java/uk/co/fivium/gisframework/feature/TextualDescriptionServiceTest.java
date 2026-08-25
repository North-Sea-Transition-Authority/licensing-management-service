package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.esri.core.geometry.Point;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.grpc.GrpcClientService;
import uk.co.fivium.gisframework.operator.LineWithStartEndPoints;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.LineNavigationType;

@ExtendWith(MockitoExtension.class)
class TextualDescriptionServiceTest {

  private static final String STYLE = """
      <style>
      .gis-textual-description { font-family: "GDS Transport"; font-size: 16px }
      .gis-textual-description p { margin: 0 0 1em; }
      .gis-textual-description .govuk-list--number { list-style-position: outside; padding-left: 2em; }
      </style>""";

  @Mock
  private LineRepository lineRepository;

  @Mock
  private GrpcClientService grpcClientService;

  private TextualDescriptionService textualDescriptionService;

  @BeforeEach
  void setUp() {
    var lineService = new LineService(lineRepository, grpcClientService);
    textualDescriptionService = new TextualDescriptionService(lineService);
  }

  @Test
  void getTextualDescription_whenSinglePolygonSingleRingOffshore_thenDescribesRegionInDms() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.ED50)
        .withFeatureName("30/1a")
        .withAttributes(Map.of("LAYER", "SUBAREAS"))
        .build();
    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withStartDepth(0L)
        .withEndDepth(100L)
        .build();

    var line1 = line(polygon, 0, 1, LineNavigationType.LOXODROME, null);
    var line2 = line(polygon, 0, 2, LineNavigationType.LOXODROME, null);
    var line3 = line(polygon, 0, 3, LineNavigationType.LOXODROME, null);
    var line4 = line(polygon, 0, 4, LineNavigationType.LOXODROME, null);

    var lines = List.of(line1, line2, line3, line4);
    var startEndPoints = List.of(
        startEnd(line1, 2.0, 53.0, 2.5, 53.0),
        startEnd(line2, 2.5, 53.0, 2.5, 53.5),
        startEnd(line3, 2.5, 53.5, 2.0, 53.5),
        startEnd(line4, 2.0, 53.5, 2.0, 53.0)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("Subarea 30/1a is a strata ranging from 0m from mean sea level to 100m from mean sea level " +
            "and is bounded by the following coordinates:"),
        coordinateList(1,
            latLong("53°00′00.000″N", "2°00′00.000″E"),
            latLong("53°00′00.000″N", "2°30′00.000″E"),
            latLong("53°30′00.000″N", "2°30′00.000″E"),
            latLong("53°30′00.000″N", "2°00′00.000″E"),
            latLong("53°00′00.000″N", "2°00′00.000″E")
        ),
        footer("European Datum 1950", "The lines joining coordinates (1) to (5) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getTextualDescription_whenMultiplePolygonsWithInnerRing_thenNumbersRegionsAndExcludedRegions() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("30/1a")
        .withAttributes(Map.of("LAYER", "SUBAREAS"))
        .build();
    var polygonA = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(100L).build();
    var polygonB = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(100L).build();

    var outerA1 = line(polygonA, 0, 1, LineNavigationType.LOXODROME, null);
    var outerA2 = line(polygonA, 0, 2, LineNavigationType.LOXODROME, null);
    var innerA1 = line(polygonA, 1, 3, LineNavigationType.LOXODROME, null);
    var innerA2 = line(polygonA, 1, 4, LineNavigationType.LOXODROME, null);
    var outerB1 = line(polygonB, 2, 5, LineNavigationType.LOXODROME, null);
    var outerB2 = line(polygonB, 2, 6, LineNavigationType.LOXODROME, null);

    var lines = List.of(outerA1, outerA2, innerA1, innerA2, outerB1, outerB2);
    var startEndPoints = List.of(
        bngStartEnd(outerA1, 1, 2),
        bngStartEnd(outerA2, 2, 1),
        bngStartEnd(innerA1, 4, 5),
        bngStartEnd(innerA2, 5, 4),
        bngStartEnd(outerB1, 7, 8),
        bngStartEnd(outerB2, 8, 7)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("Subarea 30/1a is defined as 2 regions, excluding 1 inner region:"),
        para("Region 1 of subarea 30/1a is a strata ranging from 0m from mean sea level to 100m from mean sea level " +
            "and is bounded by the following coordinates:"),
        coordinateList(1, gridReference(bngRef(1)), gridReference(bngRef(2)), gridReference(bngRef(1))),
        para("Excluded region 1 of subarea 30/1a is a strata ranging from 0m from mean sea level to 100m from mean " +
            "sea level and is bounded by the following coordinates:"),
        coordinateList(4, gridReference(bngRef(4)), gridReference(bngRef(5)), gridReference(bngRef(4))),
        para("Region 2 of subarea 30/1a is a strata ranging from 0m from mean sea level to 100m from mean sea level " +
            "and is bounded by the following coordinates:"),
        coordinateList(7, gridReference(bngRef(7)), gridReference(bngRef(8)), gridReference(bngRef(7))),
        footer("British National Grid",
            "The lines joining coordinates (1) to (3) are navigated as loxodromes.",
            "The lines joining coordinates (4) to (6) are navigated as loxodromes.",
            "The lines joining coordinates (7) to (9) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getTextualDescription_whenMixedNavigationTypes_thenAggregatesContiguousRunsPerType() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("30/1a")
        .withAttributes(Map.of("LAYER", "SUBAREAS"))
        .build();
    var polygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(100L).build();

    var line1 = line(polygon, 0, 1, LineNavigationType.LOXODROME, null);
    var line2 = line(polygon, 0, 2, LineNavigationType.LOXODROME, null);
    var line3 = line(polygon, 0, 3, LineNavigationType.GEODESIC, null);
    var line4 = line(polygon, 0, 4, LineNavigationType.GEODESIC, null);

    var lines = List.of(line1, line2, line3, line4);
    var startEndPoints = List.of(
        bngStartEnd(line1, 1, 2),
        bngStartEnd(line2, 2, 3),
        bngStartEnd(line3, 3, 4),
        bngStartEnd(line4, 4, 1)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("Subarea 30/1a is a strata ranging from 0m from mean sea level to 100m from mean sea level " +
            "and is bounded by the following coordinates:"),
        coordinateList(1,
            gridReference(bngRef(1)),
            gridReference(bngRef(2)),
            gridReference(bngRef(3)),
            gridReference(bngRef(4)),
            gridReference(bngRef(1))
        ),
        footer("British National Grid",
            "The lines joining coordinates (1) to (3) are navigated as loxodromes.",
            "The lines joining coordinates (3) to (5) are navigated as geodesics.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getTextualDescription_whenScribeDescriptionPresent_thenIncludesLegalText() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("30/1a")
        .withAttributes(Map.of("LAYER", "SUBAREAS"))
        .build();
    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withStartDepth(null)
        .withEndDepth(null)
        .build();

    var scribe = Map.<String, Object>of("SCRIBE_DESCRIPTION", "Boundary follows the median line");
    var line1 = line(polygon, 0, 1, LineNavigationType.LOXODROME, scribe);
    var line2 = line(polygon, 0, 2, LineNavigationType.LOXODROME, scribe);

    var lines = List.of(line1, line2);
    var startEndPoints = List.of(
        bngStartEnd(line1, 1, 2),
        bngStartEnd(line2, 2, 1)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("Subarea 30/1a is bounded by the following coordinates:"),
        coordinateList(1, gridReference(bngRef(1)), gridReference(bngRef(2)), gridReference(bngRef(1))),
        "<p class=\"govuk-body\">Boundary follows the median line</p>",
        footer("British National Grid", "The lines joining coordinates (1) to (3) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getTextualDescription_whenBoundaryLineScribeDescriptionPresent_thenIncludeLineDescription() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("30/1a")
        .withAttributes(Map.of("LAYER", "SUBAREAS"))
        .build();
    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withStartDepth(null)
        .withEndDepth(null)
        .build();

    var boundaryLineScribe = Map.<String, Object>of("BOUNDARY_LINE_SCRIBE_DESCRIPTION", "the mean high water mark");
    var line1 = line(polygon, 0, 1, LineNavigationType.LOXODROME, boundaryLineScribe);
    var line2 = line(polygon, 0, 2, LineNavigationType.LOXODROME, null);

    var lines = List.of(line1, line2);
    var startEndPoints = List.of(
        bngStartEnd(line1, 1, 2),
        bngStartEnd(line2, 2, 1)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("Subarea 30/1a is bounded by the following coordinates:"),
        coordinateList(1, gridReference(bngRef(1))),
        note("thence following the mean high water mark until coordinate:"),
        coordinateList(2, gridReference(bngRef(2)), gridReference(bngRef(1))),
        footer("British National Grid", "The lines joining coordinates (1) to (3) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getTextualDescription_whenFeatureHasNameAttribute_thenIncludesNameBesideFeatureName() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("SHAPE 4")
        .withAttributes(Map.of("LAYER", "SUBAREAS", "NAME", "Subarea A"))
        .build();
    var polygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(100L).build();

    var line1 = line(polygon, 0, 1, LineNavigationType.LOXODROME, null);
    var line2 = line(polygon, 0, 2, LineNavigationType.LOXODROME, null);

    var lines = List.of(line1, line2);
    var startEndPoints = List.of(
        bngStartEnd(line1, 1, 2),
        bngStartEnd(line2, 2, 1)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("Subarea Subarea A (SHAPE 4) is a strata ranging from 0m from mean sea level to 100m from " +
            "mean sea level and is bounded by the following coordinates:"),
        coordinateList(1, gridReference(bngRef(1)), gridReference(bngRef(2)), gridReference(bngRef(1))),
        footer("British National Grid", "The lines joining coordinates (1) to (3) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getTextualDescription_whenFeatureHasNoLayerAttribute_thenUsesFeatureNameOnly() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("30/1a")
        .withAttributes(Map.of())
        .build();
    var polygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(100L).build();

    var line1 = line(polygon, 0, 1, LineNavigationType.LOXODROME, null);
    var line2 = line(polygon, 0, 2, LineNavigationType.LOXODROME, null);

    var lines = List.of(line1, line2);
    var startEndPoints = List.of(
        bngStartEnd(line1, 1, 2),
        bngStartEnd(line2, 2, 1)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("30/1a is a strata ranging from 0m from mean sea level to 100m from mean sea level " +
            "and is bounded by the following coordinates:"),
        coordinateList(1, gridReference(bngRef(1)), gridReference(bngRef(2)), gridReference(bngRef(1))),
        footer("British National Grid", "The lines joining coordinates (1) to (3) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getTextualDescription_whenLayerAttributeUnrecognised_thenUsesFeatureNameOnly() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("30/1a")
        .withAttributes(Map.of("LAYER", "NOT_A_REAL_LAYER"))
        .build();
    var polygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(100L).build();

    var line1 = line(polygon, 0, 1, LineNavigationType.LOXODROME, null);
    var line2 = line(polygon, 0, 2, LineNavigationType.LOXODROME, null);

    var lines = List.of(line1, line2);
    var startEndPoints = List.of(
        bngStartEnd(line1, 1, 2),
        bngStartEnd(line2, 2, 1)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("30/1a is a strata ranging from 0m from mean sea level to 100m from mean sea level " +
            "and is bounded by the following coordinates:"),
        coordinateList(1, gridReference(bngRef(1)), gridReference(bngRef(2)), gridReference(bngRef(1))),
        footer("British National Grid", "The lines joining coordinates (1) to (3) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("depthsToStrataClause")
  void getTextualDescription_whenSingleRegionWithVaryingDepths_thenFormatsStrataClause(
      Long startDepth,
      Long endDepth,
      String expectedBoundedByLine
  ) {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("30/1a")
        .withAttributes(Map.of("LAYER", "SUBAREAS"))
        .build();
    var polygon = PolygonTestUtil.newBuilder()
        .withFeature(feature)
        .withStartDepth(startDepth)
        .withEndDepth(endDepth)
        .build();

    var line1 = line(polygon, 0, 1, LineNavigationType.LOXODROME, null);
    var line2 = line(polygon, 0, 2, LineNavigationType.LOXODROME, null);

    var lines = List.of(line1, line2);
    var startEndPoints = List.of(
        bngStartEnd(line1, 1, 2),
        bngStartEnd(line2, 2, 1)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para(expectedBoundedByLine),
        coordinateList(1, gridReference(bngRef(1)), gridReference(bngRef(2)), gridReference(bngRef(1))),
        footer("British National Grid", "The lines joining coordinates (1) to (3) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getTextualDescription_whenMultipleRegionsWithNoInnerRings_thenIntroOmitsExcludeClause() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("30/1a")
        .withAttributes(Map.of("LAYER", "SUBAREAS"))
        .build();
    var polygonA = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(null).withEndDepth(null).build();
    var polygonB = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(null).withEndDepth(null).build();

    var outerA1 = line(polygonA, 0, 1, LineNavigationType.LOXODROME, null);
    var outerA2 = line(polygonA, 0, 2, LineNavigationType.LOXODROME, null);
    var outerB1 = line(polygonB, 1, 3, LineNavigationType.LOXODROME, null);
    var outerB2 = line(polygonB, 1, 4, LineNavigationType.LOXODROME, null);

    var lines = List.of(outerA1, outerA2, outerB1, outerB2);
    var startEndPoints = List.of(
        bngStartEnd(outerA1, 1, 2),
        bngStartEnd(outerA2, 2, 1),
        bngStartEnd(outerB1, 4, 5),
        bngStartEnd(outerB2, 5, 4)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("Subarea 30/1a is defined as 2 regions:"),
        para("Region 1 of subarea 30/1a is bounded by the following coordinates:"),
        coordinateList(1, gridReference(bngRef(1)), gridReference(bngRef(2)), gridReference(bngRef(1))),
        para("Region 2 of subarea 30/1a is bounded by the following coordinates:"),
        coordinateList(4, gridReference(bngRef(4)), gridReference(bngRef(5)), gridReference(bngRef(4))),
        footer("British National Grid",
            "The lines joining coordinates (1) to (3) are navigated as loxodromes.",
            "The lines joining coordinates (4) to (6) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getTextualDescription_whenFeatureHasNameButNoLayer_thenUsesNameBesideFeatureName() {
    var feature = FeatureTestUtil.newBuilder()
        .withCoordinateSystem(CoordinateSystem.BRITISH_NATIONAL_GRID)
        .withFeatureName("SHAPE 4")
        .withAttributes(Map.of("NAME", "Subarea A"))
        .build();
    var polygon = PolygonTestUtil.newBuilder().withFeature(feature).withStartDepth(0L).withEndDepth(100L).build();

    var line1 = line(polygon, 0, 1, LineNavigationType.LOXODROME, null);
    var line2 = line(polygon, 0, 2, LineNavigationType.LOXODROME, null);

    var lines = List.of(line1, line2);
    var startEndPoints = List.of(
        bngStartEnd(line1, 1, 2),
        bngStartEnd(line2, 2, 1)
    );

    when(lineRepository.findAllByPolygon_FeatureIn(List.of(feature))).thenReturn(lines);
    when(grpcClientService.getLineStartAndEndPoints(lines, false)).thenReturn(startEndPoints);

    var result = textualDescriptionService.getTextualDescription(List.of(feature));

    var expected = document(String.join("\n",
        para("Subarea A (SHAPE 4) is a strata ranging from 0m from mean sea level to 100m from " +
            "mean sea level and is bounded by the following coordinates:"),
        coordinateList(1, gridReference(bngRef(1)), gridReference(bngRef(2)), gridReference(bngRef(1))),
        footer("British National Grid", "The lines joining coordinates (1) to (3) are navigated as loxodromes.")
    ));

    assertThat(result).isEqualTo(expected);
  }

  private static Stream<Arguments> depthsToStrataClause() {
    return Stream.of(
        Arguments.of(0L, 100L,
            "Subarea 30/1a is a strata ranging from 0m from mean sea level to 100m from mean sea level " +
                "and is bounded by the following coordinates:"),
        Arguments.of(null, 100L,
            "Subarea 30/1a is a strata ranging from infinity from mean sea level to 100m from mean sea level " +
                "and is bounded by the following coordinates:"),
        Arguments.of(50L, null,
            "Subarea 30/1a is a strata ranging from 50m from mean sea level to infinity " +
                "and is bounded by the following coordinates:"),
        Arguments.of(null, null,
            "Subarea 30/1a is bounded by the following coordinates:")
    );
  }

  private static String document(String featureInner) {
    return STYLE + "\n" +
        "<div class=\"gis-textual-description\">\n" +
        "<div class=\"gis-textual-description__feature\">\n" +
        featureInner + "\n" +
        "</div>\n" +
        "</div>";
  }

  private static String para(String text) {
    return "<p class=\"govuk-body\">%s</p>".formatted(text);
  }

  private static String coordinateList(int startNumber, String... items) {
    return "<ol class=\"govuk-list govuk-list--number\" start=\"%d\">%s</ol>"
        .formatted(startNumber, String.join("\n", items));
  }

  private static String latLong(String latitude, String longitude) {
    return "<li class=\"govuk-!-font-tabular-numbers\" style=\"padding-left:0.5em\">%s %s</li>"
        .formatted(latitude, longitude);
  }

  private static String gridReference(String reference) {
    return "<li class=\"govuk-!-font-tabular-numbers\" style=\"padding-left:0.5em\">%s</li>".formatted(reference);
  }

  private static String note(String text) {
    return "<p class=\"govuk-body\">%s</p>".formatted(text);
  }

  private static String footer(String datum, String... navigationClauses) {
    var footer = new StringBuilder("The above coordinates were specified using \"%s\".".formatted(datum));
    for (var clause : navigationClauses) {
      footer.append("<br>\n").append(clause);
    }
    return para(footer.toString());
  }

  private static Line line(
      Polygon polygon,
      int ringNumber,
      int displayOrder,
      LineNavigationType navigationType,
      Map<String, Object> attributes
  ) {
    var builder = LineTestUtil.newBuilder()
        .withPolygon(polygon)
        .withRingNumber(ringNumber)
        .withDisplayOrder(displayOrder)
        .withNavigationType(navigationType);
    if (attributes != null) {
      builder.withAttributes(attributes);
    }
    return builder.build();
  }

  private static LineWithStartEndPoints startEnd(Line line, double startX, double startY, double endX, double endY) {
    return new LineWithStartEndPoints(line, new Point(startX, startY), new Point(endX, endY));
  }

  /**
   * British National Grid start/end points for a line, where {@code startIndex}/{@code endIndex} select distinct
   * eastings/northings in the TQ 100 km square via {@link #bngPoint(int)}. Each index formats to {@link #bngRef(int)}.
   */
  private static LineWithStartEndPoints bngStartEnd(Line line, int startIndex, int endIndex) {
    return new LineWithStartEndPoints(line, bngPoint(startIndex), bngPoint(endIndex));
  }

  private static Point bngPoint(int index) {
    return new Point(500_000 + index * 1_000, 100_000 + index * 1_000);
  }

  /**
   * The 10 m OS grid reference that {@link #bngPoint(int)} formats to for a single-digit index (all inside the TQ
   * 100 km square), e.g. index 1 -&gt; {@code TQ 0100 0100}.
   */
  private static String bngRef(int index) {
    return "TQ 0%d00 0%d00".formatted(index, index);
  }
}
