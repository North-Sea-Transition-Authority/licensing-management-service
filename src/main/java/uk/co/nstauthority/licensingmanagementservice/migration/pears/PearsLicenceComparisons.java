package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Compares licences for the comparison reports several at a time, each in a read-only transaction of its own. Nearly
 * all the time goes on reading PEARS, so comparing every migrated licence one at a time is slow. Only for comparing
 * what is committed: a caller comparing what its own transaction has written compares one licence at a time instead.
 */
@Component
public class PearsLicenceComparisons {

  private final TransactionTemplate readOnlyTransaction;
  private final int parallelism;

  /**
   * Builds the comparisons, refusing a parallelism under one.
   *
   * @param parallelism how many licences are compared at once, each holding a connection to LMS and, while it reads,
   *                    one to PEARS
   */
  public PearsLicenceComparisons(
      PlatformTransactionManager transactionManager,
      @Value("${pears.comparison.parallelism}") int parallelism
  ) {
    if (parallelism < 1) {
      throw new IllegalArgumentException("pears.comparison.parallelism must be at least 1");
    }
    this.readOnlyTransaction = new TransactionTemplate(transactionManager);
    this.readOnlyTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.readOnlyTransaction.setReadOnly(true);
    this.parallelism = parallelism;
  }

  /**
   * One comparison per licence, in the order given, whatever order they finish in.
   *
   * @throws RuntimeException the first licence's failure, in that order, once every licence has finished
   */
  public <T> List<T> compare(List<PearsLicenceReference> licences, Function<PearsLicenceReference, T> comparison) {
    var permits = new Semaphore(parallelism);
    List<Future<T>> results;
    // closing the executor waits for every licence to finish
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      results = licences.stream()
          .map(licence -> executor.submit(() -> {
            permits.acquireUninterruptibly();
            try {
              return readOnlyTransaction.execute(status -> comparison.apply(licence));
            } finally {
              permits.release();
            }
          }))
          .toList();
    }
    return results.stream().map(PearsLicenceComparisons::result).toList();
  }

  private static <T> T result(Future<T> future) {
    try {
      return future.get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(e.getCause());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
