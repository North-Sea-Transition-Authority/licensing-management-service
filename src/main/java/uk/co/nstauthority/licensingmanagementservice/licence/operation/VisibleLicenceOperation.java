package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An operation that is shown on a position as a change view.
 *
 * <p>The block, subarea creation and subarea end operations the PEARS migration produces are {@link HiddenLicenceOperation}s
 * instead: they are carried across so the spatial timeline is right, and nothing has been designed for showing them on a
 * position yet.
 */
public sealed interface VisibleLicenceOperation extends LicenceOperation permits
    AdministratorOperation,
    SetEquityOperation,
    TransferEquityOperation,
    PartialSurrenderOperation,
    SubareaOperation,
    LicenseeOperation {

  @JsonIgnore
  OperationRoutes getOperationUrls();

}
