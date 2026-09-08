package uk.co.nstauthority.licensingmanagementservice.mockups.schedule;

import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.AddToListItem;

/**
 * Hardcoded add to list row, standing in for a licence that has already been added to the alternative work programme.
 * Implements the getter style {@link AddToListItem} contract the FDS add to list macro reads rather than relying on
 * record accessors.
 */
public record MockAddToListLicence(String id, String name) implements AddToListItem {

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
