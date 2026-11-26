package uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigratedLicenceRepository;

/**
 * Migrates one licence's PEARS work programmes onto the schedule the schedule migration wrote for it (plan, design
 * section 3). PEARS is read and mapped before the transaction that writes LMS opens, and the licence is written in one
 * transaction of its own, so it is migrated in full or not at all.
 */
@Service
@ConditionalOnPearsDataSource
public class PearsWorkProgrammeMigrationService {

  static final String NOT_IN_LMS = "Not a licence in LMS";
  static final String NO_MIGRATED_SCHEDULE = "Has no schedule from the schedule migration";
  static final String ALREADY_MIGRATED = "Work programme already migrated";
  // A failure of the write rather than of PEARS's data: a bug, where the mapper's failures are data to fix in PEARS
  static final String COULD_NOT_BE_WRITTEN = "Could not be written: ";

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsWorkProgrammeMigrationService.class);

  private final PearsWorkProgrammeService pearsWorkProgrammeService;
  private final MigratedScheduleReader migratedScheduleReader;
  private final PearsWorkProgrammeWriter pearsWorkProgrammeWriter;
  private final PearsWorkProgrammeMigratedLicenceRepository workProgrammeMigratedLicenceRepository;
  private final PearsScheduleMigratedLicenceRepository scheduleMigratedLicenceRepository;
  private final LicenceService licenceService;
  private final TransactionOperations transactionOperations;

  PearsWorkProgrammeMigrationService(
      PearsWorkProgrammeService pearsWorkProgrammeService,
      MigratedScheduleReader migratedScheduleReader,
      PearsWorkProgrammeWriter pearsWorkProgrammeWriter,
      PearsWorkProgrammeMigratedLicenceRepository workProgrammeMigratedLicenceRepository,
      PearsScheduleMigratedLicenceRepository scheduleMigratedLicenceRepository,
      LicenceService licenceService,
      TransactionOperations transactionOperations
  ) {
    this.pearsWorkProgrammeService = pearsWorkProgrammeService;
    this.migratedScheduleReader = migratedScheduleReader;
    this.pearsWorkProgrammeWriter = pearsWorkProgrammeWriter;
    this.workProgrammeMigratedLicenceRepository = workProgrammeMigratedLicenceRepository;
    this.scheduleMigratedLicenceRepository = scheduleMigratedLicenceRepository;
    this.licenceService = licenceService;
    this.transactionOperations = transactionOperations;
  }

  /**
   * Checks PEARS can be read and holds work programmes, and that schedules have been migrated to put them on, so a run
   * in the wrong environment or order fails at the start rather than looking like a run with nothing to migrate.
   *
   * @return the licences with a PEARS work programme, to migrate one by one
   * @throws MigrationPreconditionException if PEARS can't be read or holds no work programmes, or no schedule has been
   *                                        migrated
   */
  public List<PearsLicenceReference> licencesToMigrate() {
    pearsWorkProgrammeService.checkReadable();
    var licences = pearsWorkProgrammeService.workProgrammeLicenceReferences();
    if (licences.isEmpty()) {
      throw new MigrationPreconditionException(
          "PEARS returned no work programmes. Check the PEARS datasource before migrating work programmes.");
    }
    if (scheduleMigratedLicenceRepository.count() == 0) {
      throw new MigrationPreconditionException(
          "No schedule has been migrated from PEARS. Migrate schedules before work programmes.");
    }
    return licences;
  }

  /**
   * Migrates one licence's work programmes.
   *
   * @param runId the migration run, recorded against the licence
   */
  public PearsWorkProgrammeMigrationOutcome migrateLicence(PearsLicenceReference licenceReference, UUID runId) {
    var licence = licenceService.findLicenceByReference(licenceReference.licenceReference());
    if (licence.isEmpty()) {
      return PearsWorkProgrammeMigrationOutcome.skipped(licenceReference, NOT_IN_LMS, new MigrationNotes());
    }
    var active = migratedScheduleReader.read(licence.get());
    if (active.isEmpty()) {
      return PearsWorkProgrammeMigrationOutcome.skipped(licenceReference, NO_MIGRATED_SCHEDULE, new MigrationNotes());
    }
    if (workProgrammeMigratedLicenceRepository.existsById(active.get().detail().getLicenceSchedule().getId())) {
      return PearsWorkProgrammeMigrationOutcome.skipped(licenceReference, ALREADY_MIGRATED, new MigrationNotes());
    }

    var workProgrammes = pearsWorkProgrammeService.licenceWorkProgrammes(licenceReference);
    MappedWorkProgramme mappedWorkProgramme;
    try {
      mappedWorkProgramme = PearsWorkProgrammeMapper.map(licenceReference, workProgrammes, active.get().schedule());
    } catch (PearsWorkProgrammeMappingException e) {
      // Two INITIAL programmes wait on the licensing team (WP-L2); the rest are data to fix in PEARS
      if (PearsWorkProgrammeMapper.TWO_INITIAL_PROGRAMMES.equals(e.reason())) {
        return PearsWorkProgrammeMigrationOutcome.skipped(licenceReference, e.reason(), new MigrationNotes());
      }
      return PearsWorkProgrammeMigrationOutcome.failed(licenceReference, e.reason(), new MigrationNotes());
    }

    Boolean written;
    try {
      written = transactionOperations.execute(
          status -> pearsWorkProgrammeWriter.write(licence.get(), mappedWorkProgramme, runId));
    } catch (RuntimeException e) {
      LOGGER.warn("Could not write the work programme of {}", licenceReference, e);
      return PearsWorkProgrammeMigrationOutcome.failed(
          licenceReference, COULD_NOT_BE_WRITTEN + e.getMessage(), mappedWorkProgramme.notes());
    }

    // Not written when another migration of the licence wrote it after the check above
    if (!Boolean.TRUE.equals(written)) {
      return PearsWorkProgrammeMigrationOutcome.skipped(licenceReference, ALREADY_MIGRATED, mappedWorkProgramme.notes());
    }
    return PearsWorkProgrammeMigrationOutcome.migrated(licenceReference, mappedWorkProgramme.notes());
  }
}
