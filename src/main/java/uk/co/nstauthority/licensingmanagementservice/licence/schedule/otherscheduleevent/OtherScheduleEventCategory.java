package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum OtherScheduleEventCategory implements Displayable {
  MANDATORY_RELINQUISHMENT(
      "Mandatory relinquishment",
      1,
      Set.of(LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION, LicenceType.GAS_STORAGE)
  ),
  OTHER_ACTIVITY(
      "Other activity",
          999,
      Set.of(LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION, LicenceType.GAS_STORAGE)
  );

  private final String displayName;
  private final Integer displayOrder;
  private final Set<LicenceType> licenceTypes;

  OtherScheduleEventCategory(
      String displayName,
      Integer displayOrder,
      Set<LicenceType> licenceTypes
  ) {
    this.displayName = displayName;
    this.displayOrder = displayOrder;
    this.licenceTypes = licenceTypes;
  }

  public Set<LicenceType> getLicenceTypes() {
    return licenceTypes;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public int getDisplayOrder() {
    return displayOrder;
  }

  public static Map<String, String> getCategoriesForLicenceType(LicenceType licenceType) {
    var licenceTypeCategories = Arrays.stream(OtherScheduleEventCategory.values())
        .filter(category -> category.licenceTypes.contains(licenceType))
        .toList();

    return DisplayableEnumOptionUtil.getDisplayableOptions(licenceTypeCategories);
  }
}
