package uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam;

import java.util.Arrays;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.Displayable;

public enum LicenceTeam implements Displayable {
  CS_NEW_VENTURES(
      "New ventures team",
      LicenceType.CARBON_STORAGE,
      TeamType.CARBON_STORAGE_LICENSING,
      Role.CASE_MANAGER_CS_NEW_VENTURES
  ),
  CS_CARBON_TRANSPORT_AND_STORAGE(
      "Carbon transport and storage team",
      LicenceType.CARBON_STORAGE,
      TeamType.CARBON_STORAGE_LICENSING,
      Role.CASE_MANAGER_CS_CTS
  );

  private final String displayName;
  private final LicenceType licenceType;
  private final TeamType teamType;
  private final Role caseManagerRole;

  LicenceTeam(String displayName, LicenceType licenceType, TeamType teamType, Role caseManagerRole) {
    this.displayName = displayName;
    this.licenceType = licenceType;
    this.teamType = teamType;
    this.caseManagerRole = caseManagerRole;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  public LicenceType getLicenceType() {
    return licenceType;
  }

  public TeamType getTeamType() {
    return teamType;
  }

  public Role getCaseManagerRole() {
    return caseManagerRole;
  }

  public static List<LicenceTeam> fromTeamType(LicenceType licenceType) {
    return Arrays.stream(values())
        .filter(licenceTeam -> licenceTeam.getLicenceType() == licenceType)
        .toList();
  }
}
