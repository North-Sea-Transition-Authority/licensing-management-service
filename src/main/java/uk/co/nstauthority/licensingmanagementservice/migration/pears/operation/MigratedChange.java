package uk.co.nstauthority.licensingmanagementservice.migration.pears.operation;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeStatus;

/**
 * One change a migrator makes, named by the position it lands on and the PEARS operation whose
 * place in the order it takes. There is no change order here: allocating it across all the
 * migrators is {@code LicenceWritebackService}'s job.
 *
 * @param status what the change is saved as; PEARS has no notion of consent, so everything
 *               migrated is consented
 */
public record MigratedChange(
    PearsPositionKey position,
    PearsOperationKey at,
    List<LicenceOperation> operations,
    LicencePositionChangeStatus status
) {

  /**
   * A consented change, which is every change a migration makes today.
   */
  public static MigratedChange from(
      PearsPositionKey position,
      PearsOperationKey at,
      List<LicenceOperation> operations
  ) {
    return new MigratedChange(position, at, operations, LicencePositionChangeStatus.CONSENTED);
  }
}
