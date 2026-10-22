package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum EventTrackerApplicationStatus implements Displayable {
  REQUEST_SUBMITTED("Request Submitted", 20, false, Set.of(ApplicationStatus.SUBMITTED)),
  // Framing and consult are regulator case-progress stages with no equivalent application status yet, so
  // neither can currently match a row - they remain selectable options, just ones nothing satisfies.
  FRAMING("Framing", 30, true, Set.of()),
  CONSULT("Consult", 40, true, Set.of()),
  DSP_READY("DSP Ready", 50, true, Set.of(ApplicationStatus.DSP_UPLOADED)),
  DECISION_ISSUED("Decision issued", 60, false, Set.of(ApplicationStatus.ISSUE_DECISION, ApplicationStatus.COMPLETE))
  ;

  private final String displayName;
  private final int displayOrder;
  private final boolean regulatorOnly;
  private final Set<ApplicationStatus> applicationStatuses;

  EventTrackerApplicationStatus(
      String displayName, int displayOrder, boolean regulatorOnly, Set<ApplicationStatus> applicationStatuses
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.regulatorOnly = regulatorOnly;
    this.applicationStatuses = applicationStatuses;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public boolean isRegulatorOnly() {
    return regulatorOnly;
  }

  public Set<ApplicationStatus> getApplicationStatuses() {
    return applicationStatuses;
  }

  public static Map<String, String> getIndustryStatuses() {
    return Arrays.stream(values())
        .filter(status -> !status.isRegulatorOnly())
        .sorted(Comparator.comparingInt(Displayable::getDisplayOrder))
        .collect(StreamUtil.toLinkedHashMap(Displayable::getEnumName, Displayable::getDisplayName));
  }
}
