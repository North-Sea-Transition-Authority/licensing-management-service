package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectable;

public record OrganisationUnitJson(
    Integer organisationUnitId,
    String name
) implements SearchSelectable {

  public static OrganisationUnitJson from(OrganisationUnit organisationUnit) {
    return new OrganisationUnitJson(organisationUnit.getOrganisationUnitId(), organisationUnit.getName());
  }

  @Override
  public String getSelectionId() {
    return this.organisationUnitId.toString();
  }

  @Override
  public String getSelectionText() {
    return this.name;
  }
}
