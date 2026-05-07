package uk.co.nstauthority.licensingmanagementservice.licence.application;

import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEnum;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum ApplicationType implements Displayable, PathVariableEnum {

  SCHEDULE_AMENDMENT_APPLICATION(
      "Licence and work programme extension and amendment application",
      "licence-and-work-programme-extension-and-amendment-application",
      10
  ),
  CONTINUATION_APPLICATION(
      "Licence continuation application",
      "licence-continuation-application",
      20
  ),
  ;


  private final String displayName;
  private final String urlSlug;
  private final int displayOrder;

  ApplicationType(
      String displayName,
      String urlSlug,
      int displayOrder
  ) {
    this.displayName = displayName;
    this.urlSlug = urlSlug;
    this.displayOrder = displayOrder;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  public String getUrlSlug() {
    return urlSlug;
  }

  @Override
  public String getPathVariableName() {
    return ApplicationTypeArgumentResolver.APPLICATION_TYPE;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }
}