package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourney;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.StorageOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaSurrenderOutcome;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenceCleardownService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;
import uk.co.nstauthority.licensingmanagementservice.testharness.LicencePositionFeatureTestHarnessService.SeededFeatures;
import uk.co.nstauthority.licensingmanagementservice.testharness.LicencePositionFeatureTestHarnessService.SplitBlock;

@ExtendWith(MockitoExtension.class)
class TestHarnessServiceTest {

  private static final int BP_EXPLORATION_ALPHA_LTD_ID = 304;
  private static final int FLAMSTONE_OIL_AND_GAS_LIMITED = 376;
  private static final int SHELL_PLC_ID = 9205;
  private static final int PPRS_TRAINING_ORG = 12845;

  private static final LocalDate TODAY = LocalDate.of(2026, Month.JUNE, 10);
  private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

  private static final LicenceTransaction TRANSACTION_1 = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction TRANSACTION_2 = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction TRANSACTION_3 = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction TRANSACTION_4 = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction TRANSACTION_5 = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction PAIR_TRANSACTION_1 = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction PAIR_TRANSACTION_2 = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction SAME_TRANSACTION = LicenceTransactionTestUtil.newBuilder().build();
  private static final LicenceTransaction CROSS_TRANSACTION = LicenceTransactionTestUtil.newBuilder().build();

