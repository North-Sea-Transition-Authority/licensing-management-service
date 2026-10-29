package uk.co.nstauthority.licensingmanagementservice.mockups.licencecorrection.operator;

import java.util.Locale;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.IgnoreMaterialisationRule;

@IgnoreMaterialisationRule
public enum MockOperatorType {

  STORAGE("Storage operator", "CS001", LicenceType.CARBON_STORAGE, false),
  SUBAREA("Subarea operator", "P1", LicenceType.SEAWARD_PRODUCTION, true);

  private final String displayName;
  private final String licenceReference;
  private final LicenceType licenceType;
  private final boolean subareaScoped;

  MockOperatorType(String displayName, String licenceReference, LicenceType licenceType, boolean subareaScoped) {
    this.displayName = displayName;
    this.licenceReference = licenceReference;
    this.licenceType = licenceType;
    this.subareaScoped = subareaScoped;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getLicenceReference() {
    return licenceReference;
  }

  public LicenceType getLicenceType() {
    return licenceType;
  }

  public boolean isSubareaScoped() {
    return subareaScoped;
  }

  static MockOperatorType fromUrlSlug(String urlSlug) {
    return valueOf(urlSlug.toUpperCase(Locale.ROOT));
  }
}
