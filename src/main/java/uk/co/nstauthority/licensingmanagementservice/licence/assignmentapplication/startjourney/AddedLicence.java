package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.AddToListItem;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

record AddedLicence(String id, String name) implements AddToListItem {

  static AddedLicence from(Licence licence) {
    return new AddedLicence(String.valueOf(licence.getId()), licence.getLicenceReference());
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
