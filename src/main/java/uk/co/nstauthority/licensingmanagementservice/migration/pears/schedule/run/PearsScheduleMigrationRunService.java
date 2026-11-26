package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.migration.MigrationPreconditionException;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleMigratedLicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.PearsScheduleValidationService;

/**
 * The runs of the PEARS schedule migration and the licences queued in each (plan, design section 4). The worker takes
 * licences from the oldest active run a batch at a time, and only one run is active at a time.
 */
@Service
public class PearsScheduleMigrationRunService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsScheduleMigrationRunService.class);

  private static final Set<PearsScheduleMigrationRunStatus> ACTIVE =
      EnumSet.of(PearsScheduleMigrationRunStatus.QUEUED, PearsScheduleMigrationRunStatus.RUNNING);

  private static final String ONE_ACTIVE_RUN_INDEX = "pears_schedule_migration_runs_one_active_idx";

  private final PearsScheduleMigrationRunRepository runRepository;
  private final PearsScheduleMigrationQueueEntryRepository queueEntryRepository;
  private final PearsScheduleMigratedLicenceRepository migratedLicenceRepository;
  private final EntityManager entityManager;
  private final PearsMigrationWorkerSignal workerSignal;
  private final Clock clock;

  PearsScheduleMigrationRunService(
      PearsScheduleMigrationRunRepository runRepository,
      PearsScheduleMigrationQueueEntryRepository queueEntryRepository,
      PearsScheduleMigratedLicenceRepository migratedLicenceRepository,
      EntityManager entityManager,
      PearsMigrationWorkerSignal workerSignal,
      Clock clock
  ) {
    this.runRepository = runRepository;
    this.queueEntryRepository = queueEntryRepository;
    this.migratedLicenceRepository = migratedLicenceRepository;
    this.entityManager = entityManager;
    this.workerSignal = workerSignal;
    this.clock = clock;
  }

  /**
   * Queues a run migrating the licences given, which the worker then picks up.
   *
   * @throws MigrationPreconditionException if another run is still queued or running
   */
  @Transactional
  public PearsScheduleMigrationRun startRun(List<String> licenceReferences, long requestedBy) {
    return start(PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.MIGRATE, licenceReferences, requestedBy);
  }

  /**
   * Queues a run clearing down the migrated schedules of the licences given, which the worker then picks up.
   *
   * @throws MigrationPreconditionException if another run is still queued or running
   */
  @Transactional
  public PearsScheduleMigrationRun startCleardown(List<String> licenceReferences, long requestedBy) {
    return start(PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.CLEARDOWN, licenceReferences, requestedBy);
  }

  /**
   * Queues a dry run of the licences given: each is migrated and compared with PEARS, then rolled back.
   *
   * @throws MigrationPreconditionException if another run is still queued or running
   */
  @Transactional
  public PearsScheduleMigrationRun startValidation(List<String> licenceReferences, long requestedBy) {
    return start(PearsMigration.SCHEDULE, PearsScheduleMigrationRunKind.VALIDATE, licenceReferences, requestedBy);
  }

  /**
   * Queues a run migrating the work programmes of the licences given onto their migrated schedules.
   *
   * @throws MigrationPreconditionException if another run is still queued or running
   */
  @Transactional
  public PearsScheduleMigrationRun startWorkProgrammeRun(List<String> licenceReferences, long requestedBy) {
    return start(PearsMigration.WORK_PROGRAMME, PearsScheduleMigrationRunKind.MIGRATE, licenceReferences, requestedBy);
  }

  /**
   * Queues a run clearing down the migrated work programmes of the licences given, leaving their schedules.
   *
   * @throws MigrationPreconditionException if another run is still queued or running
   */
  @Transactional
  public PearsScheduleMigrationRun startWorkProgrammeCleardown(List<String> licenceReferences, long requestedBy) {
    return start(PearsMigration.WORK_PROGRAMME, PearsScheduleMigrationRunKind.CLEARDOWN, licenceReferences,
        requestedBy);
  }

  /**
   * Queues a dry run of the work programmes of the licences given: each is migrated onto its migrated schedule and
   * compared with PEARS, then rolled back.
   *
   * @throws MigrationPreconditionException if another run is still queued or running
   */
  @Transactional
  public PearsScheduleMigrationRun startWorkProgrammeValidation(List<String> licenceReferences, long requestedBy) {
    return start(PearsMigration.WORK_PROGRAMME, PearsScheduleMigrationRunKind.VALIDATE, licenceReferences,
        requestedBy);
  }

  private PearsScheduleMigrationRun start(
      PearsMigration migration,
      PearsScheduleMigrationRunKind kind,
      List<String> licenceReferences,
      long requestedBy
  ) {
    failIfARunIsActive();

    var run = new PearsScheduleMigrationRun(UUID.randomUUID(), migration, kind, requestedBy, clock.instant());
    // Persisted rather than saved, as saving an entity whose id is already set looks it up first
    entityManager.persist(run);
    flushOrFailIfAnotherRunIsActive();
    licenceReferences.stream()
        .distinct()
        .map(licenceReference -> new PearsScheduleMigrationQueueEntry(
            new PearsScheduleMigrationQueueEntryId(run.getId(), licenceReference)))
        .forEach(entityManager::persist);
    workerSignal.wakeAfterCommit();

    LOGGER.info("Queued PEARS {} {} run {} of {} licences", migration, kind, run.getId(), licenceReferences.size());
    return run;
  }

  /**
   * The next licences to work from the oldest active run, starting the run if it was queued. A run with nothing left
   * to work is completed, and there is no batch. A cleardown that leaves no migrated schedule also deletes every
   * earlier run, keeping only its own as a record of what was cleared.
   */
  @Transactional
  public Optional<Batch> nextBatch(int size) {
    // Locked, so a cancel waits for the run to be started rather than being overwritten by it
    var activeRun = runRepository.findOldestByStatusInForUpdate(ACTIVE);
    if (activeRun.isEmpty()) {
      return Optional.empty();
    }

    var run = activeRun.get();
    var pending = queueEntryRepository.findAllByIdRunIdAndStatusOrderByIdLicenceReference(
        run.getId(), PearsScheduleMigrationQueueStatus.PENDING, Limit.of(size));

    if (pending.isEmpty()) {
      run.finish(PearsScheduleMigrationRunStatus.COMPLETE, clock.instant());
      LOGGER.info("Completed PEARS {} {} run {}: {}", run.getMigration(), run.getKind(), run.getId(),
          licencesByStatus(run.getId()));
      if (run.getMigration() == PearsMigration.SCHEDULE
          && run.getKind() == PearsScheduleMigrationRunKind.CLEARDOWN
          && migratedLicenceRepository.count() == 0) {
        deleteEveryRunExcept(run);
      }
      return Optional.empty();
    }

    if (run.getStatus() == PearsScheduleMigrationRunStatus.QUEUED) {
      run.start();
    }
    return Optional.of(new Batch(
        run.getId(),
        run.getMigration(),
        run.getKind(),
        pending.stream().map(entry -> entry.getId().licenceReference()).toList()
    ));
  }

  private void deleteEveryRunExcept(PearsScheduleMigrationRun run) {
    var queueEntries = queueEntryRepository.deleteAllNotOfRun(run.getId());
    var runs = runRepository.deleteAllExcept(run.getId());
    LOGGER.info("No migrated schedules are left, so deleted {} earlier runs and their {} licences", runs, queueEntries);
  }

  public boolean isActive(UUID runId) {
    return runRepository.findById(runId)
        .map(run -> run.getStatus().isActive())
        .orElse(false);
  }

  /**
   * Records what became of a licence, unless it already has an outcome. It only has one when another worker got to it
   * first, and then the first outcome stands.
   *
   * @return whether the outcome was recorded
   */
  @Transactional
  public boolean recordOutcome(
      UUID runId,
      String licenceReference,
      PearsScheduleMigrationQueueStatus result,
      String outcome,
      Map<String, Integer> notes,
      Instant startedAt
  ) {
    var entry = queueEntryRepository.findByIdForUpdate(new PearsScheduleMigrationQueueEntryId(runId, licenceReference))
        .orElseThrow(() -> new IllegalStateException(
            "%s is not queued in PEARS schedule migration run %s".formatted(licenceReference, runId)));

    if (entry.getStatus() != PearsScheduleMigrationQueueStatus.PENDING) {
      LOGGER.warn("{} in run {} was already {}, so {} was not recorded",
          licenceReference, runId, entry.getStatus(), result);
      return false;
    }

    entry.recordOutcome(result, outcome, notes.isEmpty() ? null : notes, startedAt, clock.instant());
    return true;
  }

  /**
   * Cancels a run. The worker stops taking its licences, and finishes the one it is on.
   *
   * @throws MigrationPreconditionException if the run has already finished
   */
  @Transactional
  public Optional<PearsScheduleMigrationRunReport> cancel(UUID runId) {
    var foundRun = runRepository.findByIdForUpdate(runId);
    if (foundRun.isEmpty()) {
      return Optional.empty();
    }

    var run = foundRun.get();
    if (!run.getStatus().isActive()) {
      throw new MigrationPreconditionException("Run %s is already %s".formatted(runId, run.getStatus()));
    }
    run.finish(PearsScheduleMigrationRunStatus.CANCELLED, clock.instant());
    LOGGER.info("Cancelled PEARS schedule migration run {}", runId);
    return Optional.of(report(run));
  }

  /**
   * Queues a run's failed licences again, for example once their data is fixed in PEARS, and reopens the run if it had
   * completed.
   *
   * @throws MigrationPreconditionException if the run was cancelled, or it had completed and another run is active
   */
  @Transactional
  public Optional<PearsScheduleMigrationRunReport> retryFailed(UUID runId) {
    var foundRun = runRepository.findByIdForUpdate(runId);
    if (foundRun.isEmpty()) {
      return Optional.empty();
    }

    var run = foundRun.get();
    if (run.getStatus() == PearsScheduleMigrationRunStatus.CANCELLED) {
      throw new MigrationPreconditionException("Run %s was cancelled, so start a new run instead".formatted(runId));
    }

    var failed = queueEntryRepository.countByIdRunIdAndStatus(runId, PearsScheduleMigrationQueueStatus.FAILED);
    if (failed > 0 && run.getStatus() == PearsScheduleMigrationRunStatus.COMPLETE) {
      failIfARunIsActive();
      run.reopen();
      flushOrFailIfAnotherRunIsActive();
    }
    queueEntryRepository.requeue(
        runId, PearsScheduleMigrationQueueStatus.FAILED, PearsScheduleMigrationQueueStatus.PENDING);
    workerSignal.wakeAfterCommit();

    LOGGER.info("Queued the {} failed licences of PEARS schedule migration run {} again", failed, runId);
    return Optional.of(report(run));
  }

  /**
   * Carries on a run left queued or running when the app restarted, as the worker doesn't pick one up by itself.
   *
   * @throws MigrationPreconditionException if the run has already finished
   */
  @Transactional
  public Optional<PearsScheduleMigrationRunReport> resume(UUID runId) {
    var foundRun = runRepository.findById(runId);
    if (foundRun.isEmpty()) {
      return Optional.empty();
    }

    var run = foundRun.get();
    if (!run.getStatus().isActive()) {
      throw new MigrationPreconditionException("Run %s is already %s".formatted(runId, run.getStatus()));
    }
    workerSignal.wakeAfterCommit();
    LOGGER.info("Resumed PEARS schedule migration run {}", runId);
    return Optional.of(report(run));
  }

  @Transactional
  public Optional<PearsScheduleMigrationRunReport> report(UUID runId) {
    return runRepository.findById(runId).map(this::report);
  }

  /**
   * Every licence of a run and what became of it, in licence order; empty for an unknown run.
   */
  @Transactional
  public Optional<List<RunLicence>> licences(UUID runId) {
    if (!runRepository.existsById(runId)) {
      return Optional.empty();
    }
    return Optional.of(queueEntryRepository.findAllByIdRunIdOrderByIdLicenceReference(runId).stream()
        .map(entry -> new RunLicence(
            entry.getId().licenceReference(),
            entry.getStatus(),
            entry.getAttempts(),
            entry.getOutcome(),
            entry.getNotes() == null ? Map.of() : Map.copyOf(entry.getNotes())
        ))
        .toList());
  }

  private PearsScheduleMigrationRunReport report(PearsScheduleMigrationRun run) {
    return new PearsScheduleMigrationRunReport(
        run.getId(),
        run.getMigration(),
        run.getKind(),
        run.getStatus(),
        run.getRequestedBy(),
        run.getRequestedAt(),
        run.getCompletedAt(),
        licencesByStatus(run.getId()),
        outcomes(run.getId(), PearsScheduleMigrationQueueStatus.FAILED),
        outcomes(run.getId(), PearsScheduleMigrationQueueStatus.SKIPPED),
        outcomes(run.getId(), PearsScheduleMigrationQueueStatus.DIFFERS),
        comparisonResults(run)
    );
  }

  /**
   * For a dry run, how many comparison rows had each result across the licences compared; otherwise empty.
   */
  private Map<String, Long> comparisonResults(PearsScheduleMigrationRun run) {
    var results = new TreeMap<String, Long>();
    if (run.getKind() != PearsScheduleMigrationRunKind.VALIDATE) {
      return results;
    }
    Stream.of(PearsScheduleMigrationQueueStatus.MATCHED, PearsScheduleMigrationQueueStatus.DIFFERS)
        .flatMap(status -> queueEntryRepository.findAllByIdRunIdAndStatusOrderByIdLicenceReference(run.getId(), status)
            .stream())
        .filter(entry -> entry.getNotes() != null)
        .forEach(entry -> entry.getNotes().forEach((note, count) -> {
          if (note.startsWith(PearsScheduleValidationService.RESULT_NOTE_PREFIX)) {
            results.merge(note.substring(PearsScheduleValidationService.RESULT_NOTE_PREFIX.length()), (long) count,
                Long::sum);
          }
        }));
    return results;
  }

  private Map<PearsScheduleMigrationQueueStatus, Long> licencesByStatus(UUID runId) {
    var licencesByStatus = new EnumMap<PearsScheduleMigrationQueueStatus, Long>(PearsScheduleMigrationQueueStatus.class);
    Arrays.stream(PearsScheduleMigrationQueueStatus.values())
        .forEach(status -> licencesByStatus.put(status, queueEntryRepository.countByIdRunIdAndStatus(runId, status)));
    return licencesByStatus;
  }

  private List<PearsScheduleMigrationRunReport.LicenceOutcome> outcomes(
      UUID runId,
      PearsScheduleMigrationQueueStatus status
  ) {
    return queueEntryRepository.findAllByIdRunIdAndStatusOrderByIdLicenceReference(runId, status).stream()
        .map(entry -> new PearsScheduleMigrationRunReport.LicenceOutcome(
            entry.getId().licenceReference(), entry.getOutcome(), entry.getAttempts()))
        .toList();
  }

  private void failIfARunIsActive() {
    runRepository.findFirstByStatusInOrderByRequestedAt(ACTIVE).ifPresent(activeRun -> {
      throw new MigrationPreconditionException(
          "Run %s is still %s. Wait for it to finish or cancel it first, or resume it if the app has restarted since."
              .formatted(activeRun.getId(), activeRun.getStatus()));
    });
  }

  /**
   * The check in {@link #failIfARunIsActive()} can't see a run another request has yet to commit, so two runs made
   * active at once are caught by the unique index on active runs instead.
   */
  private void flushOrFailIfAnotherRunIsActive() {
    try {
      entityManager.flush();
    } catch (ConstraintViolationException e) {
      if (ONE_ACTIVE_RUN_INDEX.equals(e.getConstraintName())) {
        throw new MigrationPreconditionException(
            "Another run was made active at the same time. Wait for it to finish or cancel it first.");
      }
      throw e;
    }
  }

  /**
   * One licence of a run.
   *
   * @param outcome why it was skipped or failed, or for a dry run its issues; null otherwise
   * @param notes   the mapper's notes, and for a dry run the comparison result counts, counted by reason
   */
  public record RunLicence(
      String licenceReference,
      PearsScheduleMigrationQueueStatus status,
      int attempts,
      @Nullable String outcome,
      Map<String, Integer> notes
  ) {
  }

  public record Batch(
      UUID runId,
      PearsMigration migration,
      PearsScheduleMigrationRunKind kind,
      List<String> licenceReferences
  ) {
  }
}
