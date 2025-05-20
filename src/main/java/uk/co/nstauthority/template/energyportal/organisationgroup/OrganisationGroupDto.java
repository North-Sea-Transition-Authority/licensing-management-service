package uk.co.nstauthority.template.energyportal.organisationgroup;

import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;
import uk.co.nstauthority.template.fds.searchselector.SearchSelectable;

public class OrganisationGroupDto implements SearchSelectable {
  private Integer organisationGroupId;
  private String organisationGroupName;

  public static OrganisationGroupDto from(OrganisationGroup organisationGroup) {
    var orgGroup = new OrganisationGroupDto();
    orgGroup.setOrganisationGroupId(organisationGroup.getOrganisationGroupId());
    orgGroup.setOrganisationGroupName(organisationGroup.getName());
    return orgGroup;
  }

  public Integer getOrganisationGroupId() {
    return organisationGroupId;
  }

  public void setOrganisationGroupId(Integer organisationGroupId) {
    this.organisationGroupId = organisationGroupId;
  }

  public String getOrganisationGroupName() {
    return organisationGroupName;
  }

  public void setOrganisationGroupName(String organisationGroupName) {
    this.organisationGroupName = organisationGroupName;
  }

  @Override
  public String getSelectionId() {
    return organisationGroupId.toString();
  }

  @Override
  public String getSelectionText() {
    return organisationGroupName;
  }
}
