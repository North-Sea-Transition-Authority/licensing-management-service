package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.LineNavigationType;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

@ExtendWith(MockitoExtension.class)
class LicencePositionFeatureTestHarnessServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceReference("P1").build();

  private static final BigDecimal FEATURE_AREA = BigDecimal.valueOf(200000000);

  private static final String SOUTHERN_EDGE = """
      {"spatialReference":{"wkid":4230},"paths":[[[2.8,53.8333333333333],[3.0,53.8333333333333]]]}""";
  private static final String EASTERN_EDGE = """
      {"spatialReference":{"wkid":4230},"paths":[[[3.0,53.8333333333333],[3.0,54.0]]]}""";
  private static final String NORTHERN_EDGE = """
      {"spatialReference":{"wkid":4230},"paths":[[[3.0,54.0],[2.8,54.0]]]}""";
  private static final String WESTERN_EDGE = """
      {"spatialReference":{"wkid":4230},"paths":[[[2.8,54.0],[2.8,53.8333333333333]]]}""";

  @Mock
  private FeatureService featureService;

  @Mock
  private PolygonService polygonService;

  @Mock
  private LineService lineService;

  @Mock
  private LicencePositionService licencePositionService;

  @InjectMocks
  private LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService;

  @Captor
  private ArgumentCaptor<Feature> featureCaptor;

  @Captor
  private ArgumentCaptor<Polygon> polygonCaptor;

  @Captor
  private ArgumentCaptor<Collection<Line>> linesCaptor;

  @Test
  void createAndLinkFeatures_assertTwoBlocksAndTwoSubareasPerBlockLinkedToEachPosition() {
    var firstPosition = LicencePositionTestUtil.newBuilder().build();
    var secondPosition = LicencePositionTestUtil.newBuilder().build();
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(firstPosition, secondPosition));

    var createdFeatureCount = licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    assertThat(createdFeatureCount).isEqualTo(12);

    verify(featureService, times(12)).saveFeature(featureCaptor.capture());
    var features = featureCaptor.getAllValues();

    var block1 = expectedFeature("test harness for P1 1.1",
        Map.of("LAYER", "BLOCKS", "QUADRANT_NO", "30", "BLOCK_NO", "1"), null);
    var subarea1a = expectedFeature("test harness for P1 1.2",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/1a"), block1);
    var subarea1b = expectedFeature("test harness for P1 1.3",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/1b"), block1);
    var block2 = expectedFeature("test harness for P1 1.4",
        Map.of("LAYER", "BLOCKS", "QUADRANT_NO", "30", "BLOCK_NO", "2"), null);
    var subarea2a = expectedFeature("test harness for P1 1.5",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/2a"), block2);
    var subarea2b = expectedFeature("test harness for P1 1.6",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/2b"), block2);
    var block3 = expectedFeature("test harness for P1 2.1",
        Map.of("LAYER", "BLOCKS", "QUADRANT_NO", "30", "BLOCK_NO", "3"), null);
    var subarea3a = expectedFeature("test harness for P1 2.2",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/3a"), block3);
    var subarea3b = expectedFeature("test harness for P1 2.3",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/3b"), block3);
    var block4 = expectedFeature("test harness for P1 2.4",
        Map.of("LAYER", "BLOCKS", "QUADRANT_NO", "30", "BLOCK_NO", "4"), null);
    var subarea4a = expectedFeature("test harness for P1 2.5",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/4a"), block4);
    var subarea4b = expectedFeature("test harness for P1 2.6",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/4b"), block4);

    assertThat(features)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(block1, subarea1a, subarea1b, block2, subarea2a, subarea2b,
            block3, subarea3a, subarea3b, block4, subarea4a, subarea4b);

    verify(licencePositionService).setFeatures(firstPosition, features.subList(0, 6));
    verify(licencePositionService).setFeatures(secondPosition, features.subList(6, 12));
  }

  @Test
  void createAndLinkFeatures_assertEachFeatureIsASquareOfFourLines() {
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(LicencePositionTestUtil.newBuilder().build()));

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(featureService, times(6)).saveFeature(featureCaptor.capture());
    verify(polygonService, times(6)).savePolygon(polygonCaptor.capture());
    verify(lineService, times(6)).saveLines(linesCaptor.capture());

    var expectedPolygons = featureCaptor.getAllValues().stream()
        .map(LicencePositionFeatureTestHarnessServiceTest::expectedPolygon)
        .toList();

    assertThat(polygonCaptor.getAllValues())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyElementsOf(expectedPolygons);

    assertThat(linesCaptor.getAllValues())
        .zipSatisfy(polygonCaptor.getAllValues(), (lines, polygon) -> assertThat(lines)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                expectedLine(polygon, SOUTHERN_EDGE, 1),
                expectedLine(polygon, EASTERN_EDGE, 2),
                expectedLine(polygon, NORTHERN_EDGE, 3),
                expectedLine(polygon, WESTERN_EDGE, 4)));
  }

  @Test
  void createAndLinkFeatures_whenLicenceHasNoPositions_assertNothingIsCreated() {
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of());

    assertThat(licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE)).isZero();

    verifyNoInteractions(featureService, polygonService, lineService);
  }

  @Test
  void getSeedState() {
    var licencePosition = LicencePositionTestUtil.newBuilder().withFeatureIds(Set.of(UUID.randomUUID())).build();
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of(licencePosition));

    assertThat(licencePositionFeatureTestHarnessService.getSeedState(LICENCE))
        .isEqualTo(new LicencePositionFeatureSeedState(1, true));
  }

  private static Feature expectedFeature(String featureName, Map<String, String> attributes, Feature parentFeature) {
    var feature = new Feature();
    feature.setFeatureName(featureName);
    feature.setCoordinateSystem(CoordinateSystem.ED50);
    feature.setFeatureArea(FEATURE_AREA);
    feature.setAttributes(attributes);
    feature.setParentFeature(parentFeature);
    return feature;
  }

  private static Polygon expectedPolygon(Feature feature) {
    var polygon = new Polygon();
    polygon.setFeature(feature);
    polygon.setAttributes(Map.of());
    return polygon;
  }

  private static Line expectedLine(Polygon polygon, String esriJson, int displayOrder) {
    var line = new Line();
    line.setPolygon(polygon);
    line.setEsriJson(esriJson);
    line.setDisplayOrder(displayOrder);
    line.setRingNumber(1);
    line.setNavigationType(LineNavigationType.LOXODROME);
    line.setAttributes(Map.of());
    return line;
  }
}
