package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigratedLicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeMigrationOutcome.Result;

@ExtendWith(MockitoExtension.class)
class PearsWorkProgrammeMigrationServiceTest {

  private static final PearsLicenceReference P2244 = new PearsLicenceReference("P", 2244);
  private static final LocalDateTime SAVED = LocalDateTime.of(2015, Month.MARCH, 2, 10, 0);
  private static final UUID RUN_ID = UUID.randomUUID();

  @Mock
  private PearsWorkProgrammeService pearsWorkProgrammeService;

  @Mock
  private MigratedScheduleReader migratedScheduleReader;

  @Mock
  private PearsWorkProgrammeWriter pearsWorkProgrammeWriter;

  @Mock
  private PearsWorkProgrammeMigratedLicenceRepository workProgrammeMigratedLicenceRepository;

  @Mock
  private PearsScheduleMigratedLicenceRepository scheduleMigratedLicenceRepository;

  @Mock
  private LicenceService licenceService;

  private PearsWorkProgrammeMigrationService service;

  private Licence licence;

  private ActiveScheduleDetail active;

  @BeforeEach
  void setUp() {
    service = new PearsWorkProgrammeMigrationService(pearsWorkProgrammeService, migratedScheduleReader,
        pearsWorkProgrammeWriter, workProgrammeMigratedLicenceRepository, scheduleMigratedLicenceRepository,
        licenceService, TransactionOperations.withoutTransaction());

    licence = LicenceTestUtil.builder().withLicenceReference("P2244").build();
    var detail = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence));
    var start = LocalDate.of(2014, Month.DECEMBER, 1);
    active = new ActiveScheduleDetail(detail, List.of(), List.of(), new MigratedSchedule(start,
        List.of(new MigratedSchedule.Term(TermType.INITIAL, start, LocalDate.of(2018, Month.NOVEMBER, 30))),
        List.of(), List.of()));
  }

  @Test
  void licencesToMigrate_checksPearsAndReturnsTheLicences() {
    when(pearsWorkProgrammeService.workProgrammeLicenceReferences()).thenReturn(List.of(P2244));
    when(scheduleMigratedLicenceRepository.count()).thenReturn(1L);

    assertThat(service.licencesToMigrate()).containsExactly(P2244);
    verify(pearsWorkProgrammeService).checkReadable();
  }

  @Test
  void licencesToMigrate_whenPearsHasNoWorkProgrammes_throws() {
    when(pearsWorkProgrammeService.workProgrammeLicenceReferences()).thenReturn(List.of());

    assertThatThrownBy(() -> service.licencesToMigrate())
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessageContaining("PEARS returned no work programmes");
  }

  @Test
  void licencesToMigrate_whenNoScheduleHasBeenMigrated_throws() {
    when(pearsWorkProgrammeService.workProgrammeLicenceReferences()).thenReturn(List.of(P2244));
    when(scheduleMigratedLicenceRepository.count()).thenReturn(0L);

    assertThatThrownBy(() -> service.licencesToMigrate())
        .isInstanceOf(MigrationPreconditionException.class)
        .hasMessageContaining("Migrate schedules before work programmes");
  }

  @Test
  void migrateLicence_whenNotInLms_skips() {
    when(licenceService.findLicenceByReference("P2244")).thenReturn(Optional.empty());

    assertThat(service.migrateLicence(P2244, RUN_ID))
        .extracting(PearsWorkProgrammeMigrationOutcome::result, PearsWorkProgrammeMigrationOutcome::reason)
        .containsExactly(Result.SKIPPED, PearsWorkProgrammeMigrationService.NOT_IN_LMS);
  }

  @Test
  void migrateLicence_whenTheScheduleWasNotMigrated_skipsWithoutReadingPears() {
    when(licenceService.findLicenceByReference("P2244")).thenReturn(Optional.of(licence));
    when(migratedScheduleReader.read(licence)).thenReturn(Optional.empty());

    assertThat(service.migrateLicence(P2244, RUN_ID))
        .extracting(PearsWorkProgrammeMigrationOutcome::result, PearsWorkProgrammeMigrationOutcome::reason)
        .containsExactly(Result.SKIPPED, PearsWorkProgrammeMigrationService.NO_MIGRATED_SCHEDULE);
    verify(pearsWorkProgrammeService, never()).licenceWorkProgrammes(any());
  }

  @Test
  void migrateLicence_whenAlreadyMigrated_skipsWithoutReadingPears() {
    givenAMigratedSchedule();
    when(workProgrammeMigratedLicenceRepository.existsById(active.detail().getLicenceSchedule().getId()))
        .thenReturn(true);

    assertThat(service.migrateLicence(P2244, RUN_ID))
        .extracting(PearsWorkProgrammeMigrationOutcome::result, PearsWorkProgrammeMigrationOutcome::reason)
        .containsExactly(Result.SKIPPED, PearsWorkProgrammeMigrationService.ALREADY_MIGRATED);
    verify(pearsWorkProgrammeService, never()).licenceWorkProgrammes(any());
  }

  @Test
  void migrateLicence_whenTheLicenceHasTwoInitialProgrammes_skips() {
    givenAMigratedSchedule();
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P2244))
        .thenReturn(List.of(programme(11L, "OTHER"), programme(12L, "OTHER")));

    assertThat(service.migrateLicence(P2244, RUN_ID))
        .extracting(PearsWorkProgrammeMigrationOutcome::result, PearsWorkProgrammeMigrationOutcome::reason)
        .containsExactly(Result.SKIPPED, PearsWorkProgrammeMapper.TWO_INITIAL_PROGRAMMES);
    verify(pearsWorkProgrammeWriter, never()).write(any(), any(), any());
  }

  @Test
  void migrateLicence_whenPearsHoldsWhatTheMapperCannotMap_fails() {
    givenAMigratedSchedule();
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P2244)).thenReturn(List.of(programme(11L, "MYSTERY")));

    var outcome = service.migrateLicence(P2244, RUN_ID);

    assertThat(outcome.result()).isEqualTo(Result.FAILED);
    assertThat(outcome.reason()).contains("MYSTERY");
    verify(pearsWorkProgrammeWriter, never()).write(any(), any(), any());
  }

  @Test
  void migrateLicence_whenTheWriteFails_failsWithTheReason() {
    givenAMigratedSchedule();
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P2244)).thenReturn(List.of(programme(11L, "OTHER")));
    doThrow(new IllegalStateException("broken")).when(pearsWorkProgrammeWriter).write(eq(licence), any(), eq(RUN_ID));

    assertThat(service.migrateLicence(P2244, RUN_ID))
        .extracting(PearsWorkProgrammeMigrationOutcome::result, PearsWorkProgrammeMigrationOutcome::reason)
        .containsExactly(Result.FAILED, PearsWorkProgrammeMigrationService.COULD_NOT_BE_WRITTEN + "broken");
  }

  @Test
  void migrateLicence_whenAnotherMigrationWroteItFirst_skips() {
    givenAMigratedSchedule();
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P2244)).thenReturn(List.of(programme(11L, "OTHER")));
    when(pearsWorkProgrammeWriter.write(eq(licence), any(), eq(RUN_ID))).thenReturn(false);

    assertThat(service.migrateLicence(P2244, RUN_ID))
        .extracting(PearsWorkProgrammeMigrationOutcome::result, PearsWorkProgrammeMigrationOutcome::reason)
        .containsExactly(Result.SKIPPED, PearsWorkProgrammeMigrationService.ALREADY_MIGRATED);
  }

  @Test
  void migrateLicence_writesTheMappedWorkProgramme() {
    givenAMigratedSchedule();
    when(pearsWorkProgrammeService.licenceWorkProgrammes(P2244)).thenReturn(List.of(programme(11L, "OTHER")));
    when(pearsWorkProgrammeWriter.write(eq(licence), any(), eq(RUN_ID))).thenReturn(true);

    var outcome = service.migrateLicence(P2244, RUN_ID);

    assertThat(outcome.result()).isEqualTo(Result.MIGRATED);
    assertThat(outcome.reason()).isNull();
    verify(pearsWorkProgrammeWriter).write(eq(licence),
        argThat(mapped -> mapped.activities().size() == 1), eq(RUN_ID));
  }

  private void givenAMigratedSchedule() {
    when(licenceService.findLicenceByReference("P2244")).thenReturn(Optional.of(licence));
    when(migratedScheduleReader.read(licence)).thenReturn(Optional.of(active));
  }

  private static PearsWorkProgramme programme(long pedWpId, String activityType) {
    return new PearsWorkProgramme(P2244, pedWpId, pedWpId * 10, SAVED, "INITIAL", null, "ACTIVE", null,
        List.of(new PearsActivity(1, PearsActivity.ListKind.FLAT, null, "701", activityType, null, "NOT_STARTED", null,
            "Studies", SAVED)),
        List.of());
  }
}
