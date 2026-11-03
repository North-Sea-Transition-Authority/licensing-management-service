package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.Line;
import uk.co.fivium.gisframework.feature.Polygon;
import uk.co.fivium.gisframework.relationship.BoundingBox;
import uk.co.fivium.gisframework.relationship.FeatureRelationshipService;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderTypeFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.definearea.PartialSurrenderSelectAreasFormValidator;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

@ExtendWith(MockitoExtension.class)
class PartialSurrenderValidatorTest {

  private static final LocalDate POSITION_DATE = LocalDate.of(2026, Month.JUNE, 5);
  private static final UUID COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final Feature BLOCK = FeatureTestUtil.builder().withFeatureName("30/1").build();
  private static final Feature SURRENDERED_AREA = FeatureTestUtil.builder().build();
  private static final Feature RETAINED_AREA = FeatureTestUtil.builder().build();
  private static final LicencePositionState STATE = LicencePositionState.EMPTY.withBlock(BLOCK.getId(), List.of());
  private static final BoundingBox BOUNDING_BOX = new BoundingBox(0, 0, 1, 1);

  @Mock
  private CommandJourneyService commandJourneyService;

  @Mock
  private FeatureService featureService;

  @Mock
  private FeatureRelationshipService featureRelationshipService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private PartialSurrenderSelectAreasFormValidator partialSurrenderSelectAreasFormValidator;

  private PartialSurrenderValidator partialSurrenderValidator;

  @BeforeEach
  void setUp() {
    partialSurrenderValidator = new PartialSurrenderValidator(
        new PartialSurrenderDetailsFormValidator(),
        new BlockSurrenderTypeFormValidator(),
        partialSurrenderSelectAreasFormValidator,
        commandJourneyService,
        featureService,
        featureRelationshipService,
        licencePositionChangeService
    );
  }

  @Test
  void validateForms_whenSurrenderedBlockNotOnPosition_thenOnlyBlockSelectionError() {
    var otherBlock = FeatureTestUtil.builder().build();
    var state = LicencePositionState.EMPTY.withBlock(otherBlock.getId(), List.of());
    when(featureService.getFeaturesByIds(Set.of(otherBlock.getId()))).thenReturn(List.of(otherBlock));

    var errors = partialSurrenderValidator.validateForms(partialSurrenderOfBlock(), state);

    assertThat(errors).containsExactly("Select the licence blocks being surrendered");
    verifyNoInteractions(commandJourneyService, featureRelationshipService);
  }

