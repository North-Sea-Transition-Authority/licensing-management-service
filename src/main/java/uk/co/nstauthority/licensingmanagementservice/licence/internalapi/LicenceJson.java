package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectable;

public record LicenceJson(
    Integer licenceId,
    String licenceReference
) implements SearchSelectable {

  @Override
  public String getSelectionId() {
    return licenceId.toString();
  }

  @Override
  public String getSelectionText() {
    return licenceReference;
  }
}
