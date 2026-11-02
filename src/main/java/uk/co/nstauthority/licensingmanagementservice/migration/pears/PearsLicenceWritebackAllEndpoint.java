package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;

/**
 * Replays every licence's PEARS history into this application in one go, as
 * {@link PearsLicenceWritebackEndpoint} does for one. Only registered where the PEARS datasource is
 * configured.
 */
@Component
@Endpoint(id = "pears-licence-writeback-all")
@ConditionalOnPearsDataSource
class PearsLicenceWritebackAllEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsLicenceWritebackAllEndpoint.class);

  private final PearsLicenceService pearsLicenceService;
  private final LicenceService licenceService;
  private final LicenceWritebackService licenceWritebackService;
  private final int parallelism;
  private final int writeParallelism;

  /**
   * Builds the endpoint, refusing a parallelism under one.
   *
   * @param parallelism      how many licences are in hand at once, each holding a connection to PEARS
   *                         while it is read, so this stays below the PEARS pool
   * @param writeParallelism how many of those are written at once, each holding a connection to this
   *                         application's database while it is written, so this stays well below its
   *                         pool, leaving room for everything else
   */
  PearsLicenceWritebackAllEndpoint(
      PearsLicenceService pearsLicenceService,
      LicenceService licenceService,
      LicenceWritebackService licenceWritebackService,
      @Value("${pears.writeback.parallelism}") int parallelism,
      @Value("${pears.writeback.write-parallelism}") int writeParallelism
  ) {
    if (parallelism < 1 || writeParallelism < 1) {
      throw new IllegalArgumentException("pears.writeback.parallelism and pears.writeback.write-parallelism must be at least 1");
    }

    this.pearsLicenceService = pearsLicenceService;
    this.licenceService = licenceService;
    this.licenceWritebackService = licenceWritebackService;
    this.parallelism = parallelism;
    this.writeParallelism = writeParallelism;
  }

  /**
   * Deletes the positions of every licence PEARS holds and rebuilds them out of PEARS, several
   * licences at a time. Each licence is read from PEARS first and then written in a transaction of
   * its own, so one that fails is rolled back on its own and the rest carry on.
   */
  @WriteOperation(produces = MediaType.APPLICATION_JSON_VALUE)
  LicenceWritebackAllResult overwriteAllLicencePositionsFromPears() {
    var pearsLicences = pearsLicenceService.licenceReferences();
    LOGGER.info(
        "Writing back {} licences from PEARS, {} at a time and writing {} at a time",
        pearsLicences.size(),
        parallelism,
        writeParallelism
    );

    var permits = new Semaphore(parallelism);
    var writePermits = new Semaphore(writeParallelism);
    var completed = new AtomicInteger();
    List<Future<Outcome>> outcomes;

    // closing the executor waits for every licence to finish
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      outcomes = pearsLicences.stream()
          .map(PearsLicenceReference::licenceReference)
          .map(licenceReference -> executor.submit(() -> {
            permits.acquireUninterruptibly();
            try {
              return writeback(licenceReference, writePermits, completed, pearsLicences.size());
            } finally {
              permits.release();
            }
          }))
          .toList();
    }

    return summarise(outcomes.stream().map(Future::resultNow).toList());
  }

  private Outcome writeback(String licenceReference, Semaphore writePermits, AtomicInteger completed, int total) {
    var licence = licenceService.findLicenceByReference(licenceReference);
    if (licence.isEmpty()) {
      LOGGER.warn(
          "Did not write back {} ({}/{}): not a licence in this application",
          licenceReference,
          completed.incrementAndGet(),
          total
      );
      return new NotInLms(licenceReference);
    }

    try {
      var history = licenceWritebackService.readHistory(licence.get());

      LicenceWritebackResult result;
      writePermits.acquireUninterruptibly();
      try {
        result = licenceWritebackService.overwriteLicencePositionsFromPears(licence.get(), history).toResult();
      } finally {
        writePermits.release();
      }

      LOGGER.info("Wrote back {} ({}/{}): {}", licenceReference, completed.incrementAndGet(), total, result.result());
      return new WrittenBack(licenceReference, result);
    } catch (RuntimeException e) {
      LOGGER.error("Could not write back {} ({}/{})", licenceReference, completed.incrementAndGet(), total, e);
      return new Failed(licenceReference, String.valueOf(e.getMessage()));
    }
  }

  /**
   * Totals up the outcomes, which are in licence order whatever order they finished in.
   */
  private static LicenceWritebackAllResult summarise(List<Outcome> outcomes) {
    var writtenBack = 0;
    var nothingToDo = new ArrayList<String>();
    var notInLms = new ArrayList<String>();
    var failed = new LinkedHashMap<String, String>();
    var positions = 0;
    var changes = 0;
    var operations = 0;
    var operationsIgnored = 0;

    for (var outcome : outcomes) {
      switch (outcome) {
        case NotInLms(String licenceReference) -> notInLms.add(licenceReference);
        case Failed(String licenceReference, String message) -> failed.put(licenceReference, message);
        case WrittenBack(String licenceReference, LicenceWritebackResult result)
            when result.positions() == 0 -> nothingToDo.add(licenceReference);
        case WrittenBack(String ignored, LicenceWritebackResult result) -> {
          writtenBack++;
          positions += result.positions();
          changes += result.changes();
          operations += result.operations();
          operationsIgnored += result.operationsIgnored();
        }
      }
    }

    LOGGER.info(
        "Wrote back {} of {} licences from PEARS: {} with nothing to do, {} not in this application, {} failed",
        writtenBack,
        outcomes.size(),
        nothingToDo.size(),
        notInLms.size(),
        failed.size()
    );

    return new LicenceWritebackAllResult(
        outcomes.size(),
        writtenBack,
        nothingToDo,
        notInLms,
        failed,
        positions,
        changes,
        operations,
        operationsIgnored
    );
  }

  /**
   * What became of one licence.
   */
  private sealed interface Outcome {
  }

  private record WrittenBack(String licenceReference, LicenceWritebackResult result) implements Outcome {
  }

  private record NotInLms(String licenceReference) implements Outcome {
  }

  private record Failed(String licenceReference, String message) implements Outcome {
  }
}
