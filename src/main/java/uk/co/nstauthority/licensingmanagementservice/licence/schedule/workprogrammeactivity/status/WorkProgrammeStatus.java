package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.status;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity.WorkProgrammeActivityCategory;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum WorkProgrammeStatus implements Displayable {
  OPEN("Open", 1, "govuk-tag--green"),
  IN_PROGRESS("In progress", 2, "govuk-tag--light-blue"),
  COMPLETE("Complete", 3, ""),
  FULL_WAIVER("Full waiver", 4, "govuk-tag--red"),
  TRANSFERRED("Transferred", 5, "govuk-tag--yellow"),
  DROPPED("Dropped (not drilled)", 6, "govuk-tag--grey");

  private static final Set<WorkProgrammeActivityCategory> DROPPED_CATEGORIES = Set.of(
      WorkProgrammeActivityCategory.DRILL_OR_DROP_WELL,
      WorkProgrammeActivityCategory.OTHER_ACTIVITY
  );

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

  public static Optional<WorkProgrammeStatus> fromDisplayName(String displayName) {
    return Arrays.stream(values())
        .filter(s -> s.displayName.equals(displayName))
        .findFirst();
  }

  public static Map<String, String> getRadioOptions(WorkProgrammeActivityCategory category) {
    return DisplayableEnumOptionUtil.getDisplayableOptionsFromStream(
        Arrays.stream(values()).filter(status -> status.isAvailableFor(category)));
  }

  public boolean isAvailableFor(WorkProgrammeActivityCategory category) {
    return this != DROPPED || DROPPED_CATEGORIES.contains(category);
  }

  public String getTagDisplayClass() {
    return tagDisplayClass;
  }
}
