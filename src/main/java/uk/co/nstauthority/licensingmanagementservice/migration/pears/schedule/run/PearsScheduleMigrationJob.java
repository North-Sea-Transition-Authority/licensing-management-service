package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import net.javacrumbs.shedlock.core.LockExtender;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.ConditionalOnPearsDataSource;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.PearsLicenceReference;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleCleardownService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigrationService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationOutcome;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeCleardownService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeMigrationService;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.workprogramme.PearsWorkProgrammeValidationService;

/**
 * Works through the active run a batch of licences at a time, migrating, clearing down or dry running each in a
 * transaction of its own. A licence in flight when the instance stops is rolled back and stays pending, so the next
 * pass, on whichever instance, carries on from it.
 */
@Service
@Profile("migration")
@ConditionalOnPearsDataSource
public class PearsScheduleMigrationJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsScheduleMigrationJob.class);
  // As the scheduler lock's lockAtMostFor, renewed before each licence
  private static final Duration LOCK_HELD_FOR = Duration.ofMinutes(30);

  private final PearsScheduleMigrationRunService runService;
  private final PearsScheduleMigrationService migrationService;
  private final PearsScheduleService pearsScheduleService;
  private final PearsScheduleCleardownService cleardownService;
  private final PearsScheduleValidationService validationService;
  private final PearsWorkProgrammeMigrationService workProgrammeMigrationService;
  private final PearsWorkProgrammeCleardownService workProgrammeCleardownService;
  private final PearsWorkProgrammeValidationService workProgrammeValidationService;
  private final Clock clock;
  private final int batchSize;

  @SuppressWarnings("java:S107") // One collaborator per kind of work, for each migration
  PearsScheduleMigrationJob(
      PearsScheduleMigrationRunService runService,
      PearsScheduleMigrationService migrationService,
      PearsScheduleService pearsScheduleService,
      PearsScheduleCleardownService cleardownService,
      PearsScheduleValidationService validationService,
      PearsWorkProgrammeMigrationService workProgrammeMigrationService,
      PearsWorkProgrammeCleardownService workProgrammeCleardownService,
      PearsWorkProgrammeValidationService workProgrammeValidationService,
      Clock clock,
      @Value("${pears.schedule-migration.batch-size}") int batchSize
  ) {
    if (batchSize < 1) {
      throw new IllegalArgumentException("pears.schedule-migration.batch-size must be at least 1");
    }
    this.runService = runService;
    this.migrationService = migrationService;
    this.pearsScheduleService = pearsScheduleService;
    this.cleardownService = cleardownService;
    this.validationService = validationService;
    this.workProgrammeMigrationService = workProgrammeMigrationService;
    this.workProgrammeCleardownService = workProgrammeCleardownService;
    this.workProgrammeValidationService = workProgrammeValidationService;
    this.clock = clock;
    this.batchSize = batchSize;
  }

  @Scheduled(fixedDelayString = "${pears.schedule-migration.poll}")
  @SchedulerLock(name = "pearsScheduleMigration", lockAtMostFor = "PT30M")
  public void workNextBatch() {
    var foundBatch = runService.nextBatch(batchSize);
    if (foundBatch.isEmpty()) {
      return;
    }

    var batch = foundBatch.get();
    var work = work(batch);

    for (var licenceReference : batch.licenceReferences()) {
      if (!runService.isActive(batch.runId())) {
        LOGGER.info("PEARS schedule run {} is no longer active, so stopped working it", batch.runId());
        return;
      }
      if (!holdLock(batch.runId())) {
        return;
      }

      var startedAt = clock.instant();
      var outcome = work.apply(licenceReference);
      if (outcome.status() == PearsScheduleMigrationQueueStatus.PENDING) {
        LOGGER.info("Put off {} in PEARS schedule run {} until the next pass: {}",
            licenceReference, batch.runId(), outcome.reason());
        return;
      }
      // Not caught: an outcome that can't be saved leaves the licence pending, for the next pass to pick up
      runService.recordOutcome(
          batch.runId(), licenceReference, outcome.status(), outcome.reason(), outcome.notes(), startedAt);
    }
  }

  /**
   * Renews the scheduler lock, so it can't expire while a batch is worked and let another instance work the same
   * licences. False when the lock has been lost to another instance already, so this one stops.
   */
  private static boolean holdLock(UUID runId) {
    try {
      LockExtender.extendActiveLock(LOCK_HELD_FOR, Duration.ZERO);
      return true;
    } catch (LockExtender.NoActiveLockException e) {
      // Called directly rather than by the scheduler, so there is no lock to hold
      return true;
    } catch (LockExtender.LockCanNotBeExtendedException e) {
      LOGGER.warn("Lost the PEARS schedule migration lock to another instance, so stopped working run {}", runId);
      return false;
    }
  }

  private LicenceWork work(PearsScheduleMigrationRunService.Batch batch) {
    if (batch.migration() == PearsMigration.WORK_PROGRAMME) {
      return workProgrammeWork(batch);
    }
    return switch (batch.kind()) {
      case MIGRATE -> {
        var eventTypes = pearsScheduleService.eventTypes();
        yield licenceReference -> migrate(licenceReference, eventTypes, batch.runId());
      }
      case CLEARDOWN -> licenceReference -> clear(licenceReference, batch.runId(), cleardownService::clear);
      case VALIDATE -> {
        var eventTypes = pearsScheduleService.eventTypes();
        yield licenceReference -> validate(licenceReference, eventTypes, batch.runId());
      }
    };
  }

  private LicenceWork workProgrammeWork(PearsScheduleMigrationRunService.Batch batch) {
    return switch (batch.kind()) {
      case MIGRATE -> licenceReference -> migrateWorkProgramme(licenceReference, batch.runId());
      case CLEARDOWN ->
          licenceReference -> clear(licenceReference, batch.runId(), workProgrammeCleardownService::clear);
      case VALIDATE -> licenceReference -> validated(licenceReference, batch.runId(),
          () -> workProgrammeValidationService.validate(licenceReference, batch.runId()));
    };
  }

  private Outcome migrate(String licenceReference, List<PearsScheduleEventType> eventTypes, UUID runId) {
    return migrated(licenceReference, runId, () -> {
      var outcome = migrationService.migrateLicence(PearsLicenceReference.parse(licenceReference), eventTypes, runId);
      return new Outcome(PearsScheduleMigrationQueueStatus.valueOf(outcome.result().name()), outcome.reason(),
          outcome.notes().noteCountsByReason());
    });
  }

  private Outcome migrateWorkProgramme(String licenceReference, UUID runId) {
    return migrated(licenceReference, runId, () -> {
      var outcome = workProgrammeMigrationService.migrateLicence(PearsLicenceReference.parse(licenceReference), runId);
      return new Outcome(PearsScheduleMigrationQueueStatus.valueOf(outcome.result().name()), outcome.reason(),
          outcome.notes().noteCountsByReason());
    });
  }

  private static Outcome migrated(String licenceReference, UUID runId, Supplier<Outcome> migration) {
    try {
      return migration.get();
    } catch (RuntimeException e) {
      LOGGER.warn("Could not migrate {} in run {}", licenceReference, runId, e);
      return new Outcome(
          PearsScheduleMigrationQueueStatus.FAILED, "Could not be migrated: %s".formatted(e.getMessage()), Map.of());
    }
  }

  private Outcome clear(
      String licenceReference,
      UUID runId,
      Function<String, PearsScheduleCleardownOutcome> cleardown
  ) {
    try {
      var outcome = cleardown.apply(licenceReference);
      var status = outcome.result() == PearsScheduleCleardownOutcome.Result.POSTPONED
          ? PearsScheduleMigrationQueueStatus.PENDING
          : PearsScheduleMigrationQueueStatus.valueOf(outcome.result().name());
      return new Outcome(status, outcome.reason(), Map.of());
    } catch (RuntimeException e) {
      LOGGER.warn("Could not clear {} in run {}", licenceReference, runId, e);
      return new Outcome(
          PearsScheduleMigrationQueueStatus.FAILED, "Could not be cleared: %s".formatted(e.getMessage()), Map.of());
    }
  }

  private Outcome validate(String licenceReference, List<PearsScheduleEventType> eventTypes, UUID runId) {
    return validated(licenceReference, runId, () -> validationService.validate(licenceReference, eventTypes, runId));
  }

  private static Outcome validated(
      String licenceReference,
      UUID runId,
      Supplier<PearsScheduleValidationOutcome> dryRun
  ) {
    try {
      var outcome = dryRun.get();
      return new Outcome(
          PearsScheduleMigrationQueueStatus.valueOf(outcome.result().name()), outcome.reason(), outcome.notes());
    } catch (RuntimeException e) {
      LOGGER.warn("Could not dry run {} in run {}", licenceReference, runId, e);
      return new Outcome(
          PearsScheduleMigrationQueueStatus.FAILED, "Could not be dry run: %s".formatted(e.getMessage()), Map.of());
    }
  }

  @FunctionalInterface
  private interface LicenceWork {
    Outcome apply(String licenceReference);
  }

  private record Outcome(PearsScheduleMigrationQueueStatus status, String reason, Map<String, Integer> notes) {
  }
}
