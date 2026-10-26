package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
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
        licencePositionChangeService
    );
  }

  @Test
  void validate_whenSurrenderedBlockNotOnPosition_thenOnlyBlockSelectionError() {
    var otherBlock = FeatureTestUtil.builder().build();
    var state = LicencePositionState.EMPTY.withBlock(otherBlock.getId(), List.of());
    when(featureService.getFeaturesByIds(Set.of(otherBlock.getId()))).thenReturn(List.of(otherBlock));

    var errors = partialSurrenderValidator.validate(partialSurrenderOfBlock(), state, POSITION_DATE, null);

    assertThat(errors).containsExactly("Select the licence blocks being surrendered");
    verifyNoInteractions(commandJourneyService, featureRelationshipService);
  }

  @Test
  void validate_whenBlockHasNoSurrenderType_thenTypeErrorForBlock() {
    givenBlockOnPosition();
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK.getId()))
        .build();

    var errors = partialSurrenderValidator.validate(surrender, STATE, POSITION_DATE, null);

    assertThat(errors).containsExactly("Block 30/1: Select the type of surrender for this block");
  }

  @Test
  void validate_whenEveryBlockFullySurrendered_thenNoPartialSurrenderError() {
    givenBlockOnPosition();
    var surrender = surrenderOfBlock(new SurrenderDetails(BlockSurrenderType.FULL_SURRENDER, null, List.of()));

    var errors = partialSurrenderValidator.validate(surrender, STATE, POSITION_DATE, null);

    assertThat(errors).containsExactly("Select partial surrender for at least one block");
  }

  @Test
  void validate_whenSelectedAreasCoverWholeBlock_thenAreaErrorAndNoOverlapCheck() {
    givenBlockOnPosition();
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID)).thenReturn(List.of(SURRENDERED_AREA, RETAINED_AREA));
    when(featureRelationshipService.isTopologicallyEqual(List.of(SURRENDERED_AREA), BLOCK)).thenReturn(true);

    var errors = partialSurrenderValidator.validate(partialSurrenderOfBlock(), STATE, POSITION_DATE, null);

    assertThat(errors).containsExactly("Block 30/1: Select areas that do not cover the whole block");
    verify(featureRelationshipService, never()).get2dOverlappingFeatures(List.of(RETAINED_AREA), List.of());
  }

  @Test
  void validate_whenRetainedAreaOverlapsActiveBlock_thenOverlapError() {
    givenBlockOnPosition();
    givenValidAreaSelection();
    var overlappedBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var blockInAnotherCoordinateSystem =
        blockActiveBetween("30/3", CoordinateSystem.WGS84, POSITION_DATE.minusYears(1), null);
    var surrenderedBlock = FeatureTestUtil.builder().withId(BLOCK.getId()).build();
    surrenderedBlock.setStartDate(POSITION_DATE.minusYears(1));
    when(featureService.findAllByAttribute("LAYER", "BLOCKS"))
        .thenReturn(List.of(overlappedBlock, blockInAnotherCoordinateSystem, surrenderedBlock));
    when(featureRelationshipService.get2dOverlappingFeatures(List.of(RETAINED_AREA), List.of(overlappedBlock)))
        .thenReturn(List.of(overlappedBlock, overlappedBlock));

    var errors = partialSurrenderValidator.validate(partialSurrenderOfBlock(), STATE, POSITION_DATE, null);

    assertThat(errors).containsExactly(
        "Block 30/1: Block after surrender must not overlap licence block 30/2"
    );
  }

  @Test
  void validate_whenTheAreaKeptHasBeenSplitSinceItWasSelected_thenAreasMustBeSelectedAgain() {
    givenBlockOnPosition();
    var firstPartOfRetainedArea = FeatureTestUtil.builder().build();
    var secondPartOfRetainedArea = FeatureTestUtil.builder().build();
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID))
        .thenReturn(List.of(SURRENDERED_AREA, firstPartOfRetainedArea, secondPartOfRetainedArea));
    when(featureRelationshipService.isTopologicallyEqual(List.of(SURRENDERED_AREA), BLOCK)).thenReturn(false);

    var errors = partialSurrenderValidator.validate(partialSurrenderOfBlock(), STATE, POSITION_DATE, null);

    assertThat(errors).containsExactly(
        "Block 30/1: Select the areas being surrendered again as the block's areas have changed"
    );
  }

  @Test
  void validate_whenTheAreaKeptByAnAppliedSurrenderOverlapsABlockActiveOnThePositionDate_thenOverlapError() {
    givenBlockOnPosition();
    var areaKept = FeatureTestUtil.builder().build();
    var appliedSurrender = surrenderOfBlock(new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(UUID.randomUUID()),
        List.of(areaKept.getId())
    ));
    var overlappedBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    when(featureService.findAllByAttribute("LAYER", "BLOCKS")).thenReturn(List.of(overlappedBlock));
    when(featureService.getFeaturesByIds(List.of(areaKept.getId()))).thenReturn(List.of(areaKept));
    when(featureRelationshipService.get2dOverlappingFeatures(List.of(areaKept), List.of(overlappedBlock)))
        .thenReturn(List.of(overlappedBlock));

    var errors = partialSurrenderValidator.validate(appliedSurrender, STATE, POSITION_DATE, null);

    assertThat(errors).containsExactly("Block 30/1: Block after surrender must not overlap licence block 30/2");
  }

  @Test
  void validate_whenCorrectingALiveSurrender_thenThePartsItCreatedAreNotCheckedForOverlap() {
    givenBlockOnPosition();
    givenValidAreaSelection();
    var liveRetainedArea = blockActiveBetween("30/1", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var liveSurrender = surrenderOfBlock(new SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(UUID.randomUUID()),
        List.of(liveRetainedArea.getId())
    ));
    var liveChange = LicencePositionChangeTestUtil.newBuilder().withOperations(List.of(liveSurrender)).build();
    when(licencePositionChangeService.findById(liveChange.getId())).thenReturn(Optional.of(liveChange));
    when(featureService.findAllByAttribute("LAYER", "BLOCKS")).thenReturn(List.of(liveRetainedArea));

    var errors = partialSurrenderValidator.validate(
        partialSurrenderOfBlock(),
        STATE,
        POSITION_DATE,
        liveChange.getId().toString()
    );

    assertThat(errors).isEmpty();
    verify(featureRelationshipService).get2dOverlappingFeatures(List.of(RETAINED_AREA), List.of());
  }

  @Test
  void validate_whenRetainedAreasOverlapSeveralActiveBlocks_thenOneOverlapErrorPerOverlappedBlock() {
    givenBlockOnPosition();
    var otherRetainedArea = FeatureTestUtil.builder().build();
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID))
        .thenReturn(List.of(SURRENDERED_AREA, RETAINED_AREA, otherRetainedArea));
    when(featureRelationshipService.isTopologicallyEqual(List.of(SURRENDERED_AREA), BLOCK)).thenReturn(false);
    var firstOverlappedBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var secondOverlappedBlock = blockActiveBetween("30/3", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var otherActiveBlocks = List.of(firstOverlappedBlock, secondOverlappedBlock);
    when(featureService.findAllByAttribute("LAYER", "BLOCKS")).thenReturn(otherActiveBlocks);
    when(featureRelationshipService.get2dOverlappingFeatures(
        List.of(RETAINED_AREA, otherRetainedArea),
        otherActiveBlocks
    ))
        .thenReturn(List.of(firstOverlappedBlock, secondOverlappedBlock));

    var errors = partialSurrenderValidator.validate(partialSurrenderOfBlock(), STATE, POSITION_DATE, null);

    assertThat(errors).containsExactly(
        "Block 30/1: Block after surrender must not overlap licence block 30/2",
        "Block 30/1: Block after surrender must not overlap licence block 30/3"
    );
  }

  @Test
  void validate_whenSeveralBlocksPartiallySurrendered_thenEachCheckedForOverlapWithBlocksNotInSurrender() {
    var secondBlock = blockActiveBetween("30/4", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    var secondCommandJourneyId = UUID.randomUUID();
    var secondSurrenderedArea = FeatureTestUtil.builder().build();
    var secondRetainedArea = FeatureTestUtil.builder().build();
    var state = STATE.withBlock(secondBlock.getId(), List.of());
    when(featureService.getFeaturesByIds(Set.of(BLOCK.getId(), secondBlock.getId())))
        .thenReturn(List.of(BLOCK, secondBlock));
    givenValidAreaSelection();
    when(commandJourneyService.getActiveFeatures(secondCommandJourneyId))
        .thenReturn(List.of(secondSurrenderedArea, secondRetainedArea));
    when(featureRelationshipService.isTopologicallyEqual(List.of(secondSurrenderedArea), secondBlock))
        .thenReturn(false);
    var overlappedBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, POSITION_DATE.minusYears(1), null);
    when(featureService.findAllByAttribute("LAYER", "BLOCKS")).thenReturn(List.of(secondBlock, overlappedBlock));
    when(featureRelationshipService.get2dOverlappingFeatures(List.of(RETAINED_AREA), List.of(overlappedBlock)))
        .thenReturn(List.of(overlappedBlock));
    when(featureRelationshipService.get2dOverlappingFeatures(List.of(secondRetainedArea), List.of(overlappedBlock)))
        .thenReturn(List.of(overlappedBlock));
    var surrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(BLOCK.getId(), secondBlock.getId()))
        .withSurrenderDetails(Map.of(
            BLOCK.getId(),
            new SurrenderDetails(BlockSurrenderType.PARTIAL_SURRENDER, COMMAND_JOURNEY_ID, List.of(SURRENDERED_AREA.getId())),
            secondBlock.getId(),
            new SurrenderDetails(
                BlockSurrenderType.PARTIAL_SURRENDER,
                secondCommandJourneyId,
                List.of(secondSurrenderedArea.getId())
            )
        ))
        .build();

    var errors = partialSurrenderValidator.validate(surrender, state, POSITION_DATE, null);

    assertThat(errors).containsExactlyInAnyOrder(
        "Block 30/1: Block after surrender must not overlap licence block 30/2",
        "Block 30/4: Block after surrender must not overlap licence block 30/2"
    );
  }

  @ParameterizedTest
  @MethodSource("datesActiveOnPositionDate")
  void validate_whenBlockActiveOnPositionDate_thenCheckedForOverlap(LocalDate startDate, LocalDate endDate) {
    givenBlockOnPosition();
    givenValidAreaSelection();
    var otherBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, startDate, endDate);
    when(featureService.findAllByAttribute("LAYER", "BLOCKS")).thenReturn(List.of(otherBlock));
    when(featureRelationshipService.get2dOverlappingFeatures(List.of(RETAINED_AREA), List.of(otherBlock)))
        .thenReturn(List.of());

    var errors = partialSurrenderValidator.validate(partialSurrenderOfBlock(), STATE, POSITION_DATE, null);

    assertThat(errors).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("datesNotActiveOnPositionDate")
  void validate_whenBlockNotActiveOnPositionDate_thenNotCheckedForOverlap(LocalDate startDate, LocalDate endDate) {
    givenBlockOnPosition();
    givenValidAreaSelection();
    var otherBlock = blockActiveBetween("30/2", CoordinateSystem.ED50, startDate, endDate);
    when(featureService.findAllByAttribute("LAYER", "BLOCKS")).thenReturn(List.of(otherBlock));
    when(featureRelationshipService.get2dOverlappingFeatures(List.of(RETAINED_AREA), List.of())).thenReturn(List.of());

    var errors = partialSurrenderValidator.validate(partialSurrenderOfBlock(), STATE, POSITION_DATE, null);

    assertThat(errors).isEmpty();
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

  private void givenValidAreaSelection() {
    when(commandJourneyService.getActiveFeatures(COMMAND_JOURNEY_ID)).thenReturn(List.of(SURRENDERED_AREA, RETAINED_AREA));
    when(featureRelationshipService.isTopologicallyEqual(List.of(SURRENDERED_AREA), BLOCK)).thenReturn(false);
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
