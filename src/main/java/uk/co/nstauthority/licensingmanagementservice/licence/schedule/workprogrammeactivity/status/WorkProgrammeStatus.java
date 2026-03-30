package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum WorkProgrammeStatus implements Displayable {
  OPEN("Open", 1),
  IN_PROGRESS("In progress", 2),
  COMPLETE("Complete", 3),
  FULL_WAIVER("Full waiver", 4),
  TRANSFERRED("Transferred", 5);

  private final String displayName;
  private final int displayOrder;

  WorkProgrammeStatus(
      String displayName,
      int displayOrder
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public static Map<String, String> getRadioOptions() {
    return DisplayableEnumOptionUtil.getDisplayableOptions(WorkProgrammeStatus.class);
  }
}
