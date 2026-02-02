package uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam;

import java.util.Arrays;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceTeam implements Displayable {
  CS_NEW_VENTURES(
      "New ventures team",
      LicenceType.CARBON_STORAGE
  ),
  CS_CARBON_TRANSPORT_AND_STORAGE(
      "Carbon transport and storage team",
      LicenceType.CARBON_STORAGE
  );

  private final String displayName;
  private final LicenceType licenceType;

  LicenceTeam(String displayName, LicenceType licenceType) {
    this.displayName = displayName;
    this.licenceType = licenceType;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  public LicenceType getLicenceType() {
    return licenceType;
  }

  public static List<LicenceTeam> fromTeamType(LicenceType licenceType) {
    return Arrays.stream(values())
        .filter(licenceTeam -> licenceTeam.getLicenceType() == licenceType)
        .toList();
  }
}