  private static final Feature FULLY_SURRENDERED_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 7);
  private static final Feature PARTIALLY_SURRENDERED_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 8);
  private static final Feature SPLIT_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 9);
  private static final Feature STRATA_SPLIT_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 10);
  private static final Feature FULLY_SURRENDERED_SUBAREA = subarea("ALL");
  private static final Feature RELINQUISHED_SUBAREA = subarea("A");
  private static final Feature SUBAREA_ACROSS_THE_CUT = subarea("B");
  private static final Feature KEPT_SUBAREA = subarea("C");
  private static final Feature CROPPED_SUBAREA = subarea("B");
  private static final Feature MERGED_SUBAREA = subarea("ALL");
  private static final Feature SPLIT_SUBAREA = subarea("ALL");
  private static final Feature WESTERN_SUBAREA_HALF = subarea("A");
  private static final Feature EASTERN_SUBAREA_HALF = subarea("B");
  private static final Feature STRATA_SPLIT_SUBAREA = subarea("ALL");
  private static final Feature SHALLOW_STRATUM = subarea("ABOVE");
  private static final Feature DEEP_STRATUM = subarea("BELOW");
  private static final Feature SURRENDERED_HALF = FeatureTestUtil.builder()
      .withFeatureName("30/8 western half")
      .build();
  private static final Feature RETAINED_HALF = FeatureTestUtil.builder()
      .withFeatureName("30/8 eastern half")
      .build();
  private static final UUID PARTIAL_SURRENDER_COMMAND_JOURNEY_ID = UUID.randomUUID();
  private static final int PRODUCTION_POSITION_COUNT = 10;
  private static final int PRODUCTION_FIRST_ORDER_CHANGE_COUNT = 9;

  @Mock
  private LicenceTransactionService licenceTransactionService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicenceCleardownService licenceCleardownService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService;

  @Mock
  private CommandJourneyService commandJourneyService;

  private TestHarnessService testHarnessService;

  @Captor
  private ArgumentCaptor<Licence> licenceCaptor;

  @Captor
  private ArgumentCaptor<LicenceTransaction> transactionCaptor;

  @Captor
  private ArgumentCaptor<LocalDate> dateCaptor;

  @Captor
  private ArgumentCaptor<LicencePosition> positionCaptor;

  @Captor
  private ArgumentCaptor<List<LicenceOperation>> operationsCaptor;

  @BeforeEach
  void setUp() {
    testHarnessService = new TestHarnessService(
        licenceTransactionService, licencePositionService, licenceCleardownService,
        licencePositionChangeService, licencePositionFeatureTestHarnessService, commandJourneyService, CLOCK);
  }

  private List<LicencePosition> givenPositions(
      Licence licence,
      int count
  ) {
    var positions = IntStream.range(0, count)
        .mapToObj(i -> LicencePositionTestUtil.newBuilder().withLicence(licence).build())
        .toList();
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(positions);
    return positions;
  }

  private List<LicencePosition> givenProductionLicence(
      Licence licence,
      Licence secondaryLicence
  ) {
    var positions = givenPositions(licence, PRODUCTION_POSITION_COUNT);
    givenPositions(secondaryLicence, 1);

    when(licencePositionFeatureTestHarnessService.createAndLinkFeatures(licence)).thenReturn(new SeededFeatures(
        List.of(FULLY_SURRENDERED_BLOCK, PARTIALLY_SURRENDERED_BLOCK, SPLIT_BLOCK, STRATA_SPLIT_BLOCK),
        Map.of(
            FULLY_SURRENDERED_BLOCK.getId(), List.of(FULLY_SURRENDERED_SUBAREA),
            PARTIALLY_SURRENDERED_BLOCK.getId(), List.of(RELINQUISHED_SUBAREA, SUBAREA_ACROSS_THE_CUT, KEPT_SUBAREA),
            SPLIT_BLOCK.getId(), List.of(SPLIT_SUBAREA),
            STRATA_SPLIT_BLOCK.getId(), List.of(STRATA_SPLIT_SUBAREA)
        )
    ));
    when(licencePositionFeatureTestHarnessService.strataSplitSubarea(
        STRATA_SPLIT_SUBAREA,
        STRATA_SPLIT_BLOCK,
        -3000,
        positions.get(2).getPositionDate()
    ))
        .thenReturn(List.of(SHALLOW_STRATUM, DEEP_STRATUM));
    when(licencePositionFeatureTestHarnessService.splitSubareaInHalf(
        SPLIT_SUBAREA,
        SPLIT_BLOCK,
        positions.get(5).getPositionDate()
    ))
        .thenReturn(List.of(WESTERN_SUBAREA_HALF, EASTERN_SUBAREA_HALF));

    var partialSurrenderCommandJourney = commandJourney(PARTIAL_SURRENDER_COMMAND_JOURNEY_ID);
    when(commandJourneyService.createAndAssignCommandJourney(List.of(PARTIALLY_SURRENDERED_BLOCK)))
        .thenReturn(partialSurrenderCommandJourney);
    when(licencePositionFeatureTestHarnessService
        .splitBlockInHalf(partialSurrenderCommandJourney, PARTIALLY_SURRENDERED_BLOCK))
        .thenReturn(new SplitBlock(SURRENDERED_HALF, RETAINED_HALF));
    when(licencePositionFeatureTestHarnessService.cropSubareaAcrossTheMiddle(
        SUBAREA_ACROSS_THE_CUT,
        PARTIALLY_SURRENDERED_BLOCK,
        penultimate(positions).getPositionDate()
    ))
        .thenReturn(CROPPED_SUBAREA);
    when(licencePositionFeatureTestHarnessService.mergeSubareasOnRetainedHalf(
        List.of(CROPPED_SUBAREA, KEPT_SUBAREA),
        PARTIALLY_SURRENDERED_BLOCK,
        positions.getLast().getPositionDate()
    ))
        .thenReturn(MERGED_SUBAREA);

    return positions;
  }

  private List<LicenceOperation> capturedOperationsOn(LicencePosition position) {
    verify(licencePositionChangeService, times(PRODUCTION_FIRST_ORDER_CHANGE_COUNT)).createLicencePositionChange(
        positionCaptor.capture(),
        operationsCaptor.capture(),
        eq(1),
        eq(LicencePositionChangeStatus.CONSENTED)
    );
    var positions = positionCaptor.getAllValues();
    var operations = operationsCaptor.getAllValues();
    return IntStream.range(0, positions.size())
        .filter(index -> positions.get(index).equals(position))
        .mapToObj(operations::get)
        .flatMap(List::stream)
        .toList();
  }

  private static SubareaDetails subareaDetails(Feature subarea) {
    var name = subarea.getAttributes().get("NAME");
    return new SubareaDetails(subarea.getId(), name, name);
  }

  private static LicencePosition penultimate(List<LicencePosition> positions) {
    return positions.get(positions.size() - 2);
  }

  private static Feature subarea(String name) {
    return FeatureTestUtil.builder()
        .withAttributes(Map.of("LAYER", "SUBAREAS", "NAME", name))
        .build();
  }

  private static CommandJourney commandJourney(UUID id) {
    var commandJourney = new CommandJourney();
    commandJourney.setId(id);
    return commandJourney;
  }

  @Test
  void generateLicencePositions_createsFeaturesOnBothLicences() {
    var licence = carbonStorage(1);
    var secondaryLicence = carbonStorage(2);
    givenPositions(licence, 5);
    givenPositions(secondaryLicence, 1);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionFeatureTestHarnessService).createAndLinkFeatures(licence);
    verify(licencePositionFeatureTestHarnessService).createAndLinkFeatures(secondaryLicence);
  }

  @Test
  void generateLicencePositions_clearsBothLicencesAndCreatesElevenPositions() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    when(licenceTransactionService.createLicenceTransaction(anyString()))
        .thenReturn(TRANSACTION_1, TRANSACTION_2, TRANSACTION_3, TRANSACTION_4, TRANSACTION_5,
            PAIR_TRANSACTION_1, PAIR_TRANSACTION_2, SAME_TRANSACTION, CROSS_TRANSACTION);
    givenProductionLicence(licence, secondaryLicence);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licenceCleardownService).clear(licence);
    verify(licenceCleardownService).clear(secondaryLicence);
    verify(licencePositionFeatureTestHarnessService).clearFeatures(licence);
    verify(licencePositionFeatureTestHarnessService).clearFeatures(secondaryLicence);

    verify(licenceTransactionService, times(9)).createLicenceTransaction(anyString());
    verify(licencePositionService, times(11))
        .createLicencePosition(licenceCaptor.capture(), transactionCaptor.capture(), dateCaptor.capture());

    assertThat(licenceCaptor.getAllValues()).containsExactly(
        licence, licence, licence, licence, licence,  // 1. Five same date positions on primary licence
        licence, licence,                             // 2. Same date pair on primary licence
        licence, licence,                             // 3. Same transaction pair on primary licence
        licence, secondaryLicence);                   // 4. Cross licence reuse

    assertThat(transactionCaptor.getAllValues()).containsExactly(
        TRANSACTION_1, TRANSACTION_2, TRANSACTION_3, TRANSACTION_4, TRANSACTION_5,
        PAIR_TRANSACTION_1, PAIR_TRANSACTION_2,
        SAME_TRANSACTION, SAME_TRANSACTION,
        CROSS_TRANSACTION, CROSS_TRANSACTION);

    assertThat(dateCaptor.getAllValues()).containsExactly(
        TODAY.minusWeeks(7), TODAY.minusWeeks(7), TODAY.minusWeeks(7), TODAY.minusWeeks(7), TODAY.minusWeeks(7),
        TODAY.minusWeeks(4), TODAY.minusWeeks(4),
        TODAY.minusWeeks(5), TODAY.minusWeeks(3),
        TODAY.minusWeeks(2), TODAY.minusWeeks(1));
  }

  @Test
  void generateLicencePositions_whenPrimaryIsProduction_addsLicenseeChangeOnFirstAndNonFinalPosition() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = givenProductionLicence(licence, secondaryLicence);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionChangeService).createLicencePositionChange(
        positions.getFirst(),
        List.of(new LicenseeOperation(
            LicenseeOperation.LICENSEE_OPERATION_ID,
            List.of(BP_EXPLORATION_ALPHA_LTD_ID, FLAMSTONE_OIL_AND_GAS_LIMITED),
            List.of()
        )),
        1,
        LicencePositionChangeStatus.CONSENTED
    );
    verify(licencePositionChangeService).createLicencePositionChange(
        positions.get(6),
        List.of(new LicenseeOperation(
            LicenseeOperation.LICENSEE_OPERATION_ID,
            List.of(SHELL_PLC_ID),
            List.of(BP_EXPLORATION_ALPHA_LTD_ID)
        )),
        1,
        LicencePositionChangeStatus.CONSENTED
    );
  }

  @Test
  void generateLicencePositions_whenPrimaryIsProduction_addsAdministratorChangeOnFirstAndNonFinalPosition() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = givenProductionLicence(licence, secondaryLicence);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionChangeService).createLicencePositionChange(
        positions.getFirst(),
        List.of(new AdministratorOperation(AdministratorOperation.ADMINISTRATOR_OPERATION_ID, SHELL_PLC_ID)),
        1,
        LicencePositionChangeStatus.CONSENTED
    );
    verify(licencePositionChangeService).createLicencePositionChange(
        positions.get(7),
        List.of(new AdministratorOperation(AdministratorOperation.ADMINISTRATOR_OPERATION_ID, BP_EXPLORATION_ALPHA_LTD_ID)),
        1,
        LicencePositionChangeStatus.CONSENTED
    );
  }

  @Test
  void generateLicencePositions_whenPrimaryIsProduction_addsPartialSurrenderOnPenultimatePosition() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = givenProductionLicence(licence, secondaryLicence);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    var expectedFullSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.FULL_SURRENDER,
        null,
        List.of(FULLY_SURRENDERED_BLOCK.getId()),
        List.of(),
        Map.of()
    );
    var expectedPartialSurrender = new PartialSurrenderOperation.SurrenderDetails(
        BlockSurrenderType.PARTIAL_SURRENDER,
        null,
        List.of(SURRENDERED_HALF.getId()),
        List.of(RETAINED_HALF.getId()),
        Map.of(RETAINED_HALF.getId(), List.of(
            SubareaSurrenderOutcome.relinquished(subareaDetails(RELINQUISHED_SUBAREA)),
            SubareaSurrenderOutcome.cropped(subareaDetails(SUBAREA_ACROSS_THE_CUT), subareaDetails(CROPPED_SUBAREA)),
            SubareaSurrenderOutcome.kept(subareaDetails(KEPT_SUBAREA))
        ))
    );
    LicenceOperation expectedSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(FULLY_SURRENDERED_BLOCK.getId(), PARTIALLY_SURRENDERED_BLOCK.getId()))
        .withSurrenderDetails(Map.of(
            FULLY_SURRENDERED_BLOCK.getId(), expectedFullSurrender,
            PARTIALLY_SURRENDERED_BLOCK.getId(), expectedPartialSurrender
        ))
        .build();
    verify(licencePositionChangeService).createLicencePositionChange(
        penultimate(positions), List.of(expectedSurrender), 1, LicencePositionChangeStatus.CONSENTED);
    verify(commandJourneyService).deleteAllExcludingActiveFeatures(PARTIAL_SURRENDER_COMMAND_JOURNEY_ID);
  }

  @Test
  void generateLicencePositions_whenPrimaryIsProduction_endsTheRelinquishedSubareaOnTheSurrenderDate() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = givenProductionLicence(licence, secondaryLicence);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionFeatureTestHarnessService)
        .relinquishSubarea(RELINQUISHED_SUBAREA, penultimate(positions).getPositionDate());
  }

  @Test
  void generateLicencePositions_whenPrimaryIsProduction_addsStrataSplitOnTheThirdPosition() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = givenProductionLicence(licence, secondaryLicence);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    var expectedStrataSplit = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(STRATA_SPLIT_BLOCK.getId())
        .withReplacedSubareas(List.of(subareaDetails(STRATA_SPLIT_SUBAREA)))
        .withOutputSubareas(List.of(subareaDetails(SHALLOW_STRATUM), subareaDetails(DEEP_STRATUM)))
        .build();
    assertThat(capturedOperationsOn(positions.get(2)))
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(expectedStrataSplit);
  }

  @Test
  void generateLicencePositions_whenPrimaryIsProduction_addsSubareaSplitOnTheSixthPosition() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = givenProductionLicence(licence, secondaryLicence);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    var expectedSplit = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(SPLIT_BLOCK.getId())
        .withReplacedSubareas(List.of(subareaDetails(SPLIT_SUBAREA)))
        .withOutputSubareas(List.of(subareaDetails(WESTERN_SUBAREA_HALF), subareaDetails(EASTERN_SUBAREA_HALF)))
        .build();
    assertThat(capturedOperationsOn(positions.get(5)))
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(expectedSplit);
  }

  @Test
  void generateLicencePositions_whenPrimaryIsProduction_mergesTheSubareasTheSurrenderKeptOnTheFinalPosition() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = givenProductionLicence(licence, secondaryLicence);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    var expectedMerge = LicenceOperation.newSubAreaOperation()
        .withBlockFeatureId(RETAINED_HALF.getId())
        .withReplacedSubareas(List.of(subareaDetails(CROPPED_SUBAREA), subareaDetails(KEPT_SUBAREA)))
        .withOutputSubareas(List.of(subareaDetails(MERGED_SUBAREA)))
        .build();
    assertThat(capturedOperationsOn(positions.getLast()))
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
        .containsExactly(expectedMerge);
  }

  @Test
  void generateLicencePositions_whenPrimaryIsProduction_addsSubareaBeneficialInterestOnASplitSubareaOnTheFinalPosition() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = givenProductionLicence(licence, secondaryLicence);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    var westernSubareaHalf = subareaDetails(WESTERN_SUBAREA_HALF);
    verify(licencePositionChangeService).createLicencePositionChange(
        positions.getLast(),
        List.of(
            new SubareaSetEquityOperation(
                SPLIT_BLOCK.getId(), westernSubareaHalf, SHELL_PLC_ID, BigDecimal.valueOf(60)),
            new SubareaSetEquityOperation(
                SPLIT_BLOCK.getId(), westernSubareaHalf, BP_EXPLORATION_ALPHA_LTD_ID, BigDecimal.valueOf(40))
        ),
        2,
        LicencePositionChangeStatus.CONSENTED
    );
  }

  @Test
  void generateLicencePositions_whenSecondaryIsProduction_addsInitialAdministratorOnFirstPosition() {
    var licence = gasStorage(1);
    givenPositions(licence, 5);
    var secondaryLicence = production(2);
    var secondaryPositions = givenPositions(secondaryLicence, 1);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionChangeService, times(3)).createLicencePositionChange(
        positionCaptor.capture(), operationsCaptor.capture(), eq(1), eq(LicencePositionChangeStatus.CONSENTED));

    assertThat(positionCaptor.getValue()).isEqualTo(secondaryPositions.getFirst());
    assertThat(operationsCaptor.getValue())
        .containsExactly(new AdministratorOperation(AdministratorOperation.ADMINISTRATOR_OPERATION_ID, SHELL_PLC_ID));
  }

  @Test
  void generateLicencePositions_whenSecondaryIsCarbonStorage_addsInitialStorageOperatorOnFirstPosition() {
    var licence = gasStorage(1);
    givenPositions(licence, 5);
    var secondaryLicence = carbonStorage(2);
    var secondaryPositions = givenPositions(secondaryLicence, 1);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionChangeService, times(3)).createLicencePositionChange(
        positionCaptor.capture(), operationsCaptor.capture(), eq(1), eq(LicencePositionChangeStatus.CONSENTED));

    assertThat(positionCaptor.getValue()).isEqualTo(secondaryPositions.getFirst());
    assertThat(operationsCaptor.getValue())
        .containsExactly(new StorageOperatorOperation(StorageOperatorOperation.STORAGE_OPERATOR_OPERATION_ID, SHELL_PLC_ID));
  }

  @Test
  void generateLicencePositions_whenPrimaryIsCarbonStorage_addsStorageOperatorOnFirstAndNonFinalPosition() {
    var licence = carbonStorage(1);
    var secondaryLicence = production(2);
    givenPositions(secondaryLicence, 1);
    var positions = givenPositions(licence, 5);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionChangeService).createLicencePositionChange(
        positions.getFirst(),
        List.of(new StorageOperatorOperation(StorageOperatorOperation.STORAGE_OPERATOR_OPERATION_ID, SHELL_PLC_ID)),
        1,
        LicencePositionChangeStatus.CONSENTED
    );
    verify(licencePositionChangeService).createLicencePositionChange(
        positions.get(2),
        List.of(new StorageOperatorOperation(
            StorageOperatorOperation.STORAGE_OPERATOR_OPERATION_ID,
            BP_EXPLORATION_ALPHA_LTD_ID
        )),
        1,
        LicencePositionChangeStatus.CONSENTED
    );
  }

  @Test
  void generateLicencePositions_whenPrimaryIsCarbonStorage_addsSetOnFirstAndTransferOnNonFinalPosition() {
    var licence = carbonStorage(1);
    var secondaryLicence = carbonStorage(2);
    var positions = givenPositions(licence, 5);
    givenPositions(secondaryLicence, 1);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionChangeService).createLicencePositionChange(
        positions.getFirst(),
        List.of(
            new SetEquityOperation(SHELL_PLC_ID, BigDecimal.valueOf(60)),
            new SetEquityOperation(BP_EXPLORATION_ALPHA_LTD_ID, BigDecimal.valueOf(40)),
            new SetEquityOperation(PPRS_TRAINING_ORG, BigDecimal.ZERO)
        ),
        1,
        LicencePositionChangeStatus.CONSENTED
    );
    verify(licencePositionChangeService).createLicencePositionChange(
        positions.get(2),
        List.of(
            new TransferEquityOperation(SHELL_PLC_ID, BP_EXPLORATION_ALPHA_LTD_ID, BigDecimal.valueOf(10), null),
            new TransferEquityOperation(SHELL_PLC_ID, PPRS_TRAINING_ORG, BigDecimal.valueOf(15), null)
        ),
        1,
        LicencePositionChangeStatus.CONSENTED
    );

    verify(licencePositionChangeService, times(7)).createLicencePositionChange(
        positionCaptor.capture(), operationsCaptor.capture(), eq(1), eq(LicencePositionChangeStatus.CONSENTED));
    assertThat(operationsCaptor.getAllValues().stream().flatMap(List::stream))
        .noneMatch(AdministratorOperation.class::isInstance)
        .noneMatch(PartialSurrenderOperation.class::isInstance);
  }

  private static Licence production(int id) {
    return LicenceTestUtil.builder().withId(id).withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
  }

  private static Licence carbonStorage(int id) {
    return LicenceTestUtil.builder().withId(id).withLicenceType(LicenceType.CARBON_STORAGE).build();
  }

  private static Licence gasStorage(int id) {
    return LicenceTestUtil.builder().withId(id).withLicenceType(LicenceType.GAS_STORAGE).build();
  }
}
