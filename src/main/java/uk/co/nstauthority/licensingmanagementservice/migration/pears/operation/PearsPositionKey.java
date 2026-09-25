package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

/**
 * Which position a produced change lands on, named by the PEARS transaction that reached it. The
 * transaction rather than the date and order, because a change is produced before any position has
 * been built and so before any of them has an id.
 */
public record PearsPositionKey(long transactionId) {
}
