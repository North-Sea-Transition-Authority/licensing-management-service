package uk.co.nstauthority.licensingmanagementservice.mockups.schedule;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

/**
 * Mockup stand-in for {@code WorkProgrammeStatus} in which the TRANSFERRED option is replaced by
 * ALTERNATIVE_WORK_PROGRAMME. Kept separate from the real enum so the live status screen is untouched.
 */
public enum MockWorkProgrammeStatus implements Displayable {
  OPEN("Open", 1),
  IN_PROGRESS("In progress", 2),
  COMPLETE("Complete", 3),
  FULL_WAIVER("Full waiver", 4),
  ALTERNATIVE_WORK_PROGRAMME("Alternative work programme", 5);

  private final String displayName;
  private final int displayOrder;

  MockWorkProgrammeStatus(String displayName, int displayOrder) {
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
    return DisplayableEnumOptionUtil.getDisplayableOptions(MockWorkProgrammeStatus.class);
  }
}
