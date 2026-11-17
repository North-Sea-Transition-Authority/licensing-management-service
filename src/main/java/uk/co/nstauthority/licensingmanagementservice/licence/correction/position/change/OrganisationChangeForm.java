package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import uk.co.fivium.formlibrary.input.StringInput;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public interface OrganisationChangeForm {
  StringInput getId();

  Class<? extends LicenceOperation> operationType();
}
