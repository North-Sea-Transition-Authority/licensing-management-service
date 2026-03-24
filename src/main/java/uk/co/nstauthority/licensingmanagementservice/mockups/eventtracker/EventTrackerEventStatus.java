package uk.co.nstauthority.licensingmanagementservice.mockups.eventtracker;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum EventTrackerEventStatus implements Displayable {
  REQUEST_PENDING("Request Pending", 10, false),
  REQUEST_SUBMITTED("Request Submitted", 20, false),
  FRAMING("Framing", 30, true),
  CONSULT("Consult", 40, true),
  DSP_READY("DSP Ready", 50, true),
  DECISION_ISSUED("Decision issued", 60, false)
  ;

  private final String displayName;
  private final int displayOrder;
  private final boolean regulatorOnly;

  EventTrackerEventStatus(String displayName, int displayOrder, boolean regulatorOnly) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.regulatorOnly = regulatorOnly;
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

  public static Map<String, String> getIndustryStatuses() {
    return Arrays.stream(values())
        .filter(status -> !status.isRegulatorOnly())
        .sorted(Comparator.comparingInt(Displayable::getDisplayOrder))
        .collect(StreamUtil.toLinkedHashMap(Displayable::getEnumName, Displayable::getDisplayName));
  }
}
