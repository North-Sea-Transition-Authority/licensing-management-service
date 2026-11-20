package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.uploaddsp;

import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumWithDescription;

public enum UploadDspAction implements DisplayableEnumWithDescription {
  CORRECT(
      "Replace the DSP to correct a mistake",
      "Use this if the wrong document was uploaded. The new document replaces the current DSP.",
      10
  ),
  ;

  private final String displayName;
  private final String description;
  private final int displayOrder;

  UploadDspAction(String displayName, String description, int displayOrder) {
    this.displayName = displayName;
    this.description = description;
    this.displayOrder = displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public String getEnumName() {
    return name();
  }

  public static Map<String, Map<String, String>> getOptionsWithHintText() {
    return DisplayableEnumOptionUtil.getDisplayableOptionsWithSeparateNameAndDescription(UploadDspAction.class);
  }
}
