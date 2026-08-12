package uk.co.nstauthority.licensingmanagementservice.licence.application;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import uk.co.nstauthority.licensingmanagementservice.endpointvalidation.PathVariableEnum;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.PhaseGated;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum ApplicationType implements Displayable, PathVariableEnum, PhaseGated {

  SCHEDULE_AMENDMENT_APPLICATION(
      "Licence and work programme extension and amendment application",
      "licence-and-work-programme-extension-and-amendment-application",
      10,
      "I want to extend a term, phase or work programme activity due date, or amend a work programme activity",
      ReleaseFeature.SCHEDULE_APPLICATION
  ),
  CONTINUATION_APPLICATION(
      "Licence continuation application",
      "licence-continuation-application",
      20,
      "I want to continue into the next term or phase of my production licence",
      ReleaseFeature.CONTINUATION_APPLICATION
  ),
  ;

  private final String displayName;
  private final String urlSlug;
  private final int displayOrder;
  private final String selectionDisplay;
  private final ReleaseFeature releaseFeature;

  ApplicationType(
      String displayName,
      String urlSlug,
      int displayOrder,
      String selectionDisplay,
      ReleaseFeature releaseFeature
  ) {
    this.displayName = displayName;
    this.urlSlug = urlSlug;
    this.displayOrder = displayOrder;
    this.selectionDisplay = selectionDisplay;
    this.releaseFeature = releaseFeature;
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

  public String getSelectionDisplay() {
    return selectionDisplay;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return releaseFeature;
  }

  public static Map<String, String> getSelectionDisplayOptions() {
    return getSelectionDisplayOptions(Arrays.asList(ApplicationType.values()));
  }

  public static Map<String, String> getSelectionDisplayOptions(Collection<ApplicationType> applicationTypes) {
    return applicationTypes.stream()
        .sorted(Comparator.comparingInt(Displayable::getDisplayOrder))
        .collect(StreamUtil.toLinkedHashMap(ApplicationType::getEnumName, ApplicationType::getSelectionDisplay));
  }


}
