package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
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
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.testharness.LicencePositionFeatureTestHarnessService.SeededFeatures;

@ExtendWith(MockitoExtension.class)
class LicencePositionFeatureTestHarnessServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder().withLicenceReference("P1").build();

  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();

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

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private CommandJourneyService commandJourneyService;

  @InjectMocks
  private LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService;

  @Captor
  private ArgumentCaptor<Feature> featureCaptor;

  @Captor
  private ArgumentCaptor<Polygon> polygonCaptor;

  @Captor
  private ArgumentCaptor<Collection<Line>> linesCaptor;

  @Test
  void createAndLinkFeatures_assertFourBlocksAndTwoSubareasPerBlockAreCreatedForTheLicence() {
    givenPositions(LicencePositionTestUtil.newBuilder().build());
    givenSpatialDataCanBePersisted();

    var seededFeatures = licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    assertThat(seededFeatures.count()).isEqualTo(12);

    verify(featureService, times(12)).saveFeature(featureCaptor.capture());

    var block1 = expectedFeature("test harness for P1 1",
        Map.of("LAYER", "BLOCKS", "QUADRANT_NO", "30", "BLOCK_NO", "1"), null);
    var subarea1a = expectedFeature("test harness for P1 2",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/1a"), block1);
    var subarea1b = expectedFeature("test harness for P1 3",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/1b"), block1);
    var block2 = expectedFeature("test harness for P1 4",
        Map.of("LAYER", "BLOCKS", "QUADRANT_NO", "30", "BLOCK_NO", "2"), null);
    var subarea2a = expectedFeature("test harness for P1 5",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/2a"), block2);
    var subarea2b = expectedFeature("test harness for P1 6",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/2b"), block2);
    var block3 = expectedFeature("test harness for P1 7",
        Map.of("LAYER", "BLOCKS", "QUADRANT_NO", "30", "BLOCK_NO", "3"), null);
    var subarea3a = expectedFeature("test harness for P1 8",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/3a"), block3);
    var subarea3b = expectedFeature("test harness for P1 9",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/3b"), block3);
    var block4 = expectedFeature("test harness for P1 10",
        Map.of("LAYER", "BLOCKS", "QUADRANT_NO", "30", "BLOCK_NO", "4"), null);
    var subarea4a = expectedFeature("test harness for P1 11",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/4a"), block4);
    var subarea4b = expectedFeature("test harness for P1 12",
        Map.of("LAYER", "SUBAREAS", "NAME", "30/4b"), block4);

    assertThat(featureCaptor.getAllValues())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "parentFeature.id")
        .containsExactly(block1, subarea1a, subarea1b, block2, subarea2a, subarea2b,
            block3, subarea3a, subarea3b, block4, subarea4a, subarea4b);
  }

  @Test
  void createAndLinkFeatures_assertTheEarliestPositionSurrendersTheFirstBlockAndOutputsTheRemainingBlocks() {
    var earliestPosition = LicencePositionTestUtil.newBuilder().build();
    givenPositions(earliestPosition, LicencePositionTestUtil.newBuilder().build());
    givenSpatialDataCanBePersisted();

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(featureService, times(12)).saveFeature(featureCaptor.capture());
    var blocks = featureCaptor.getAllValues().stream().filter(LicenceBlockFeatureUtil::isLicenceBlock).toList();
    var surrenderedBlock = blocks.getFirst();
    var retainedBlockIds = blocks.stream().skip(1).map(Feature::getId).toList();

    var expectedOperation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(surrenderedBlock.getId()))
        .withSurrenderDetails(Map.of(surrenderedBlock.getId(), new SurrenderDetails(
            BlockSurrenderType.FULL_SURRENDER, COMMAND_JOURNEY_ID, List.of(surrenderedBlock.getId()))))
        .withOutputFeatureIds(retainedBlockIds)
        .build();

    verify(licencePositionChangeService).createLicencePositionChange(
        earliestPosition, List.of(expectedOperation), 1, LicencePositionChangeStatus.CONSENTED);
  }

  @Test
  void createAndLinkFeatures_assertTheRetainedBlocksAreTheSeededBlocksWithoutTheSurrenderedOne() {
    givenPositions(LicencePositionTestUtil.newBuilder().build());
    givenSpatialDataCanBePersisted();

    var seededFeatures = licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(featureService, times(12)).saveFeature(featureCaptor.capture());
    var blocks = featureCaptor.getAllValues().stream().filter(LicenceBlockFeatureUtil::isLicenceBlock).toList();

    assertThat(seededFeatures.surrenderedBlock()).isEqualTo(blocks.getFirst());
    assertThat(seededFeatures.retainedBlocks()).isEqualTo(blocks.subList(1, blocks.size()));
  }

  @Test
  void createAndLinkFeatures_whenTheEarliestPositionHasNoChanges_thenTheSeedIsTheFirstChange() {
    var earliestPosition = LicencePositionTestUtil.newBuilder().build();
    givenPositions(earliestPosition);
    givenSpatialDataCanBePersisted();

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(licencePositionChangeService).createLicencePositionChange(
        eq(earliestPosition), anyList(), eq(1), eq(LicencePositionChangeStatus.CONSENTED));
  }

  @Test
  void createAndLinkFeatures_whenTheEarliestPositionAlreadyHasChanges_thenTheSeedIsAppendedAfterThem() {
    var earliestPosition = LicencePositionTestUtil.newBuilder().build();
    givenPositions(earliestPosition);
    givenSpatialDataCanBePersisted();
    when(licencePositionChangeService.findByLicencePositionId(earliestPosition.getId())).thenReturn(List.of(
        LicencePositionChangeTestUtil.newBuilder().withChangeOrder(1).build(),
        LicencePositionChangeTestUtil.newBuilder().withChangeOrder(3).build()));

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(licencePositionChangeService).createLicencePositionChange(
        eq(earliestPosition), anyList(), eq(4), eq(LicencePositionChangeStatus.CONSENTED));
  }

  @Test
  void createAndLinkFeatures_assertEachFeatureIsASquareOfFourLines() {
    givenPositions(LicencePositionTestUtil.newBuilder().build());
    givenSpatialDataCanBePersisted();

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(featureService, times(12)).saveFeature(featureCaptor.capture());
    verify(polygonService, times(12)).savePolygon(polygonCaptor.capture());
    verify(lineService, times(12)).saveLines(linesCaptor.capture());

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

    assertThat(licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE))
        .isEqualTo(new SeededFeatures(List.of(), List.of()));

    verifyNoInteractions(featureService, polygonService, lineService);
  }

  @Test
  void getSeedState_whenASpatialOperationHasOutputFeatures_thenFeaturesAreAlreadySeeded() {
    givenSeedOperationWithOutputs(List.of(UUID.randomUUID()));

    assertThat(licencePositionFeatureTestHarnessService.getSeedState(LICENCE))
        .isEqualTo(new LicencePositionFeatureSeedState(1, true));
  }

  @Test
  void getSeedState_whenTheOnlySpatialOperationHasNoOutputFeatures_thenFeaturesAreNotSeeded() {
    givenSeedOperationWithOutputs(List.of());

    assertThat(licencePositionFeatureTestHarnessService.getSeedState(LICENCE))
        .isEqualTo(new LicencePositionFeatureSeedState(1, false));
  }

  /**
   * The real services assign ids on save, which the seeded spatial operation then records.
   */
  private void givenSpatialDataCanBePersisted() {
    when(commandJourneyService.createAndAssignCommandJourney(anyList())).thenReturn(commandJourney());
    doAnswer(invocation -> {
      invocation.getArgument(0, Feature.class).setId(UUID.randomUUID());
      return null;
    }).when(featureService).saveFeature(any(Feature.class));
  }

  private void givenPositions(LicencePosition... licencePositions) {
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE))
        .thenReturn(List.of(licencePositions));
  }

  private void givenSeedOperationWithOutputs(List<UUID> outputFeatureIds) {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var operation = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(UUID.randomUUID()))
        .withOutputFeatureIds(outputFeatureIds)
        .build();

    givenPositions(licencePosition);
    when(licencePositionChangeService.findByLicencePositionIn(List.of(licencePosition)))
        .thenReturn(List.of(LicencePositionChangeTestUtil.newBuilder()
            .withLicencePosition(licencePosition)
            .withOperations(List.<LicenceOperation>of(operation))
            .build()));
  }

  private static CommandJourney commandJourney() {
    var commandJourney = new CommandJourney();
    commandJourney.setId(COMMAND_JOURNEY_ID);
    return commandJourney;
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
