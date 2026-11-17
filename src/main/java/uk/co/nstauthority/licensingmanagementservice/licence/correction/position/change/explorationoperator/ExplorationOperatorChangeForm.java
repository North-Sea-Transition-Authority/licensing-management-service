package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.explorationoperator;

import com.google.common.annotations.VisibleForTesting;
import uk.co.fivium.formlibrary.input.StringInput;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.OrganisationChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.ExplorationOperatorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public class ExplorationOperatorChangeForm implements OrganisationChangeForm {
  private final StringInput id = new StringInput("id", "an exploration operator");

  public ExplorationOperatorChangeForm() {
  }

  @VisibleForTesting
  public ExplorationOperatorChangeForm(String id) {
    this.id.setInputValue(id);
  }

  @Override
  public StringInput getId() {
    return id;
  }

  @Override
  public Class<? extends LicenceOperation> operationType() {
    return ExplorationOperatorOperation.class;
  }
}
