package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.position.transaction.LicenceTransactionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransactionService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.LicenceOperationHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsHistoryTestUtil;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperation;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperationType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigratedChange;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.PearsOperationKey;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.PearsOperationMigrator;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.PearsPositionKey;

@ExtendWith(MockitoExtension.class)
class LicenceWritebackServiceTest {

  private static final LocalDate POSITION_DATE = LocalDate.of(1964, 9, 18);

  @Mock
  private LicenceTransactionService licenceTransactionService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @Mock
  private LicenceCleardownService licenceCleardownService;

  @Mock
  private PearsLicenceService pearsLicenceService;

  private final uk.co.nstauthority.licensingmanagementservice.licence.Licence licence = LicenceTestUtil.builder()
      .withLicencePrefix("P")
      .withLicenceNumber("1")
      .withLicenceReference("P1")
      .build();

  private LicenceWritebackService licenceWritebackService;

  @BeforeEach
  void setUp() {
    licenceWritebackService = writebackServiceWith(List.of());
  }

  @Test
  void overwriteLicencePositionsFromPears_whenPearsHoldsNoPositions_thenNothingIsCleared() {
    givenPearsHolds(new LicenceOperationHistory("P", 1, List.of()));

    var writeback = licenceWritebackService.overwriteLicencePositionsFromPears(licence);

    assertThat(writeback.toResult())
        .isEqualTo(LicenceWritebackResult.nothingToDo("No positions found in PEARS for licence P1"));
    verifyNoInteractions(
        licenceCleardownService, licenceTransactionService, licencePositionService, licencePositionChangeService);
  }

  @Test
  void overwriteLicencePositionsFromPears_whenTheLicenceStartsAndEndsInPears_thenEveryPositionIsBuilt() {
    givenPearsHolds(PearsHistoryTestUtil.history("P", 1)
        .position(1, "XPT/1", POSITION_DATE, 6)
        .operationWithoutPayload(PearsOperationType.LICENCE_CREATE)
        .and()
        .position(2, "XPT/2", POSITION_DATE, 9)
        .licenceEnd()
        .build());
    givenPositionsAreCreated();

    var writeback = licenceWritebackService.overwriteLicencePositionsFromPears(licence);

    assertThat(writeback.positionsByTransactionId()).containsOnlyKeys(1L, 2L);
    verify(licenceTransactionService).createLicenceTransaction("XPT/1");
    verify(licenceTransactionService).createLicenceTransaction("XPT/2");
  }

  @Test
  void overwriteLicencePositionsFromPears_whenOnlySomeOperationsAreSupported_thenEachIsCountedAsIncludedOrIgnored() {
    givenPearsHolds(PearsHistoryTestUtil.history("P", 1)
        .position(1, "XPT/1", POSITION_DATE, 6)
        .operationWithoutPayload(PearsOperationType.LICENCE_CREATE)
        .operationWithoutPayload("PED_BLOCK_CREATE")
        .operationWithoutPayload("PED_BLOCK_CREATE")
        .and()
        .position(2, "XPT/2", POSITION_DATE, 9)
        .administratorSet(11)
        .build());
    givenPositionsAreCreated();

    licenceWritebackService = writebackServiceWith(List.of(
        stubMigrator("administrator", Set.of(), PearsOperationType.CONSORTIUM_LIST_CHANGE)
    ));

    var writeback = licenceWritebackService.overwriteLicencePositionsFromPears(licence);

    assertThat(writeback.includedOperationsByType())
        .isEqualTo(Map.of(PearsOperationType.CONSORTIUM_LIST_CHANGE, 1));
    assertThat(writeback.ignoredOperationsByType())
        .isEqualTo(Map.of(PearsOperationType.LICENCE_CREATE, 1, "PED_BLOCK_CREATE", 2));
    assertThat(writeback.toResult())
        .isEqualTo(new LicenceWritebackResult("Saved 2 positions, 0 changes and 0 operations for licence P1", 2, 0, 0, 3));
  }

