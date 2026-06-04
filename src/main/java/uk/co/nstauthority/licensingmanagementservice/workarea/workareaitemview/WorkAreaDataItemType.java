package uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview;

import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum WorkAreaDataItemType implements Displayable {
  LICENCE_CONTINUATION_APPLICATION("Licence continuation application"),
  SCHEDULE_WORK_PROGRAMME_APPLICATION("Schedule and work programme application");

  private final String displayName;

  WorkAreaDataItemType(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }
}
