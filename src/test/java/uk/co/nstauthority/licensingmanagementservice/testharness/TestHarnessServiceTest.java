package uk.co.nstauthority.licensingmanagementservice.testharness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SetEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.TransferEquityOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;

@ExtendWith(MockitoExtension.class)
class TestHarnessServiceTest {

  private static final int BP_EXPLORATION_ALPHA_LTD_ID = 304;
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

  private static final Feature SURRENDERED_BLOCK = FeatureTestUtil.blockFeature(UUID.randomUUID(), "30", 7);

  @Mock
  private LicenceTransactionService licenceTransactionService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionTestHarnessService licencePositionTestHarnessService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicencePositionFeatureTestHarnessService licencePositionFeatureTestHarnessService;

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
        licenceTransactionService, licencePositionService, licencePositionTestHarnessService,
        licencePositionChangeService, licencePositionFeatureTestHarnessService, CLOCK);
  }

  @Test
  void generateLicencePositions_createsFeaturesOnBothLicences() {
    var licence = carbonStorage(1);
    var secondaryLicence = carbonStorage(2);
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(buildPositions(5));

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
    var positions = buildPositions(5);
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(positions);
    when(licencePositionService.getBlockFeatures(positions.get(3))).thenReturn(List.of(SURRENDERED_BLOCK));

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionTestHarnessService).clearPositionsForLicence(licence);
    verify(licencePositionTestHarnessService).clearPositionsForLicence(secondaryLicence);

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
  void generateLicencePositions_whenPrimaryIsProduction_addsAdministratorChangeOnFirstAndNonFinalPosition() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = buildPositions(5);
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(positions);
    when(licencePositionService.getBlockFeatures(positions.get(3))).thenReturn(List.of(SURRENDERED_BLOCK));

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionChangeService, times(3)).createLicencePositionChange(
        positionCaptor.capture(), operationsCaptor.capture(), eq(1), eq(LicencePositionChangeStatus.CONSENTED));

    // first = index 0 (Shell); non-final = index size - 3 = 2 (BP)
    assertThat(positionCaptor.getAllValues()).startsWith(positions.get(0), positions.get(2));
    assertThat(operationsCaptor.getAllValues().stream().flatMap(List::stream))
        .filteredOn(AdministratorOperation.class::isInstance)
        .extracting(operation -> ((AdministratorOperation) operation).operatorId())
        .containsExactly(SHELL_PLC_ID, BP_EXPLORATION_ALPHA_LTD_ID);
  }

  @Test
  void generateLicencePositions_whenPrimaryIsProduction_addsPartialSurrenderOnPenultimatePosition() {
    var licence = production(1);
    var secondaryLicence = carbonStorage(2);
    var positions = buildPositions(5);
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(positions);
    when(licencePositionService.getBlockFeatures(positions.get(3))).thenReturn(List.of(SURRENDERED_BLOCK));

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionChangeService, times(3)).createLicencePositionChange(
        positionCaptor.capture(), operationsCaptor.capture(), eq(1), eq(LicencePositionChangeStatus.CONSENTED));

    // penultimate = index size - 2 = 3
    assertThat(positionCaptor.getAllValues().getLast()).isEqualTo(positions.get(3));

    // no surrender date - the change takes the date of the position it sits on
    var expectedSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(SURRENDERED_BLOCK.getId()))
        .withBlockSurrenderTypeByFeatureId(Map.of(SURRENDERED_BLOCK.getId(), BlockSurrenderType.FULL_SURRENDER))
        .build();
    assertThat(operationsCaptor.getAllValues().getLast()).containsExactly(expectedSurrender);
  }

  @Test
  void generateLicencePositions_whenSecondaryIsProduction_addsInitialAdministratorOnFirstPosition() {
    // primary is gas storage so that only the secondary production licence produces a change
    var licence = gasStorage(1);
    var secondaryLicence = production(2);
    var secondaryPositions = buildPositions(1);
    when(licencePositionService.getExecutedChronologicalLicencePositions(secondaryLicence)).thenReturn(secondaryPositions);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    verify(licencePositionChangeService).createLicencePositionChange(
        positionCaptor.capture(), operationsCaptor.capture(), eq(1), eq(LicencePositionChangeStatus.CONSENTED));

    assertThat(positionCaptor.getValue()).isEqualTo(secondaryPositions.getFirst());
    assertThat(((AdministratorOperation) operationsCaptor.getValue().getFirst()).operatorId())
        .isEqualTo(SHELL_PLC_ID);
  }

  @Test
  void generateLicencePositions_whenPrimaryIsCarbonStorage_addsSetOnFirstAndTransferOnNonFinalPosition() {
    var licence = carbonStorage(1);
    var secondaryLicence = carbonStorage(2);
    var positions = buildPositions(5);
    when(licencePositionService.getExecutedChronologicalLicencePositions(licence)).thenReturn(positions);

    testHarnessService.generateLicencePositions(licence, secondaryLicence);

    // the secondary carbon storage licence is not enriched (it only has a single position)
    verify(licencePositionService, never()).getExecutedChronologicalLicencePositions(secondaryLicence);

    verify(licencePositionChangeService, times(2)).createLicencePositionChange(
        positionCaptor.capture(), operationsCaptor.capture(), eq(1), eq(LicencePositionChangeStatus.CONSENTED));

    // set on the first position (index 0), transfer on a non-final position (index size - 3 = 2)
    assertThat(positionCaptor.getAllValues()).containsExactly(positions.get(0), positions.get(2));

    var setOperations = operationsCaptor.getAllValues().get(0);
    assertThat(setOperations)
        .extracting(
            operation -> ((SetEquityOperation) operation).transferTo(),
            operation -> ((SetEquityOperation) operation).equity())
        .containsExactly(
            tuple(SHELL_PLC_ID, BigDecimal.valueOf(60)),
            tuple(BP_EXPLORATION_ALPHA_LTD_ID, BigDecimal.valueOf(40)),
            tuple(PPRS_TRAINING_ORG, BigDecimal.ZERO));

    var transferOperations = operationsCaptor.getAllValues().get(1);
    assertThat(transferOperations)
        .extracting(
            operation -> ((TransferEquityOperation) operation).transferFrom(),
            operation -> ((TransferEquityOperation) operation).transferTo(),
            operation -> ((TransferEquityOperation) operation).equity())
        .containsExactly(
            tuple(SHELL_PLC_ID, BP_EXPLORATION_ALPHA_LTD_ID, BigDecimal.valueOf(10)),
            tuple(SHELL_PLC_ID, PPRS_TRAINING_ORG, BigDecimal.valueOf(15)));

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

  private static List<LicencePosition> buildPositions(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> LicencePositionTestUtil.newBuilder().build())
        .toList();
  }
}