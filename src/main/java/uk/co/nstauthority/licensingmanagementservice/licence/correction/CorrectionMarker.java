package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import jakarta.annotation.Nullable;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

public enum CorrectionMarker {

  CHANGE_ADDED("Added change", "govuk-tag--green"),
  CHANGE_CORRECTED("Corrected change", "govuk-tag--blue"),
  CHANGE_REMOVED("Removed", "govuk-tag--red");

  private final String label;
  private final String tagClass;

  CorrectionMarker(String label, String tagClass) {
    this.label = label;
    this.tagClass = tagClass;
  }

  public String getLabel() {
    return label;
  }

  public String getTagClass() {
    return tagClass;
  }

  @Nullable
  public static CorrectionMarker forChange(@Nullable String changeType) {
    if (changeType == null) {
      return null;
    }

    return switch (changeType) {
      case LicencePositionChangeType.ADD_CHANGE -> CHANGE_ADDED;
      case LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS -> CHANGE_CORRECTED;
      case LicencePositionChangeType.REMOVE_CHANGE -> CHANGE_REMOVED;
      default -> null;
    };
  }
}