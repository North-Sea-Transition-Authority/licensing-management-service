package uk.co.nstauthority.licensingmanagementservice.licence.position.change.storageoperator;

import uk.co.nstauthority.licensingmanagementservice.licence.position.change.OrganisationChangeContext;

public record StorageOperatorChangeContext(
    Integer currentStorageOperatorId,
    Integer previousStorageOperatorId,
    String currentStorageOperatorName,
    String previousStorageOperatorName
) implements OrganisationChangeContext {

  @Override
  public Integer currentId() {
    return currentStorageOperatorId;
  }

  @Override
  public Integer previousId() {
    return previousStorageOperatorId;
  }

  @Override
  public String currentName() {
    return currentStorageOperatorName;
  }

  @Override
  public String previousName() {
    return previousStorageOperatorName;
  }
}
