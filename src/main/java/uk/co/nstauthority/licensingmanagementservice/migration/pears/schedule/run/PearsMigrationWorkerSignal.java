package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Wakes {@link PearsScheduleMigrationJob} on this instance when a run is started, retried or resumed. Until then the
 * job doesn't touch the run tables, so it does nothing from start-up, and once a run is done it goes idle again. A run
 * left active by a restart waits to be resumed.
 */
@Component
public class PearsMigrationWorkerSignal {

  private final AtomicLong wakeUps = new AtomicLong();
  // Only the job's thread sets it
  private volatile long idleAt;

  /**
   * Wakes the job once the current transaction commits, so the run it looks for is there to see.
   */
  public void wakeAfterCommit() {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      wakeUps.incrementAndGet();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        wakeUps.incrementAndGet();
      }
    });
  }

  /**
   * The latest wake-up, to pass to {@link #idle(long)}; empty while the job is idle.
   */
  OptionalLong wakeUp() {
    var latest = wakeUps.get();
    return latest == idleAt ? OptionalLong.empty() : OptionalLong.of(latest);
  }

  /**
   * Sends the job idle, unless it was woken again after {@code wakeUp}, as a run started meanwhile may not have been
   * seen.
   */
  void idle(long wakeUp) {
    idleAt = wakeUp;
  }
}
