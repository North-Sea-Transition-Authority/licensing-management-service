package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenseeOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsCompanyListType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsHistoryTestUtil;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperationType;

class LicenseeOperationMigratorTest {

  private static final LocalDate FIRST_DATE = LocalDate.of(1964, Month.SEPTEMBER, 18);
  private static final LocalDate LAST_DATE = LocalDate.of(1972, Month.MARCH, 1);

  private final LicenseeOperationMigrator licenseeOperationMigrator = new LicenseeOperationMigrator();

  private MigrationNotes notes;

  @BeforeEach
  void setUp() {
    notes = new MigrationNotes();
  }

  @Test
  void pearsOperationTypes() {
    assertThat(licenseeOperationMigrator.pearsOperationTypes())
        .containsExactlyInAnyOrder(
            PearsOperationType.CONSORTIUM_LIST_CREATE, PearsOperationType.CONSORTIUM_LIST_CHANGE);
  }

  @Test
  void supports_whenTheOperationIsTheLicenseeList_thenItIsSupported() {
    var history = history(position -> position.licenseeSet(11));

    var operation = history.positions().getFirst().operations().getFirst();

    assertThat(licenseeOperationMigrator.supports(operation)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      PearsCompanyListType.LICENCE_ADMINISTRATOR,
      PearsCompanyListType.BENEFICIAL_INTEREST,
      PearsCompanyListType.SUBAREA_OPERATOR
  })
  void supports_whenTheOperationIsAnotherCompanyList_thenItIsNotSupported(String companyListType) {
    var history = history(position -> position.companyList(companyListType, 11));

    var operation = history.positions().getFirst().operations().getFirst();

    assertThat(licenseeOperationMigrator.supports(operation)).isFalse();
  }

  @Test
  void supports_whenTheOperationIsOfATypeNotFetched_thenItIsNotSupported() {
    var history = history(position -> position.licenceEnd());

    var operation = history.positions().getFirst().operations().getFirst();

    assertThat(licenseeOperationMigrator.supports(operation)).isFalse();
  }

