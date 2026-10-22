package uk.co.nstauthority.licensingmanagementservice.mockups.uploaddsp;

import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.AddToListItem;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectable;

public record DecisionDetailsPerson(String id, String name) implements SearchSelectable, AddToListItem {

  @Override
  public String getSelectionId() {
    return id;
  }

  @Override
  public String getSelectionText() {
    return name;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
