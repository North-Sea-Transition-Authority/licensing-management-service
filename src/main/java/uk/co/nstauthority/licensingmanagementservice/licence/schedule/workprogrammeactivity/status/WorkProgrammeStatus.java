package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum WorkProgrammeStatus implements Displayable {
  OPEN("Open", 1, "govuk-tag--green"),
  IN_PROGRESS("In progress", 2, "govuk-tag--light-blue"),
  COMPLETE("Complete", 3, ""),
  FULL_WAIVER("Full waiver", 4, "govuk-tag--red"),
  TRANSFERRED("Transferred", 5, "govuk-tag--yellow");

  private final String displayName;
  private final int displayOrder;
  private final String tagDisplayClass;

  WorkProgrammeStatus(
      String displayName,
      int displayOrder,
      String tagDisplayClass
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.tagDisplayClass = tagDisplayClass;
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

  public String getTagDisplayClass() {
    return tagDisplayClass;
  }
}
