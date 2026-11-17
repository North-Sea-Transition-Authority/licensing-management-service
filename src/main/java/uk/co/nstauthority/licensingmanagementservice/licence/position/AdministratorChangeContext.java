package uk.co.nstauthority.licensingmanagementservice.licence.position;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.OrganisationChangeContext;

public record AdministratorChangeContext(
    @Nullable Integer currentAdministratorId,
    @Nullable Integer previousAdministratorId,
    String currentAdministratorName,
    String previousAdministratorName
) implements OrganisationChangeContext {

  @Override
  public Integer currentId() {
    return currentAdministratorId;
  }

  @Override
  public Integer previousId() {
    return previousAdministratorId;
  }

  @Override
  public String currentName() {
    return currentAdministratorName;
  }

  @Override
  public String previousName() {
    return previousAdministratorName;
  }
}
