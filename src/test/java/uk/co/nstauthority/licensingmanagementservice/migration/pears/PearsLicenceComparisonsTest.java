package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class PearsLicenceComparisonsTest {

  private static final List<PearsLicenceReference> LICENCES = List.of(
      new PearsLicenceReference("P", 1),
      new PearsLicenceReference("P", 2),
      new PearsLicenceReference("P", 3),
      new PearsLicenceReference("P", 4),
      new PearsLicenceReference("P", 5)
  );

  private PlatformTransactionManager transactionManager;

  @BeforeEach
  void setUp() {
    transactionManager = mock(PlatformTransactionManager.class);
    when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
  }

  @Test
  void compare_givesTheResultsInTheOrderGiven_whateverOrderTheyFinishIn() {
    var firstMayFinish = new CountDownLatch(1);

    var results = new PearsLicenceComparisons(transactionManager, 5).compare(LICENCES, licence -> {
      if (licence.licenceNo() == 1) {
        await(firstMayFinish);
      } else if (licence.licenceNo() == 5) {
        firstMayFinish.countDown();
      }
      return licence.licenceReference();
    });

    assertThat(results).containsExactly("P1", "P2", "P3", "P4", "P5");
  }

  @Test
  void compare_comparesNoMoreThanTheParallelismAtOnce() {
    var inHand = new AtomicInteger();
    var mostInHand = new AtomicInteger();
    // Never counted down: each licence waits on it for a while, so they overlap
    var neverReleased = new CountDownLatch(1);

    new PearsLicenceComparisons(transactionManager, 2).compare(LICENCES, licence -> {
      mostInHand.accumulateAndGet(inHand.incrementAndGet(), Math::max);
      awaitBriefly(neverReleased);
      inHand.decrementAndGet();
      return licence;
    });

    assertThat(mostInHand.get()).isBetween(1, 2);
  }

  @Test
  void compare_comparesEachLicenceInAReadOnlyTransactionOfItsOwn() {
    new PearsLicenceComparisons(transactionManager, 3).compare(LICENCES, PearsLicenceReference::licenceReference);

    var definitions = ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(transactionManager, times(LICENCES.size())).getTransaction(definitions.capture());
    assertThat(definitions.getAllValues()).allSatisfy(definition -> {
      assertThat(definition.isReadOnly()).isTrue();
      assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    });
  }

  @Test
  void compare_whenALicenceFails_throwsItsFailureOnceEveryLicenceHasFinished() {
    var compared = new AtomicInteger();
    var comparisons = new PearsLicenceComparisons(transactionManager, 2);

    assertThatThrownBy(() -> comparisons.compare(LICENCES, licence -> {
      compared.incrementAndGet();
      if (licence.licenceNo() == 2) {
        throw new IllegalStateException("P2 failed");
      }
      return licence;
    }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("P2 failed");
    assertThat(compared).hasValue(LICENCES.size());
  }

  @Test
  void new_whenTheParallelismIsUnderOne_throws() {
    assertThatThrownBy(() -> new PearsLicenceComparisons(transactionManager, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("pears.comparison.parallelism must be at least 1");
  }

  private static void await(CountDownLatch latch) {
    try {
      assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static void awaitBriefly(CountDownLatch latch) {
    try {
      assertThat(latch.await(50, TimeUnit.MILLISECONDS)).isFalse();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
