package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.operation.MigrationNotes;

/**
 * Migrates one licence's PEARS schedule, with its full history, into LMS (plan, design section 3). PEARS is read and
 * mapped before the transaction that writes LMS opens, so a licence waiting on PEARS holds no LMS connection, and the
 * licence is written in one transaction of its own: it is migrated in full or not at all, and re-running picks up only
 * what didn't migrate. The background worker calls it once per licence.
 */
@Service
@ConditionalOnPearsDataSource
public class PearsScheduleMigrationService {

  static final String NOT_IN_LMS = "Not a licence in LMS";
  static final String ALREADY_HAS_A_SCHEDULE = "Already has a schedule in LMS";
  // A failure of the write rather than of PEARS's data: a bug, where the mapper's failures are data to fix in PEARS
  static final String COULD_NOT_BE_WRITTEN = "Could not be written: ";
  static final String NEEDS_BESPOKE_MILESTONE =
      "Has a bespoke milestone, which LMS can't hold until the BESPOKE_MILESTONE category is added";

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsScheduleMigrationService.class);

  private final PearsScheduleService pearsScheduleService;
  private final PearsScheduleMapper pearsScheduleMapper;
  private final PearsScheduleWriter pearsScheduleWriter;
  private final LicenceService licenceService;
  private final LicenceScheduleService licenceScheduleService;
  private final TransactionOperations transactionOperations;

  PearsScheduleMigrationService(
      PearsScheduleService pearsScheduleService,
      PearsScheduleWriter pearsScheduleWriter,
      LicenceService licenceService,
      LicenceScheduleService licenceScheduleService,
      TransactionOperations transactionOperations
  ) {
    this.pearsScheduleService = pearsScheduleService;
    this.pearsScheduleMapper = new PearsScheduleMapper();
    this.pearsScheduleWriter = pearsScheduleWriter;
    this.licenceService = licenceService;
    this.licenceScheduleService = licenceScheduleService;
    this.transactionOperations = transactionOperations;
  }

  /**
   * Checks PEARS can be read and holds schedules, so a run against the wrong datasource or without the grants fails at
   * the start rather than looking like a run with nothing to migrate.
   *
   * @return the licences with a PEARS schedule, to migrate one by one
   * @throws MigrationPreconditionException if PEARS can't be read or holds no schedules
   */
  public List<PearsLicenceReference> licencesToMigrate() {
    pearsScheduleService.checkReadable();
    var licences = pearsScheduleService.scheduleLicenceReferences();
    if (licences.isEmpty()) {
      throw new MigrationPreconditionException(
          "PEARS returned no licence schedules. Check the PEARS datasource before migrating schedules.");
    }
    return licences;
  }

  /**
   * Migrates one licence's schedule.
   *
   * @param eventTypes the PEARS event type catalogue, read once for the run
   * @param runId      the migration run, recorded against the licence for the cleardown
   */
  public PearsScheduleMigrationOutcome migrateLicence(
      PearsLicenceReference licenceReference,
      List<PearsScheduleEventType> eventTypes,
      UUID runId
  ) {
    var licence = licenceService.findLicenceByReference(licenceReference.licenceReference());
    if (licence.isEmpty()) {
      return PearsScheduleMigrationOutcome.skipped(licenceReference, NOT_IN_LMS, new MigrationNotes());
    }
    if (!licenceScheduleService.getIdsOfLicencesWithASchedule(List.of(licence.get())).isEmpty()) {
      return PearsScheduleMigrationOutcome.skipped(licenceReference, ALREADY_HAS_A_SCHEDULE, new MigrationNotes());
    }

    var pearsSchedule = pearsScheduleService.licenceSchedule(licenceReference);
    MappedSchedule mappedSchedule;
    try {
      mappedSchedule = pearsScheduleMapper.map(pearsSchedule, eventTypes);
    } catch (PearsScheduleMappingException e) {
      return PearsScheduleMigrationOutcome.failed(licenceReference, e.reason(), new MigrationNotes());
    }

    var unsupported = unsupportedReason(mappedSchedule);
    if (unsupported != null) {
      return PearsScheduleMigrationOutcome.skipped(licenceReference, unsupported, mappedSchedule.notes());
    }

    Boolean written;
    try {
      written = transactionOperations.execute(
          status -> pearsScheduleWriter.write(licence.get(), mappedSchedule, pearsSchedule, runId).isPresent());
    } catch (RuntimeException e) {
      LOGGER.warn("Could not write the schedule of {}", licenceReference, e);
      return PearsScheduleMigrationOutcome.failed(
          licenceReference, COULD_NOT_BE_WRITTEN + e.getMessage(), mappedSchedule.notes());
    }

    // Not written when another migration of the licence wrote its schedule after the check above
    if (!Boolean.TRUE.equals(written)) {
      return PearsScheduleMigrationOutcome.skipped(licenceReference, ALREADY_HAS_A_SCHEDULE, mappedSchedule.notes());
    }
    return PearsScheduleMigrationOutcome.migrated(licenceReference, mappedSchedule.notes());
  }

  /**
   * Why LMS can't hold the schedule yet, or null when it can. Bespoke milestones need an LMS change still to come
   * (plan, prerequisite A); until then those licences are skipped, and migrate on a later run.
   */
  @Nullable
  static String unsupportedReason(MappedSchedule mappedSchedule) {
    if (mappedSchedule.versions().stream().anyMatch(version -> version.otherEvents().stream()
        .anyMatch(event -> event.category() == MappedOtherEvent.Category.BESPOKE_MILESTONE))) {
      return NEEDS_BESPOKE_MILESTONE;
    }
    return null;
  }
}