  @Test
  void validateForms_whenBlockHasNoSurrenderType_thenTypeErrorForBlock() {
    givenBlockOnPosition();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK.getId()))
        .build();

    var errors = partialSurrenderValidator.validateForms(surrender, STATE);

    assertThat(errors).containsExactly("Block 30/1: Select the type of surrender for this block");
  }

  @Test
  void validateForms_whenEveryBlockFullySurrendered_thenNoPartialSurrenderError() {
    givenBlockOnPosition();
    var surrender = surrenderOfBlock(new SurrenderDetails(BlockSurrenderType.FULL_SURRENDER, null, List.of()));

    var errors = partialSurrenderValidator.validateForms(surrender, STATE);

    assertThat(errors).containsExactly("Select partial surrender for at least one block");
  }

  @Test
  void validateForms_whenSelectedAreasCoverWholeBlock_thenAreaError() {
    givenBlockOnPosition();
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID)).thenReturn(List.of(SURRENDERED_AREA, RETAINED_AREA));
    when(featureRelationshipService.is2dTopologicallyEqual(List.of(SURRENDERED_AREA), BLOCK)).thenReturn(true);

    var errors = partialSurrenderValidator.validateForms(partialSurrenderOfBlock(), STATE);

    assertThat(errors).containsExactly("Block 30/1: Select areas that do not cover the whole block");
  }

  @Test
  void validateForms_whenSurrenderTypeAndAreaSelectionErrors_thenOnlySurrenderTypeErrors() {
    var blockWithoutSurrenderType = blockActiveBetween("30/4", CoordinateSystem.ED50, POSITION_DATE, null);
    var state = STATE.withBlock(blockWithoutSurrenderType.getId(), List.of());
    when(featureService.getFeaturesByIds(Set.of(BLOCK.getId(), blockWithoutSurrenderType.getId())))
        .thenReturn(List.of(BLOCK, blockWithoutSurrenderType));
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK.getId(), blockWithoutSurrenderType.getId()))
        .withSurrenderDetails(Map.of(
            BLOCK.getId(),
            new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, COMMAND_JOURNEY_ID, List.of())
        ))
        .build();

    var errors = partialSurrenderValidator.validateForms(surrender, state);

    assertThat(errors).containsExactly("Block 30/4: Select the type of surrender for this block");
    verifyNoInteractions(commandJourneyService, featureRelationshipService);
  }

  @Test
  void validateForms_whenFormsValid_thenNoErrors() {
    givenBlockOnPosition();
    givenActiveAreas();
    when(featureRelationshipService.is2dTopologicallyEqual(List.of(SURRENDERED_AREA), BLOCK)).thenReturn(false);

    var errors = partialSurrenderValidator.validateForms(partialSurrenderOfBlock(), STATE);

    assertThat(errors).isEmpty();
  }

  @Test
  void validateSpatially_whenNoAreasKept_thenNoOverlapCheck() {
    var surrender = surrenderOfBlock(new SurrenderDetails(BlockSurrenderType.FULL_SURRENDER, null, List.of()));

    var errors = partialSurrenderValidator.validateSpatially(surrender, STATE, POSITION_DATE, null, Map.of());

    assertThat(errors).isEmpty();
    verifyNoInteractions(featureService, featureRelationshipService);
  }

  @Test
  void validateSpatially_whenRetainedAreaOverlapsActiveBlock_thenOverlapErrorAndBlocksInOtherCoordinateSystemsNotChecked() {
    givenBlockOnPosition();
    givenActiveAreas();
    var overlappedBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var blockInAnotherCoordinateSystem =
        blockActiveBetween("30/3", CoordinateSystem.WGS84, POSITION_DATE.minusYears(1), null);
    var surrenderedBlock = FeatureTestUtil.builder().withId(BLOCK.getId()).build();
    surrenderedBlock.setStartDate(POSITION_DATE.minusYears(1));
    var blockLines = givenBlocks(List.of(overlappedBlock, blockInAnotherCoordinateSystem, surrenderedBlock));
    when(featureRelationshipService.get2dCrossedFeatures(List.of(RETAINED_AREA), linesOf(blockLines, overlappedBlock)))
        .thenReturn(List.of(overlappedBlock, overlappedBlock));

    var errors = partialSurrenderValidator.validateSpatially(partialSurrenderOfBlock(), STATE, POSITION_DATE, null, blockLines);

    assertThat(errors).containsExactly(
        "Block 30/1: Block after surrender must not overlap licence block 30/2"
    );
  }

  @Test
  void validateForms_whenTheAreaKeptHasBeenSplitSinceItWasSelected_thenAreasMustBeSelectedAgain() {
    givenBlockOnPosition();
    var firstPartOfRetainedArea = FeatureTestUtil.builder().build();
    var secondPartOfRetainedArea = FeatureTestUtil.builder().build();
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID))
        .thenReturn(List.of(SURRENDERED_AREA, firstPartOfRetainedArea, secondPartOfRetainedArea));
    when(featureRelationshipService.is2dTopologicallyEqual(List.of(SURRENDERED_AREA), BLOCK)).thenReturn(false);

    var errors = partialSurrenderValidator.validateForms(partialSurrenderOfBlock(), STATE);

    assertThat(errors).containsExactly(
        "Block 30/1: Select the areas being surrendered again as the block's retained area has changed"
    );
  }

  @Test
  void validateSpatially_whenTheAreaKeptByAnAppliedSurrenderOverlapsABlockActiveOnThePositionDate_thenOverlapError() {
    givenBlockOnPosition();
    var areaKept = FeatureTestUtil.builder().build();
    var appliedSurrender = surrenderOfBlock(new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(UUID.randomUUID()),
        List.of(areaKept.getId())
    ));
    var overlappedBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var blockLines = givenBlocks(List.of(overlappedBlock));
    when(featureService.getFeaturesByIds(List.of(areaKept.getId()))).thenReturn(List.of(areaKept));
    when(featureRelationshipService.get2dCrossedFeatures(List.of(areaKept), blockLines))
        .thenReturn(List.of(overlappedBlock));

    var errors = partialSurrenderValidator.validateSpatially(appliedSurrender, STATE, POSITION_DATE, null, blockLines);

    assertThat(errors).containsExactly("Block 30/1: Block after surrender must not overlap licence block 30/2");
  }

  @Test
  void validateSpatially_whenCorrectingALiveSurrender_thenThePartsItCreatedAreNotCheckedForOverlap() {
    givenBlockOnPosition();
    givenActiveAreas();
    var liveRetainedArea = blockActiveBetween("30/1", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var liveSurrender = surrenderOfBlock(new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(UUID.randomUUID()),
        List.of(liveRetainedArea.getId())
    ));
    var liveChange = LicencePositionChangeTestUtil.newBuilder().withOperations(List.of(liveSurrender)).build();
    when(licencePositionChangeService.findById(liveChange.getId())).thenReturn(Optional.of(liveChange));
    var blockLines = givenBlocks(List.of(liveRetainedArea));

    var errors = partialSurrenderValidator.validateSpatially(
        partialSurrenderOfBlock(),
        STATE,
        POSITION_DATE,
        liveChange.getId().toString(),
        blockLines
    );

    assertThat(errors).isEmpty();
    verify(featureRelationshipService).get2dCrossedFeatures(List.of(RETAINED_AREA), Map.of());
  }

  @Test
  void validateSpatially_whenRetainedAreasOverlapSeveralActiveBlocks_thenOneOverlapErrorPerOverlappedBlock() {
    givenBlockOnPosition();
    var otherRetainedArea = FeatureTestUtil.builder().build();
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID))
        .thenReturn(List.of(SURRENDERED_AREA, RETAINED_AREA, otherRetainedArea));
    var firstOverlappedBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var secondOverlappedBlock = blockActiveBetween("30/3", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var blockLines = givenBlocks(List.of(firstOverlappedBlock, secondOverlappedBlock));
    when(featureRelationshipService.get2dCrossedFeatures(List.of(RETAINED_AREA, otherRetainedArea), blockLines))
        .thenReturn(List.of(firstOverlappedBlock, secondOverlappedBlock));
    var surrender = surrenderOfBlock(new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        COMMAND_JOURNEY_ID,
        List.of(SURRENDERED_AREA.getId()),
        List.of(RETAINED_AREA.getId(), otherRetainedArea.getId())
    ));

    var errors = partialSurrenderValidator.validateSpatially(surrender, STATE, POSITION_DATE, null, blockLines);

    assertThat(errors).containsExactly(
        "Block 30/1: Block after surrender must not overlap licence block 30/2",
        "Block 30/1: Block after surrender must not overlap licence block 30/3"
    );
  }

  @Test
  void validateSpatially_whenSeveralBlocksPartiallySurrendered_thenEachCheckedForOverlapWithTheSameBlocksNotInSurrender() {
    var secondBlock = blockActiveBetween("30/4", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var secondCommandJourneyId = UUID.randomUUID();
    var secondSurrenderedArea = FeatureTestUtil.builder().build();
    var secondRetainedArea = FeatureTestUtil.builder().build();
    var state = STATE.withBlock(secondBlock.getId(), List.of());
    when(featureService.getFeaturesByIds(Set.of(BLOCK.getId(), secondBlock.getId())))
        .thenReturn(List.of(BLOCK, secondBlock));
    givenActiveAreas();
    when(commandJourneyService.getActiveFeatures(secondCommandJourneyId))
        .thenReturn(List.of(secondSurrenderedArea, secondRetainedArea));
    var overlappedBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var blockLines = givenBlocks(List.of(secondBlock, overlappedBlock));
    var otherActiveBlockLines = linesOf(blockLines, overlappedBlock);
    when(featureRelationshipService.get2dCrossedFeatures(List.of(RETAINED_AREA), otherActiveBlockLines))
        .thenReturn(List.of(overlappedBlock));
    when(featureRelationshipService.get2dCrossedFeatures(List.of(secondRetainedArea), otherActiveBlockLines))
        .thenReturn(List.of(overlappedBlock));
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK.getId(), secondBlock.getId()))
        .withSurrenderDetails(Map.of(
            BLOCK.getId(),
            new SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                COMMAND_JOURNEY_ID,
                List.of(SURRENDERED_AREA.getId()),
                List.of(RETAINED_AREA.getId())
            ),
            secondBlock.getId(),
            new SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                secondCommandJourneyId,
                List.of(secondSurrenderedArea.getId()),
                List.of(secondRetainedArea.getId())
            )
        ))
        .build();

    var errors = partialSurrenderValidator.validateSpatially(surrender, state, POSITION_DATE, null, blockLines);

    assertThat(errors).containsExactlyInAnyOrder(
        "Block 30/1: Block after surrender must not overlap licence block 30/2",
        "Block 30/4: Block after surrender must not overlap licence block 30/2"
    );
    verify(featureService).findAllByAttribute("LAYER", "BLOCKS");
  }

  @ParameterizedTest
  @MethodSource("datesActiveOnPositionDate")
  void validateSpatially_whenBlockActiveOnPositionDate_thenCheckedForOverlap(LocalDate startDate, LocalDate endDate) {
    givenBlockOnPosition();
    givenActiveAreas();
    var otherBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, startDate, endDate);
    var blockLines = givenBlocks(List.of(otherBlock));
    when(featureRelationshipService.get2dCrossedFeatures(List.of(RETAINED_AREA), blockLines))
        .thenReturn(List.of());

    var errors = partialSurrenderValidator.validateSpatially(partialSurrenderOfBlock(), STATE, POSITION_DATE, null, blockLines);

    assertThat(errors).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("datesNotActiveOnPositionDate")
  void validateSpatially_whenBlockNotActiveOnPositionDate_thenNotCheckedForOverlap(LocalDate startDate, LocalDate endDate) {
    givenBlockOnPosition();
    givenActiveAreas();
    var otherBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, startDate, endDate);
    var blockLines = givenBlocks(List.of(otherBlock));
    when(featureRelationshipService.get2dCrossedFeatures(List.of(RETAINED_AREA), Map.of()))
        .thenReturn(List.of());

    var errors = partialSurrenderValidator.validateSpatially(partialSurrenderOfBlock(), STATE, POSITION_DATE, null, blockLines);

    assertThat(errors).isEmpty();
  }

  @Test
  void getBlockLineBoundingBoxes_whenBlocksActiveOnDifferentDates_thenOnlyLinesOfBlocksActiveOnAPositionDateLoaded() {
    var laterPositionDate = POSITION_DATE.plusYears(2);
    var blockActiveOnPositionDate =
        blockActiveBetween("30/2", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), POSITION_DATE);
    var blockActiveOnNeitherDate =
        blockActiveBetween("30/3", CoordinateSystem.ED50, POSITION_DATE.plusDays(1), laterPositionDate.minusDays(1));
    var blockActiveOnLaterPositionDate = blockActiveBetween("30/4", CoordinateSystem.ED50, laterPositionDate, null);
    when(featureService.findAllByAttribute("LAYER", "BLOCKS"))
        .thenReturn(List.of(blockActiveOnPositionDate, blockActiveOnNeitherDate, blockActiveOnLaterPositionDate));
    var blockLines = Map.of(
        lineOf(blockActiveOnPositionDate), BOUNDING_BOX,
        lineOf(blockActiveOnLaterPositionDate), BOUNDING_BOX
    );
    when(featureRelationshipService.getLineBoundingBoxes(List.of(blockActiveOnPositionDate, blockActiveOnLaterPositionDate)))
        .thenReturn(blockLines);

    var result = partialSurrenderValidator.getBlockLineBoundingBoxes(Set.of(POSITION_DATE, laterPositionDate));

    assertThat(result).isEqualTo(blockLines);
  }

  private static Stream<Arguments> datesActiveOnPositionDate() {
    return Stream.of(
        Arguments.of(POSITION_DATE, null),
        Arguments.of(POSITION_DATE.minusYears(1), POSITION_DATE),
        Arguments.of(POSITION_DATE.minusYears(1), POSITION_DATE.plusYears(1))
    );
  }

  private static Stream<Arguments> datesNotActiveOnPositionDate() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(POSITION_DATE.plusDays(1), null),
        Arguments.of(POSITION_DATE.minusYears(1), POSITION_DATE.minusDays(1))
    );
  }

  private void givenBlockOnPosition() {
    when(featureService.getFeaturesByIds(Set.of(BLOCK.getId()))).thenReturn(List.of(BLOCK));
  }

  private Map<Line, BoundingBox> givenBlocks(List<Feature> blocks) {
    when(featureService.findAllByAttribute("LAYER", "BLOCKS")).thenReturn(blocks);
    return blocks.stream().collect(Collectors.toMap(PartialSurrenderValidatorTest::lineOf, block -> BOUNDING_BOX));
  }

  private static Map<Line, BoundingBox> linesOf(
      Map<Line, BoundingBox> blockLines,
      Feature block
  ) {
    return blockLines.entrySet().stream()
        .filter(entry -> entry.getKey().getPolygon().getFeature().equals(block))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Line lineOf(Feature block) {
    var polygon = new Polygon();
    polygon.setFeature(block);
    var line = new Line();
    line.setPolygon(polygon);
    return line;
  }

  private void givenActiveAreas() {
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID)).thenReturn(List.of(SURRENDERED_AREA, RETAINED_AREA));
  }

  private static PartialSurrenderOperation partialSurrenderOfBlock() {
    return surrenderOfBlock(new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        COMMAND_JOURNEY_ID,
        List.of(SURRENDERED_AREA.getId()),
        List.of(RETAINED_AREA.getId())
    ));
  }

  private static PartialSurrenderOperation surrenderOfBlock(SurrenderDetails surrenderDetails) {
    return LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK.getId()))
        .withSurrenderDetails(Map.of(BLOCK.getId(), surrenderDetails))
        .build();
  }

  private static Feature blockActiveBetween(
      String name,
      CoordinateSystem coordinateSystem,
      LocalDate startDate,
      LocalDate endDate
  ) {
    var block = FeatureTestUtil.builder().withFeatureName(name).withCoordinateSystem(coordinateSystem).build();
    block.setStartDate(startDate);
    block.setEndDate(endDate);
    return block;
  }
}