  @Test
  void migrate_whenTheLicenseeListIsCreated_thenEveryLicenseeJoinsOnThatPosition() {
    var history = history(position -> position.licenseeSet(11, 22, 33));

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11, 22, 33), List.of())));
  }

  @Test
  void migrate_whenLicenseesJoinAndWithdrawOnLaterPositions_thenEachPositionCarriesItsOwnChange() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 1)
        .licenseeSet(11, 22)
        .and()
        .position(2, "XPT/2", FIRST_DATE, 2)
        .licenseeSet(33)
        .and()
        .position(3, "XPT/3", LAST_DATE, 1)
        .licenseeRemove(11)
        .build());

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11, 22), List.of()),
            licenseeChange(2, new PearsOperationKey(1, 1001), List.of(33), List.of()),
            licenseeChange(3, new PearsOperationKey(1, 1002), List.of(), List.of(11))
        ));
    assertThat(notes.noteCountsByReason()).isEmpty();
    assertThat(notes.unmappedCountsByReason()).isEmpty();
  }

  @Test
  void migrate_whenALicenseeIsTransferred_thenThePrimaryWithdrawsAndTheSecondaryJoins() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 1)
        .licenseeSet(11, 22)
        .and()
        .position(2, "XPT/2", LAST_DATE, 1)
        .licenseeTransfer(11, 33)
        .build());

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11, 22), List.of()),
            licenseeChange(2, new PearsOperationKey(1, 1001), List.of(33), List.of(11))
        ));
  }

  @Test
  void migrate_whenALicenseeJoinsInsideACorrection_thenItIsCarriedAcross() {
    var history = history(position -> position.correctedLicenseeSet(11));

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11), List.of())));
  }

  @Test
  void migrate_whenOneTransactionHoldsSeveralLicenseeOperations_thenTheyAreCombinedAtTheFirst() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 1)
        .licenseeSet(11, 22)
        .and()
        .position(2, "XPT/2", LAST_DATE, 1)
        .operationWithoutPayload(PearsOperationType.LICENCE_CREATE)
        .licenseeSet(33)
        .licenseeRemove(22)
        .licenseeSet(44)
        .build());

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11, 22), List.of()),
            licenseeChange(2, new PearsOperationKey(2, 1002), List.of(33, 44), List.of(22))
        ));
    assertThat(notes.noteCountsByReason())
        .isEqualTo(Map.of("several licensee operations in one transaction combined into one change", 1));
  }

  @Test
  void migrate_whenALicenseeJoinsAndWithdrawsInOneTransaction_thenNoChangeIsMade() {
    var history = history(position -> position.licenseeSet(11).licenseeRemove(11));

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.noteCountsByReason())
        .isEqualTo(Map.of(
            "several licensee operations in one transaction combined into one change", 1,
            "licensee operations that leave the licensees unchanged, which is not a change", 1
        ));
  }

  @Test
  void migrate_whenALicenseeAlreadyHeldJoins_thenOnlyTheOthersJoin() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 1)
        .licenseeSet(11)
        .and()
        .position(2, "XPT/2", LAST_DATE, 1)
        .licenseeSet(11, 22)
        .build());

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11), List.of()),
            licenseeChange(2, new PearsOperationKey(1, 1001), List.of(22), List.of())
        ));
    assertThat(notes.noteCountsByReason()).isEqualTo(Map.of("licensee joining a licence it already holds", 1));
  }

  @Test
  void migrate_whenALicenseeNotHeldWithdraws_thenNoChangeIsMade() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 1)
        .licenseeSet(11)
        .and()
        .position(2, "XPT/2", LAST_DATE, 1)
        .licenseeRemove(22)
        .build());

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11), List.of())));
    assertThat(notes.noteCountsByReason())
        .isEqualTo(Map.of(
            "licensee withdrawing from a licence it does not hold", 1,
            "licensee operations that leave the licensees unchanged, which is not a change", 1
        ));
  }

  @Test
  void migrate_whenTheLastLicenseeWithdraws_thenTheChangeIsMadeAndItIsNoted() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 1)
        .licenseeSet(11)
        .and()
        .position(2, "XPT/2", LAST_DATE, 1)
        .licenseeRemove(11)
        .build());

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11), List.of()),
            licenseeChange(2, new PearsOperationKey(1, 1001), List.of(), List.of(11))
        ));
    assertThat(notes.noteCountsByReason())
        .isEqualTo(Map.of("licensee change leaves the licence with no licensees", 1));
  }

  @Test
  void migrate_whenAnEntryHasNoOrganisation_thenItIsReportedAndTheRestIsCarriedAcross() {
    var history = history(position -> position.licenseeSetWithNoOrganisation().licenseeSet(11));

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11), List.of())));
    assertThat(notes.unmappedCountsByReason())
        .isEqualTo(Map.of("licensee entry with no joining organisation", 1));
  }

  @Test
  void migrate_whenAnEntryHasNoType_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = history(position -> position.licenseeEntryOfUnknownType(11));

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason()).isEqualTo(Map.of("licensee entry with no ENTRY_TYPE", 1));
  }

  @ParameterizedTest
  @CsvSource({
      ", 33, licensee entry with no withdrawing organisation",
      "11, , licensee entry with no joining organisation"
  })
  void migrate_whenATransferIsMissingAnOrganisation_thenNeitherSideChangesAndItIsReported(
      Integer fromOrganisationId,
      Integer toOrganisationId,
      String unmappedReason
  ) {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 1)
        .licenseeSet(11)
        .and()
        .position(2, "XPT/2", LAST_DATE, 1)
        .licenseeTransfer(fromOrganisationId, toOrganisationId)
        .build());

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenseeChange(1, new PearsOperationKey(1, 1000), List.of(11), List.of())));
    assertThat(notes.unmappedCountsByReason()).isEqualTo(Map.of(unmappedReason, 1));
  }

  @Test
  void migrate_whenACompanyListIsNotTheLicensees_thenItIsNotCarriedAcross() {
    var history = history(position -> position.administratorSet(11));

    var changes = licenseeOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.noteCountsByReason()).isEmpty();
  }

  private static PearsLicenceHistory history(Consumer<PearsHistoryTestUtil.PositionBuilder> operations) {
    var position = PearsHistoryTestUtil.history("P", 8).position(1, "XPT/1", FIRST_DATE, 1);
    operations.accept(position);
    return PearsLicenceHistory.reconstruct(position.build());
  }

  private static MigratedChange licenseeChange(
      long transactionId,
      PearsOperationKey at,
      List<Integer> licenseesToAdd,
      List<Integer> licenseesToRemove
  ) {
    return new MigratedChange(
        new PearsPositionKey(transactionId),
        at,
        List.of(new LicenseeOperation(LicenseeOperation.LICENSEE_OPERATION_ID, licenseesToAdd, licenseesToRemove)),
        LicencePositionChangeStatus.CONSENTED
    );
  }
}
