package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.event;
import static uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsEventFixture.term;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationOutcome.Result;

@ExtendWith(MockitoExtension.class)
class PearsScheduleMigrationServiceTest {

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);
  private static final UUID RUN_ID = UUID.randomUUID();
  private static final List<PearsScheduleEventType> EVENT_TYPES = List.of(
      new PearsScheduleEventType("T", "Term", -3),
      new PearsScheduleEventType("E", "Licence Expiry Date", 1),
      new PearsScheduleEventType("BM", "Bespoke Milestone", 6)
  );

  @Mock
  private PearsScheduleService pearsScheduleService;

  @Mock
  private PearsScheduleWriter pearsScheduleWriter;

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  private PearsScheduleMigrationService service;

  private final Licence licence = LicenceTestUtil.builder().withId(2244).withLicenceReference("P2244").build();

  @BeforeEach
  void setUp() {
    service = new PearsScheduleMigrationService(
        pearsScheduleService, pearsScheduleWriter, licenceService, licenceScheduleService,
        TransactionOperations.withoutTransaction());
  }

  @Test
  void licencesToMigrate_returnsTheLicencesWithAPearsSchedule() {
    when(pearsScheduleService.scheduleLicenceReferences()).thenReturn(List.of(P2244));

    assertThat(service.licencesToMigrate()).containsExactly(P2244);
    verify(pearsScheduleService).checkReadable();
  }

  @Test
  void licencesToMigrate_whenPearsHoldsNoSchedules_thenThrows() {
    when(pearsScheduleService.scheduleLicenceReferences()).thenReturn(List.of());

    assertThatThrownBy(() -> service.licencesToMigrate()).isInstanceOf(MigrationPreconditionException.class);
  }

  @Test
  void migrateLicence_notInLms_isSkipped() {
    when(licenceService.findLicenceByReference("P2244")).thenReturn(Optional.empty());

    var outcome = service.migrateLicence(P2244, EVENT_TYPES, RUN_ID);

    assertThat(outcome).extracting(PearsScheduleMigrationOutcome::result, PearsScheduleMigrationOutcome::reason)
        .containsExactly(Result.SKIPPED, PearsScheduleMigrationService.NOT_IN_LMS);
    verifyNoInteractions(pearsScheduleService, pearsScheduleWriter);
  }

  @Test
  void migrateLicence_alreadyHasASchedule_isSkippedWithoutReadingPears() {
    when(licenceService.findLicenceByReference("P2244")).thenReturn(Optional.of(licence));
    when(licenceScheduleService.getIdsOfLicencesWithASchedule(List.of(licence))).thenReturn(Set.of(2244));

    var outcome = service.migrateLicence(P2244, EVENT_TYPES, RUN_ID);

    assertThat(outcome).extracting(PearsScheduleMigrationOutcome::result, PearsScheduleMigrationOutcome::reason)
        .containsExactly(Result.SKIPPED, PearsScheduleMigrationService.ALREADY_HAS_A_SCHEDULE);
    verifyNoInteractions(pearsScheduleService, pearsScheduleWriter);
  }

  @Test
  void migrateLicence_whenItCannotBeMapped_fails() {
    givenPearsSchedule(event("E", LocalDate.of(2040, Month.DECEMBER, 1)));

    var outcome = service.migrateLicence(P2244, EVENT_TYPES, RUN_ID);

    assertThat(outcome).extracting(PearsScheduleMigrationOutcome::result, PearsScheduleMigrationOutcome::reason)
        .containsExactly(Result.FAILED, PearsScheduleMappingException.NO_TERMS);
    verifyNoInteractions(pearsScheduleWriter);
  }

  @Test
  void migrateLicence_withAFourthTerm_isMigrated() {
    var pearsSchedule = givenPearsSchedule(
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        term("4", LocalDate.of(2040, Month.DECEMBER, 1)),
        event("E", LocalDate.of(2040, Month.DECEMBER, 1))
    );
    when(pearsScheduleWriter.write(eq(licence), any(), eq(pearsSchedule), eq(RUN_ID)))
        .thenReturn(Optional.of(new LicenceSchedule()));

    var outcome = service.migrateLicence(P2244, EVENT_TYPES, RUN_ID);

    assertThat(outcome.result()).isEqualTo(Result.MIGRATED);
  }

  @Test
  void migrateLicence_withABespokeMilestone_isSkippedUntilLmsHasTheCategory() {
    givenPearsSchedule(
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        event("BM", LocalDate.of(2016, Month.DECEMBER, 1)).withComments("Drill a well"),
        event("E", LocalDate.of(2018, Month.DECEMBER, 1))
    );

    var outcome = service.migrateLicence(P2244, EVENT_TYPES, RUN_ID);

    assertThat(outcome).extracting(PearsScheduleMigrationOutcome::result, PearsScheduleMigrationOutcome::reason)
        .containsExactly(Result.SKIPPED, PearsScheduleMigrationService.NEEDS_BESPOKE_MILESTONE);
    verifyNoInteractions(pearsScheduleWriter);
  }

  @Test
  void migrateLicence_whenTheWriteFails_failsWithTheReason() {
    var pearsSchedule = givenPearsSchedule(
        term("1", LocalDate.of(2018, Month.DECEMBER, 1)),
        event("E", LocalDate.of(2018, Month.DECEMBER, 1))
    );
    when(pearsScheduleWriter.write(eq(licence), any(), eq(pearsSchedule), eq(RUN_ID)))
        .thenThrow(new IllegalStateException("Constraint violated"));

    var outcome = service.migrateLicence(P2244, EVENT_TYPES, RUN_ID);

    assertThat(outcome).extracting(PearsScheduleMigrationOutcome::result, PearsScheduleMigrationOutcome::reason)
        .containsExactly(Result.FAILED, "Could not be written: Constraint violated");
  }

  @Test
  void migrateLicence_whenAnotherMigrationWroteItFirst_isSkipped() {
    var pearsSchedule = givenPearsSchedule(term("1", LocalDate.of(2018, Month.DECEMBER, 1)));
    when(pearsScheduleWriter.write(eq(licence), any(), eq(pearsSchedule), eq(RUN_ID))).thenReturn(Optional.empty());

    var outcome = service.migrateLicence(P2244, EVENT_TYPES, RUN_ID);

    assertThat(outcome).extracting(PearsScheduleMigrationOutcome::result, PearsScheduleMigrationOutcome::reason)
        .containsExactly(Result.SKIPPED, PearsScheduleMigrationService.ALREADY_HAS_A_SCHEDULE);
  }

  @Test
  void migrateLicence_writesTheMappedScheduleAndReturnsItsNotes() {
    // No expiry event, so the mapper notes one
    var pearsSchedule = givenPearsSchedule(term("1", LocalDate.of(2018, Month.DECEMBER, 1)));
    when(pearsScheduleWriter.write(eq(licence), any(), eq(pearsSchedule), eq(RUN_ID)))
        .thenReturn(Optional.of(new LicenceSchedule()));

    var outcome = service.migrateLicence(P2244, EVENT_TYPES, RUN_ID);

    assertThat(outcome.result()).isEqualTo(Result.MIGRATED);
    assertThat(outcome.reason()).isNull();
    assertThat(outcome.notes().noteCountsByReason()).containsEntry(PearsScheduleMapper.NO_EXPIRY, 1);
    verify(pearsScheduleWriter).write(eq(licence), any(MappedSchedule.class), eq(pearsSchedule), eq(RUN_ID));
  }

  private PearsSchedule givenPearsSchedule(PearsEventFixture... events) {
    when(licenceService.findLicenceByReference("P2244")).thenReturn(Optional.of(licence));
    when(licenceScheduleService.getIdsOfLicencesWithASchedule(List.of(licence))).thenReturn(Set.of());

    var version = new PearsScheduleVersion(1, LocalDateTime.of(2019, Month.JANUARY, 1, 9, 0), null,
        LocalDate.of(2014, Month.DECEMBER, 1), Stream.of(events).map(PearsEventFixture::build).toList());
    var pearsSchedule = new PearsSchedule(P2244, List.of(version));
    when(pearsScheduleService.licenceSchedule(P2244)).thenReturn(pearsSchedule);
    return pearsSchedule;
  }
}
