package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.FeatureJourneyStateService;
import uk.co.fivium.gisframework.command.OperatorCommand;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.fivium.gisframework.command.TransformationType;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.testharness.LicencePositionFeatureTestHarnessService.SeededFeatures;

@ExtendWith(MockitoExtension.class)
class LicencePositionFeatureTestHarnessServiceTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceReference("GS1")
      .withLicenceType(LicenceType.GAS_STORAGE)
      .build();

  private static final Licence PRODUCTION_LICENCE = LicenceTestUtil.builder()
      .withId(2)
      .withLicenceReference("P1")
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .build();

  private static final Licence CARBON_STORAGE_LICENCE = LicenceTestUtil.builder()
      .withId(3)
      .withLicenceReference("CS1")
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .build();

  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();

  private static final BigDecimal FEATURE_AREA = BigDecimal.valueOf(200000000);
  private static final BigDecimal HALF_FEATURE_AREA = BigDecimal.valueOf(100000000);
  private static final BigDecimal QUARTER_FEATURE_AREA = BigDecimal.valueOf(50000000);

  private static final String FIRST_ROW_SOUTHERN_LATITUDE = "54";
  private static final String FIRST_ROW_NORTHERN_LATITUDE = "54.1666666666667";
  private static final String SECOND_ROW_SOUTHERN_LATITUDE = "54.25";
  private static final String SECOND_ROW_NORTHERN_LATITUDE = "54.4166666666667";

  private static final List<String> BLOCK_EDGES = firstRowEdges("3.6", "3.8");
  private static final List<String> WESTERN_HALF_EDGES = firstRowEdges("3.6", "3.7");
  private static final List<String> EASTERN_HALF_EDGES = firstRowEdges("3.7", "3.8");

  private static final LocalDate SURRENDER_DATE = LocalDate.of(2026, Month.AUGUST, 1);
  private static final LocalDate DAY_BEFORE_SURRENDER = LocalDate.of(2026, Month.JULY, 31);

  private static final LocalDate SUBAREA_CHANGE_DATE = LocalDate.of(2026, Month.SEPTEMBER, 1);
  private static final LocalDate DAY_BEFORE_SUBAREA_CHANGE = LocalDate.of(2026, Month.AUGUST, 31);

  private static final Feature BLOCK = FeatureTestUtil.builder()
      .withFeatureName("999/1")
      .withAttributes(blockAttributes(1))
      .withFeatureArea(FEATURE_AREA)
      .build();

  private static final CommandJourney COMMAND_JOURNEY = commandJourney();

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
  private OperatorCommandService operatorCommandService;

  @Mock
  private FeatureJourneyStateService featureJourneyStateService;

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

  @Captor
  private ArgumentCaptor<List<LicenceOperation>> operationsCaptor;

  @Test
  void createAndLinkFeatures_whenNeitherProductionNorCarbonStorageLicence_thenThreeBlocksWithAWholeBlockSubareaAreCreated() {
    givenPositions(LicencePositionTestUtil.newBuilder().build());
    givenFeaturesCanBePersisted();

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(featureService, times(6)).saveFeature(featureCaptor.capture());
    assertThat(featureCaptor.getAllValues())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(
            expectedFeature("999/1", blockAttributes(1)),
            expectedFeature("ALL", subareaAttributes("ALL")),
            expectedFeature("999/2", blockAttributes(2)),
            expectedFeature("ALL", subareaAttributes("ALL")),
            expectedFeature("999/3", blockAttributes(3)),
            expectedFeature("ALL", subareaAttributes("ALL"))
        );
  }

  @Test
  void createAndLinkFeatures_whenProductionLicence_thenFourBlocksAreCreatedWithTheSecondDividedIntoThreeSubareas() {
    givenPositions(PRODUCTION_LICENCE, LicencePositionTestUtil.newBuilder().build());
    givenFeaturesCanBePersisted();

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(PRODUCTION_LICENCE);

    verify(featureService, times(10)).saveFeature(featureCaptor.capture());
    assertThat(featureCaptor.getAllValues())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(
            expectedFeature("999/1", blockAttributes(1, PRODUCTION_LICENCE)),
            expectedFeature("ALL", subareaAttributes("ALL", PRODUCTION_LICENCE)),
            expectedFeature("999/2", blockAttributes(2, PRODUCTION_LICENCE)),
            expectedFeature("A", subareaAttributes("A", PRODUCTION_LICENCE), QUARTER_FEATURE_AREA, null),
            expectedFeature("B", subareaAttributes("B", PRODUCTION_LICENCE), HALF_FEATURE_AREA, null),
            expectedFeature("C", subareaAttributes("C", PRODUCTION_LICENCE), QUARTER_FEATURE_AREA, null),
            expectedFeature("999/3", blockAttributes(3, PRODUCTION_LICENCE)),
            expectedFeature("ALL", subareaAttributes("ALL", PRODUCTION_LICENCE)),
            expectedFeature("999/4", blockAttributes(4, PRODUCTION_LICENCE)),
            expectedFeature("ALL", subareaAttributes("ALL", PRODUCTION_LICENCE))
        );
  }

  @Test
  void createAndLinkFeatures_whenProductionLicence_assertTheSecondBlocksSubareasDivideItFromWestToEast() {
    givenPositions(PRODUCTION_LICENCE, LicencePositionTestUtil.newBuilder().build());
    givenFeaturesCanBePersisted();

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(PRODUCTION_LICENCE);

    verifyShapesSaved(List.of(
        BLOCK_EDGES,
        BLOCK_EDGES,
        firstRowEdges("3.9", "4.1"),
        firstRowEdges("3.9", "3.95"),
        firstRowEdges("3.95", "4.05"),
        firstRowEdges("4.05", "4.1"),
        firstRowEdges("4.2", "4.4"),
        firstRowEdges("4.2", "4.4"),
        firstRowEdges("4.5", "4.7"),
        firstRowEdges("4.5", "4.7")
    ));
  }

  @Test
  void createAndLinkFeatures_whenCarbonStorageLicence_thenOneBlockWithNoSubareasIsCreated() {
    givenPositions(CARBON_STORAGE_LICENCE, LicencePositionTestUtil.newBuilder().build());
    givenFeaturesCanBePersisted();

    var seededFeatures = licencePositionFeatureTestHarnessService.createAndLinkFeatures(CARBON_STORAGE_LICENCE);

    verify(featureService).saveFeature(featureCaptor.capture());
    var block = featureCaptor.getValue();
    assertThat(block)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(expectedFeature("999/1", blockAttributes(1, CARBON_STORAGE_LICENCE)));
    assertThat(seededFeatures).isEqualTo(new SeededFeatures(List.of(block), Map.of(block.getId(), List.of())));
  }

  @Test
  void createAndLinkFeatures_assertTheEarliestPositionCreatesTheBlocksAndTheirSubareas() {
    var earliestPosition = LicencePositionTestUtil.newBuilder().build();
    givenPositions(earliestPosition, LicencePositionTestUtil.newBuilder().build());
    givenFeaturesCanBePersisted();

    var seededFeatures = licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(featureService, times(6)).saveFeature(featureCaptor.capture());
    var features = featureCaptor.getAllValues();
    var blockIds = List.of(features.get(0).getId(), features.get(2).getId(), features.get(4).getId());
    assertThat(seededFeatures).isEqualTo(new SeededFeatures(
        List.of(features.get(0), features.get(2), features.get(4)),
        Map.of(
            blockIds.get(0), List.of(features.get(1)),
            blockIds.get(1), List.of(features.get(3)),
            blockIds.get(2), List.of(features.get(5))
        )
    ));

    var expectedOperation = LicenceOperation.newBlockCreateOperation()
        .withFeatureIds(blockIds)
        .withCreatedSubareas(Map.of(
            blockIds.get(0), List.of(new SubareaDetails(features.get(1).getId(), "ALL", "ALL")),
            blockIds.get(1), List.of(new SubareaDetails(features.get(3).getId(), "ALL", "ALL")),
            blockIds.get(2), List.of(new SubareaDetails(features.get(5).getId(), "ALL", "ALL"))
        ))
        .build();
    verify(licencePositionChangeService).createLicencePositionChange(
        eq(earliestPosition),
        operationsCaptor.capture(),
        eq(1),
        eq(LicencePositionChangeStatus.CONSENTED)
    );
    assertThat(operationsCaptor.getValue())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(expectedOperation);
  }

  @Test
  void createAndLinkFeatures_whenTheEarliestPositionHasNoChanges_thenTheSeedIsTheFirstChange() {
    var earliestPosition = LicencePositionTestUtil.newBuilder().build();
    givenPositions(earliestPosition);
    givenFeaturesCanBePersisted();

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(licencePositionChangeService).createLicencePositionChange(
        eq(earliestPosition), anyList(), eq(1), eq(LicencePositionChangeStatus.CONSENTED));
  }

  @Test
  void createAndLinkFeatures_whenTheEarliestPositionAlreadyHasChanges_thenTheSeedIsAppendedAfterThem() {
    var earliestPosition = LicencePositionTestUtil.newBuilder().build();
    givenPositions(earliestPosition);
    givenFeaturesCanBePersisted();
    when(licencePositionChangeService.findByLicencePositionId(earliestPosition.getId())).thenReturn(List.of(
        LicencePositionChangeTestUtil.newBuilder().withChangeOrder(1).build(),
        LicencePositionChangeTestUtil.newBuilder().withChangeOrder(3).build()));

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    verify(licencePositionChangeService).createLicencePositionChange(
        eq(earliestPosition), anyList(), eq(4), eq(LicencePositionChangeStatus.CONSENTED));
  }

  @Test
  void createAndLinkFeatures_assertEachBlockAndItsSubareaAreOneRectangleSpacedApartFromTheOtherBlocks() {
    givenPositions(LicencePositionTestUtil.newBuilder().build());
    givenFeaturesCanBePersisted();

    licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    var secondBlockEdges = firstRowEdges("3.9", "4.1");
    var thirdBlockEdges = firstRowEdges("4.2", "4.4");
    verifyShapesSaved(List.of(
        BLOCK_EDGES,
        BLOCK_EDGES,
        secondBlockEdges,
        secondBlockEdges,
        thirdBlockEdges,
        thirdBlockEdges
    ));
    verify(featureService, times(6)).saveFeature(featureCaptor.capture());
    assertThat(polygonCaptor.getAllValues())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyElementsOf(featureCaptor.getAllValues().stream()
            .map(feature -> expectedPolygon(feature, null, null))
            .toList());
  }

  @Test
  void createAndLinkFeatures_whenBlocksAreAlreadySeeded_thenNewBlocksTakeThePlacesAfterTheHighestSeededBlock() {
    givenPositions(LicencePositionTestUtil.newBuilder().build());
    givenFeaturesCanBePersisted();
    when(featureService.findAllByAttribute("QUADRANT_NO", "999")).thenReturn(List.of(
        FeatureTestUtil.builder().withAttributes(blockAttributes(9)).build(),
        FeatureTestUtil.builder().withAttributes(blockAttributes(4)).build(),
        FeatureTestUtil.builder().withAttributes(Map.of("LAYER", "SUBAREAS", "QUADRANT_NO", "999")).build()
    ));

    var seededFeatures = licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE);

    assertThat(seededFeatures.blocks())
        .extracting(Feature::getAttributes)
        .containsExactly(blockAttributes(10), blockAttributes(11), blockAttributes(12));
    verifyShapesSaved(List.of(
        firstRowEdges("6.3", "6.5"),
        firstRowEdges("6.3", "6.5"),
        secondRowEdges("3.6", "3.8"),
        secondRowEdges("3.6", "3.8"),
        secondRowEdges("3.9", "4.1"),
        secondRowEdges("3.9", "4.1")
    ));
  }

  @Test
  void createAndLinkFeatures_whenLicenceHasNoPositions_assertNothingIsCreated() {
    when(licencePositionService.getExecutedChronologicalLicencePositions(LICENCE)).thenReturn(List.of());

    assertThat(licencePositionFeatureTestHarnessService.createAndLinkFeatures(LICENCE))
        .isEqualTo(new SeededFeatures(List.of(), Map.of()));

    verifyNoInteractions(featureService, polygonService, lineService);
  }

  @Test
  void clearFeatures() {
    var seededFeatures = List.of(BLOCK, wholeBlockSubarea());
    when(featureService.findAllByAttribute("TEST_HARNESS_LICENCE_ID", "1")).thenReturn(seededFeatures);

    licencePositionFeatureTestHarnessService.clearFeatures(LICENCE);

    var inOrder = inOrder(commandJourneyService, featureService);
    inOrder.verify(commandJourneyService).deleteCommandJourneysHoldingFeatures(seededFeatures);
    inOrder.verify(featureService).deleteAll(seededFeatures);
  }

  @Test
  void splitBlockInHalf_assertTheBlockBecomesTwoHalvesKeepingItsAttributes() {
    givenFeaturesCanBePersisted();

    var splitBlock = licencePositionFeatureTestHarnessService.splitBlockInHalf(COMMAND_JOURNEY, BLOCK);

    assertThat(List.of(splitBlock.surrenderedHalf(), splitBlock.retainedHalf()))
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(
            expectedFeature("999/1a", blockAttributes(1), HALF_FEATURE_AREA, null),
            expectedFeature("999/1b", blockAttributes(1), HALF_FEATURE_AREA, null)
        );
  }

  @Test
  void splitBlockInHalf_assertEachHalfIsASquareOfTheBlockCutDownTheMiddle() {
    givenFeaturesCanBePersisted();

    licencePositionFeatureTestHarnessService.splitBlockInHalf(COMMAND_JOURNEY, BLOCK);

    verifyShapesSaved(List.of(WESTERN_HALF_EDGES, EASTERN_HALF_EDGES));
  }

  @Test
  void splitBlockInHalf_whenALaterSeededBlock_thenTheHalvesAreCutFromThatBlock() {
    givenFeaturesCanBePersisted();
    var block = FeatureTestUtil.builder()
        .withAttributes(blockAttributes(12))
        .withFeatureArea(FEATURE_AREA)
        .build();

    licencePositionFeatureTestHarnessService.splitBlockInHalf(COMMAND_JOURNEY, block);

    verifyShapesSaved(List.of(secondRowEdges("3.9", "4"), secondRowEdges("4", "4.1")));
  }

  @Test
  void splitBlockInHalf_assertTheHalvesReplaceTheBlockAsTheJourneysActiveFeatures() {
    var splitCommand = new OperatorCommand();
    when(operatorCommandService.createOperatorCommand(COMMAND_JOURNEY, TransformationType.SPLIT))
        .thenReturn(splitCommand);

    var splitBlock = licencePositionFeatureTestHarnessService.splitBlockInHalf(COMMAND_JOURNEY, BLOCK);

    verify(featureJourneyStateService).deactivateFeatures(COMMAND_JOURNEY, List.of(BLOCK));
    verify(featureJourneyStateService).createFeatureJourneyStatesForCommandOutput(
        COMMAND_JOURNEY,
        splitCommand,
        Set.of(BLOCK.getId()),
        List.of(splitBlock.surrenderedHalf(), splitBlock.retainedHalf())
    );
  }

  @Test
  void cropSubareaAcrossTheMiddle() {
    givenFeaturesCanBePersisted();
    var subarea = subarea("B", HALF_FEATURE_AREA);

    var croppedSubarea = licencePositionFeatureTestHarnessService.cropSubareaAcrossTheMiddle(subarea, BLOCK, SURRENDER_DATE);

    assertThat(croppedSubarea)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(expectedFeature("B", subareaAttributes("B"), QUARTER_FEATURE_AREA, SURRENDER_DATE));
    verifyShapesSaved(List.of(firstRowEdges("3.7", "3.75")));
    verifySubareaEnded(subarea, DAY_BEFORE_SURRENDER);
  }

  @Test
  void relinquishSubarea() {
    var subarea = wholeBlockSubarea();

    licencePositionFeatureTestHarnessService.relinquishSubarea(subarea, SURRENDER_DATE);

    verifySubareaEnded(subarea, DAY_BEFORE_SURRENDER);
  }

  @Test
  void mergeSubareasOnRetainedHalf() {
    givenFeaturesCanBePersisted();
    var croppedSubarea = subarea("B", QUARTER_FEATURE_AREA);
    var keptSubarea = subarea("C", QUARTER_FEATURE_AREA);

    var mergedSubarea = licencePositionFeatureTestHarnessService.mergeSubareasOnRetainedHalf(
        List.of(croppedSubarea, keptSubarea), BLOCK, SUBAREA_CHANGE_DATE);

    assertThat(mergedSubarea)
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(expectedFeature(
            "BC",
            subareaAttributes("BC"),
            HALF_FEATURE_AREA,
            SUBAREA_CHANGE_DATE
        ));
    verifyShapesSaved(List.of(EASTERN_HALF_EDGES));
    verifySubareaEnded(croppedSubarea, DAY_BEFORE_SUBAREA_CHANGE);
    verifySubareaEnded(keptSubarea, DAY_BEFORE_SUBAREA_CHANGE);
  }

  @Test
  void strataSplitSubarea() {
    givenFeaturesCanBePersisted();
    var subarea = wholeBlockSubarea();

    var strata = licencePositionFeatureTestHarnessService.strataSplitSubarea(subarea, BLOCK, -3000, SUBAREA_CHANGE_DATE);

    assertThat(strata)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(
            expectedFeature("ABOVE", subareaAttributes("ABOVE"), FEATURE_AREA, SUBAREA_CHANGE_DATE),
            expectedFeature("BELOW", subareaAttributes("BELOW"), FEATURE_AREA, SUBAREA_CHANGE_DATE)
        );
    verifyShapesSaved(List.of(BLOCK_EDGES, BLOCK_EDGES));
    assertThat(polygonCaptor.getAllValues())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            expectedPolygon(strata.getFirst(), null, -3000L),
            expectedPolygon(strata.getLast(), -3000L, null)
        );
    verifySubareaEnded(subarea, DAY_BEFORE_SUBAREA_CHANGE);
  }

  @Test
  void splitSubareaInHalf() {
    givenFeaturesCanBePersisted();
    var subarea = wholeBlockSubarea();

    var subareaHalves = licencePositionFeatureTestHarnessService.splitSubareaInHalf(subarea, BLOCK, SUBAREA_CHANGE_DATE);

    assertThat(subareaHalves)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(
            expectedFeature("A", subareaAttributes("A"), HALF_FEATURE_AREA, SUBAREA_CHANGE_DATE),
            expectedFeature("B", subareaAttributes("B"), HALF_FEATURE_AREA, SUBAREA_CHANGE_DATE)
        );
    verifyShapesSaved(List.of(WESTERN_HALF_EDGES, EASTERN_HALF_EDGES));
    verifySubareaEnded(subarea, DAY_BEFORE_SUBAREA_CHANGE);
  }

  private void givenFeaturesCanBePersisted() {
    doAnswer(invocation -> {
      invocation.getArgument(0, Feature.class).setId(UUID.randomUUID());
      return null;
    }).when(featureService).saveFeature(any(Feature.class));
  }

  private void givenPositions(LicencePosition... licencePositions) {
    givenPositions(LICENCE, licencePositions);
  }

  private void givenPositions(Licence licence, LicencePosition... licencePositions) {
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence))
        .thenReturn(List.of(licencePositions));
  }

  private void verifyShapesSaved(List<List<String>> edgesOfEachShape) {
    verify(polygonService, times(edgesOfEachShape.size())).savePolygon(polygonCaptor.capture());
    verify(lineService, times(edgesOfEachShape.size())).saveLines(linesCaptor.capture());
    var polygons = polygonCaptor.getAllValues();
    var expectedLinesOfEachShape = IntStream.range(0, edgesOfEachShape.size())
        .mapToObj(shape -> expectedLines(polygons.get(shape), edgesOfEachShape.get(shape)))
        .toList();
    assertThat(linesCaptor.getAllValues())
        .usingRecursiveComparison()
        .isEqualTo(expectedLinesOfEachShape);
  }

  private void verifySubareaEnded(
      Feature subarea,
      LocalDate endDate
  ) {
    assertThat(subarea)
        .extracting(Feature::getStartDate, Feature::getEndDate)
        .containsExactly(null, endDate);
    verify(featureService).saveFeature(subarea);
  }

  private static CommandJourney commandJourney() {
    var commandJourney = new CommandJourney();
    commandJourney.setId(COMMAND_JOURNEY_ID);
    return commandJourney;
  }

  private static Map<String, String> blockAttributes(int blockNumber) {
    return blockAttributes(blockNumber, LICENCE);
  }

  private static Map<String, String> blockAttributes(
      int blockNumber,
      Licence harnessLicence
  ) {
    return Map.of(
        "LAYER", "BLOCKS",
        "QUADRANT_NO", "999",
        "BLOCK_NO", String.valueOf(blockNumber),
        "TEST_HARNESS_LICENCE_ID", String.valueOf(harnessLicence.getId())
    );
  }

  private static Map<String, String> subareaAttributes(String subareaName) {
    return subareaAttributes(subareaName, LICENCE);
  }

  private static Map<String, String> subareaAttributes(
      String subareaName,
      Licence harnessLicence
  ) {
    return Map.of(
        "LAYER", "SUBAREAS",
        "NAME", subareaName,
        "TEST_HARNESS_LICENCE_ID", String.valueOf(harnessLicence.getId())
    );
  }

  private static Feature wholeBlockSubarea() {
    return subarea("ALL", FEATURE_AREA);
  }

  private static Feature subarea(
      String subareaName,
      BigDecimal featureArea
  ) {
    return FeatureTestUtil.builder()
        .withFeatureName(subareaName)
        .withAttributes(subareaAttributes(subareaName))
        .withFeatureArea(featureArea)
        .build();
  }

  private static Feature expectedFeature(
      String featureName,
      Map<String, String> attributes
  ) {
    return expectedFeature(featureName, attributes, FEATURE_AREA, null);
  }

  private static Feature expectedFeature(
      String featureName,
      Map<String, String> attributes,
      BigDecimal featureArea,
      LocalDate startDate
  ) {
    var feature = new Feature();
    feature.setFeatureName(featureName);
    feature.setCoordinateSystem(CoordinateSystem.ED50);
    feature.setFeatureArea(featureArea);
    feature.setAttributes(attributes);
    feature.setStartDate(startDate);
    return feature;
  }

  private static Polygon expectedPolygon(
      Feature feature,
      Long startDepth,
      Long endDepth
  ) {
    var polygon = new Polygon();
    polygon.setFeature(feature);
    polygon.setAttributes(Map.of());
    polygon.setStartDepth(startDepth);
    polygon.setEndDepth(endDepth);
    return polygon;
  }

  private static List<String> firstRowEdges(
      String westernLongitude,
      String easternLongitude
  ) {
    return rectangleEdges(westernLongitude, easternLongitude, FIRST_ROW_SOUTHERN_LATITUDE, FIRST_ROW_NORTHERN_LATITUDE);
  }

  private static List<String> secondRowEdges(
      String westernLongitude,
      String easternLongitude
  ) {
    return rectangleEdges(westernLongitude, easternLongitude, SECOND_ROW_SOUTHERN_LATITUDE, SECOND_ROW_NORTHERN_LATITUDE);
  }

  // clockwise from the southern edge, as the harness builds them
  private static List<String> rectangleEdges(
      String westernLongitude,
      String easternLongitude,
      String southernLatitude,
      String northernLatitude
  ) {
    var template = """
        {"spatialReference":{"wkid":4230},"paths":[[[%s,%s],[%s,%s]]]}""";
    return List.of(
        template.formatted(westernLongitude, southernLatitude, easternLongitude, southernLatitude),
        template.formatted(easternLongitude, southernLatitude, easternLongitude, northernLatitude),
        template.formatted(easternLongitude, northernLatitude, westernLongitude, northernLatitude),
        template.formatted(westernLongitude, northernLatitude, westernLongitude, southernLatitude)
    );
  }

  private static List<Line> expectedLines(Polygon polygon, List<String> edges) {
    return IntStream.rangeClosed(1, edges.size())
        .mapToObj(displayOrder -> expectedLine(polygon, edges.get(displayOrder - 1), displayOrder))
        .toList();
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
