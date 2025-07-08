package uk.co.nstauthority.licensingmanagementservice.licence.schedule.startjourney;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum ScheduleJourneyOption implements Displayable {
  CREATE("Create a new licence schedule", 1),
  UPDATE("Update an existing licence schedule", 2);

  private final String displayName;

  private final Integer displayOrder;

  ScheduleJourneyOption(
      String displayName,
      Integer displayOrder
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
  }

  public static Map<String, String> getScheduleJourneyRadioOptions() {
    return DisplayableEnumOptionUtil.getDisplayableOptions(ScheduleJourneyOption.class);
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return this.displayOrder;
  }

  @Override
  public String getEnumName() {
    return this.name();
  }
}
