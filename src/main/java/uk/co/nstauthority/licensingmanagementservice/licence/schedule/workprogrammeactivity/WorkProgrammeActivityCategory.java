package uk.co.nstauthority.licensingmanagementservice.licence.schedule.workprogrammeactivity;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

public enum WorkProgrammeActivityCategory implements Displayable {
  ASSESS_PRE_FEED_PLAN(
      "Assess pre feed plan",
      1,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  DRILLING_WELL(
      "Drilling Well",
      2,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  DRILL_OR_DROP_WELL(
      "Drill or drop well",
      3,
      Set.of(LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)
  ),
  DRILL_WELL(
      "Drill well",
      4,
      Set.of(LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)
  ),
  EARLY_RISK_ASSESSMENT(
      "Early risk assessment",
      5,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  EARLY_RISK_ASSESSMENT_FURTHER_MEASURES(
      "Early risk assessment further measures",
      6,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  EARLY_RISK_ASSESSMENT_WORKSHOP(
      "Early risk assessment workshop",
      7,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  END_ASSESS_PHASE_REVIEW(
      "End assess phase review",
      8,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  END_DEFINE_PHASE_REVIEW(
      "End define phase review",
      9,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  NEW_SHOOT_2_D_SEISMIC_DATA(
      "New shoot 2D seismic data",
      10,
      Set.of(LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)
  ),
  NEW_SHOOT_3_D_SEISMIC_DATA(
      "New shoot 3D seismic data",
      11,
      Set.of(LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)
  ),
  OBTAIN_EXISTING_2_D_SEISMIC_DATA(
      "Obtain existing 2D seismic data",
      12,
      Set.of(LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)
  ),
  OBTAIN_EXISTING_3_D_SEISMIC_DATA(
      "Obtain existing 3D seismic data",
      13,
      Set.of(LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)
  ),
  REPROCESS_SEISMIC_DATA(
      "Reprocess seismic data",
      14,
      Set.of(LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)
  ),
  SEISMIC_ACQUISITION_AND_PROCESSING(
      "Seismic acquisition and processing",
      15,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  SEISMIC_REPROCESSING_AND_INTERPRETATION(
      "Seismic reprocessing and interpretation",
      16,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  SITE_CHARACTERISATION_REVIEW_REPORT(
      "Site characterisation review report",
      17,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  STORAGE_PERMIT_APPLICATION(
      "Storage permit application",
      18,
      Set.of(LicenceType.CARBON_STORAGE)
  ),
  WELL_INVESTMENT_ENGAGEMENT(
      "Well investment engagement",
      19,
      Set.of(LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)
  ),
  WELL_TEST(
      "Well test",
      20,
      Set.of(LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION)
  ),
  OTHER_ACTIVITY(
      "Other activity",
          999,
      Set.of(LicenceType.CARBON_STORAGE, LicenceType.SEAWARD_PRODUCTION, LicenceType.LANDWARD_PRODUCTION, LicenceType.GAS_STORAGE)
  );

  private final String displayName;
  private final Integer displayOrder;
  private final Set<LicenceType> licenceTypes;

  WorkProgrammeActivityCategory(
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

  public static Optional<WorkProgrammeActivityCategory> fromDisplayName(String displayName) {
    return Arrays.stream(values())
        .filter(c -> c.displayName.equals(displayName))
        .findFirst();
  }

  public static Map<String, String> getCategoriesForLicenceType(LicenceType licenceType) {
    var licenceTypeCategories = Arrays.stream(WorkProgrammeActivityCategory.values())
        .filter(category -> category.licenceTypes.contains(licenceType))
        .toList();

    return DisplayableEnumOptionUtil.getDisplayableOptions(licenceTypeCategories);
  }
}
