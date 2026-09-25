package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceHistory;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsCompanyListType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsHistoryTestUtil;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.history.PearsOperationType;

class AdministratorOperationMigratorTest {

  private static final LocalDate FIRST_DATE = LocalDate.of(1964, 9, 18);
  private static final LocalDate LAST_DATE = LocalDate.of(1972, 3, 1);

  private final AdministratorOperationMigrator administratorOperationMigrator = new AdministratorOperationMigrator();

  private MigrationNotes notes;

  @BeforeEach
  void setUp() {
    notes = new MigrationNotes();
  }

  @Test
  void pearsOperationTypes_thenTheConsortiumListsAreFetched() {
    assertThat(administratorOperationMigrator.pearsOperationTypes())
        .containsExactlyInAnyOrder(
            PearsOperationType.CONSORTIUM_LIST_CREATE, PearsOperationType.CONSORTIUM_LIST_CHANGE);
  }

  @Test
  void supports_whenTheOperationIsTheAdministratorList_thenItIsSupported() {
    var history = history(position -> position.administratorSet(11));

    var operation = history.positions().getFirst().operations().getFirst();

    assertThat(administratorOperationMigrator.supports(operation)).isTrue();
  }

  @Test
  void supports_whenTheOperationIsAnotherCompanyList_thenItIsNotSupported() {
    var history = history(position -> position.companyList(PearsCompanyListType.BENEFICIAL_INTEREST, 11));

    var operation = history.positions().getFirst().operations().getFirst();

    assertThat(administratorOperationMigrator.supports(operation)).isFalse();
  }

  @Test
  void supports_whenTheOperationIsOfATypeNotFetched_thenItIsNotSupported() {
    var history = history(position -> position.licenceEnd());

    var operation = history.positions().getFirst().operations().getFirst();

    assertThat(administratorOperationMigrator.supports(operation)).isFalse();
  }

  @Test
  void produce_whenTheAdministratorIsSet_thenAChangeIsMadeOnThatPosition() {
    var history = history(position -> position.administratorSet(11));

    var changes = administratorOperationMigrator.migrate(history, notes);

    assertThat(changes)
        .usingRecursiveComparison()
        .isEqualTo(List.of(new MigratedChange(
            new PearsPositionKey(1),
            new PearsOperationKey(1, 1000),
            List.of(new AdministratorOperation(AdministratorOperation.ADMINISTRATOR_OPERATION_ID, 11)),
            LicencePositionChangeStatus.CONSENTED)));
  }

  @Test
  void produce_whenTheAdministratorIsTransferred_thenTheJoiningOrganisationIsCarriedAcross() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 1)
        .administratorSet(11)
        .and()
        .position(2, "XPT/2", LAST_DATE, 1)
        .administratorTransfer(11, 22)
        .build());

    var changes = administratorOperationMigrator.migrate(history, notes);

    assertThat(changes).extracting(AdministratorOperationMigratorTest::operatorId).containsExactly(11, 22);
  }

  @Test
  void produce_whenTheAdministratorIsRemoved_thenNothingIsCarriedAcross() {
    var history = history(position -> position.administratorRemove(11));

    var changes = administratorOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.noteCountsByReason())
        .containsEntry("licence administrator REMOVE dropped, because administrator state is single valued", 1);
  }

  @Test
  void produce_whenAnEntryHasNoOrganisation_thenNothingIsCarriedAcrossAndItIsReported() {
    var history = history(position -> position.administratorSetWithNoOrganisation());

    var changes = administratorOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
    assertThat(notes.unmappedCountsByReason())
        .containsEntry("licence administrator entry with no joining organisation", 1);
  }

  @Test
  void produce_whenACompanyListIsNotTheAdministrator_thenItIsNotCarriedAcross() {
    var history = history(position -> position.companyList(PearsCompanyListType.BENEFICIAL_INTEREST, 11));

    var changes = administratorOperationMigrator.migrate(history, notes);

    assertThat(changes).isEmpty();
  }

  @Test
  void produce_whenTheAdministratorIsSetInsideACorrection_thenItIsCarriedAcross() {
    var history = history(position -> position.correctedAdministratorSet(11));

    var changes = administratorOperationMigrator.migrate(history, notes);

    assertThat(changes).extracting(AdministratorOperationMigratorTest::operatorId).containsExactly(11);
  }

  @Test
  void produce_whenOneTransactionSetsTheAdministratorSeveralTimes_thenOnlyTheLastIsCarriedAcross() {
    var history = history(position -> position.administratorSet(11).administratorSet(22).administratorSet(33));

    var changes = administratorOperationMigrator.migrate(history, notes);

    assertThat(changes).extracting(AdministratorOperationMigratorTest::operatorId).containsExactly(33);
    assertThat(notes.noteCountsByReason())
        .containsEntry("several licence administrator operations in one transaction reduced to the last", 1);
  }

  @Test
  void produce_whenTheAdministratorIsSetToTheOneAlreadyHeld_thenNoChangeIsMade() {
    var history = PearsLicenceHistory.reconstruct(PearsHistoryTestUtil.history("P", 8)
        .position(1, "XPT/1", FIRST_DATE, 1)
        .administratorSet(11)
        .and()
        .position(2, "XPT/2", LAST_DATE, 1)
        .administratorSet(11)
        .build());

    var changes = administratorOperationMigrator.migrate(history, notes);

    assertThat(changes).extracting(change -> change.position().transactionId()).containsExactly(1L);
    assertThat(notes.noteCountsByReason())
        .containsEntry("licence administrator set to the administrator already held, which is not a change", 1);
  }

  private static PearsLicenceHistory history(Consumer<PearsHistoryTestUtil.PositionBuilder> operations) {
    var position = PearsHistoryTestUtil.history("P", 8).position(1, "XPT/1", FIRST_DATE, 1);
    operations.accept(position);
    return PearsLicenceHistory.reconstruct(position.build());
  }

  private static Integer operatorId(MigratedChange change) {
    return ((AdministratorOperation) change.operations().getFirst()).operatorId();
  }
}
