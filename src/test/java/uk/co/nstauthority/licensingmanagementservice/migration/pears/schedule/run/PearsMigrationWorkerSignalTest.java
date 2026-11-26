package uk.co.nstauthority.licensingmanagementservice.migration.pears.schedule.run;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class PearsMigrationWorkerSignalTest {

  private final PearsMigrationWorkerSignal signal = new PearsMigrationWorkerSignal();

  @Test
  void wakeUp_untilWoken_isEmpty() {
    assertThat(signal.wakeUp()).isEmpty();
  }

  @Test
  void wakeAfterCommit_outsideATransaction_wakesTheJobAtOnce() {
    signal.wakeAfterCommit();

    assertThat(signal.wakeUp()).isPresent();
  }

  @Test
  void wakeAfterCommit_inATransaction_wakesTheJobOnlyOnceItCommits() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      signal.wakeAfterCommit();
      assertThat(signal.wakeUp()).isEmpty();

      TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    assertThat(signal.wakeUp()).isPresent();
  }

  @Test
  void idle_sendsTheJobIdle() {
    signal.wakeAfterCommit();

    signal.idle(signal.wakeUp().orElseThrow());

    assertThat(signal.wakeUp()).isEmpty();
  }

  @Test
  void idle_whenWokenAgainSince_leavesTheJobAwake() {
    signal.wakeAfterCommit();
    var seen = signal.wakeUp().orElseThrow();
    signal.wakeAfterCommit();

    signal.idle(seen);

    assertThat(signal.wakeUp()).isPresent();
  }
}