  @Test
  void overwriteLicencePositionsFromPears_whenTwoMigratorsContributeToOnePosition_thenChangesTakePearsOrder() {
    var history = PearsHistoryTestUtil.history("P", 1)
        .position(1, "XPT/1", POSITION_DATE, 6)
        .operationWithoutPayload("PED_BLOCK_CREATE")
        .build();
    givenPearsHolds(history);

    var licencePosition = givenPositionsAreCreated();

    var second = LicenceOperation.newAdministratorChange().withOperator(22).build();
    var third = LicenceOperation.newAdministratorChange().withOperator(33).build();
    var first = LicenceOperation.newAdministratorChange().withOperator(11).build();

    licenceWritebackService = writebackServiceWith(List.of(
        stubMigrator("later", changeAt(1, 5, 500, third)),
        stubMigrator("earlier", changeAt(1, 1, 100, first), changeAt(1, 3, 300, second))
    ));

    licenceWritebackService.overwriteLicencePositionsFromPears(licence);

    var inOrder = inOrder(licencePositionChangeService);
    inOrder.verify(licencePositionChangeService)
        .createLicencePositionChange(licencePosition, List.of(first), 1, LicencePositionChangeStatus.CONSENTED);
    inOrder.verify(licencePositionChangeService)
        .createLicencePositionChange(licencePosition, List.of(second), 2, LicencePositionChangeStatus.CONSENTED);
    inOrder.verify(licencePositionChangeService)
        .createLicencePositionChange(licencePosition, List.of(third), 3, LicencePositionChangeStatus.CONSENTED);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void wantedOperationTypes_thenEveryMigratorsTypesAreFetched() {
    licenceWritebackService = writebackServiceWith(List.of(
        stubMigrator("administrator", Set.of(PearsOperationType.CONSORTIUM_LIST_CREATE)),
        stubMigrator("surrender", Set.of(PearsOperationType.CONSORTIUM_LIST_CREATE, "PED_BLOCK_END"))
    ));

    assertThat(licenceWritebackService.wantedOperationTypes())
        .containsExactlyInAnyOrder(PearsOperationType.CONSORTIUM_LIST_CREATE, "PED_BLOCK_END");
  }

  private LicenceWritebackService writebackServiceWith(List<PearsOperationMigrator> migrators) {
    return new LicenceWritebackService(
        licenceTransactionService,
        licencePositionService,
        licencePositionChangeService,
        licenceCleardownService,
        pearsLicenceService,
        migrators
    );
  }

  private void givenPearsHolds(LicenceOperationHistory history) {
    when(pearsLicenceService.licenceHistory(eq("P"), eq(1), org.mockito.ArgumentMatchers.anySet()))
        .thenReturn(history);
  }

  private LicencePosition givenPositionsAreCreated() {
    var licenceTransaction = LicenceTransactionTestUtil.newBuilder().build();
    var licencePosition = LicencePositionTestUtil.newBuilder().build();

    when(licenceTransactionService.createLicenceTransaction(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(licenceTransaction);
    when(licencePositionService.createLicencePosition(
        eq(licence), eq(licenceTransaction), org.mockito.ArgumentMatchers.any(LocalDate.class)))
        .thenReturn(licencePosition);

    return licencePosition;
  }

  private static MigratedChange changeAt(
      long transactionId,
      int operationSequence,
      long operationId,
      LicenceOperation operation
  ) {
    return MigratedChange.from(
        new PearsPositionKey(transactionId),
        new PearsOperationKey(operationSequence, operationId),
        List.of(operation)
    );
  }

  private static PearsOperationMigrator stubMigrator(String name, MigratedChange... changes) {
    return stubMigrator(name, Set.of(), changes);
  }

  private static PearsOperationMigrator stubMigrator(String name, Set<String> operationTypes, MigratedChange... changes) {
    return stubMigrator(name, operationTypes, null, changes);
  }

  /**
   * @param supportedOperationType the one OPERATION_TYPE the migrator supports; null for none
   */
  private static PearsOperationMigrator stubMigrator(
      String name,
      Set<String> operationTypes,
      String supportedOperationType,
      MigratedChange... changes
  ) {
    return new PearsOperationMigrator() {

      @Override
      public String name() {
        return name;
      }

      @Override
      public Set<String> pearsOperationTypes() {
        return operationTypes;
      }

      @Override
      public boolean supports(PearsOperation operation) {
        return operation.typeName().equals(supportedOperationType);
      }

      @Override
      public List<MigratedChange> migrate(PearsLicenceHistory history, MigrationNotes notes) {
        return List.of(changes);
      }
    };
  }
}
