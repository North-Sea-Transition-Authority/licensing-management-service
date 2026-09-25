package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

/**
 * Where an operation comes in PEARS' order within its transaction, which is how a migrator says
 * where its change belongs without knowing what the other migrators produced. The operation id
 * breaks a tie on the sequence, which a document can leave unset.
 */
public record PearsOperationKey(int operationSequence, long operationId)
    implements Comparable<PearsOperationKey> {

  @Override
  public int compareTo(PearsOperationKey other) {
    var bySequence = Integer.compare(operationSequence, other.operationSequence);
    return bySequence != 0 ? bySequence : Long.compare(operationId, other.operationId);
  }
}
