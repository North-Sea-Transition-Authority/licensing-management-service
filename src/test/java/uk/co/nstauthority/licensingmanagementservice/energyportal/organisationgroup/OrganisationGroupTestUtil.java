package uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup;

import static uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitTestUtil.ORG_UNIT_1;
import static uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitTestUtil.ORG_UNIT_2;

import java.util.Collections;
import java.util.List;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;

public class OrganisationGroupTestUtil {
  private OrganisationGroupTestUtil() {
  }

  public static final Integer ORGANISATION_GROUP_ID_1 = 1;

  public static final String ORGANISATION_GROUP_NAME_1 = "Organisation group 1";

  public static final OrganisationGroup ORGANISATION_GROUP_1 =
      OrganisationGroup.newBuilder()
          .organisationGroupId(ORGANISATION_GROUP_ID_1)
          .name(ORGANISATION_GROUP_NAME_1)
          .organisationUnits(List.of(ORG_UNIT_1))
          .build();

  public static final OrganisationGroup ORGANISATION_GROUP_1_WITH_EMPTY_ORGANISATION_UNITS =
      OrganisationGroup.newBuilder()
          .organisationGroupId(ORGANISATION_GROUP_ID_1)
          .name(ORGANISATION_GROUP_NAME_1)
          .organisationUnits(Collections.emptyList())
          .build();

  public static final Integer ORGANISATION_GROUP_ID_2 = 2;

  public static final String ORGANISATION_GROUP_NAME_2 = "Organisation group 2";

  public static final OrganisationGroup ORGANISATION_GROUP_2 =
      OrganisationGroup.newBuilder()
          .organisationGroupId(ORGANISATION_GROUP_ID_2)
          .name(ORGANISATION_GROUP_NAME_2)
          .organisationUnits(List.of(ORG_UNIT_2))
          .build();

  public static final OrganisationGroup ORGANISATION_GROUP_2_WITH_EMPTY_ORGANISATION_UNITS =
      OrganisationGroup.newBuilder()
          .organisationGroupId(ORGANISATION_GROUP_ID_2)
          .name(ORGANISATION_GROUP_NAME_2)
          .organisationUnits(Collections.emptyList())
          .build();

  public static OrganisationGroupDto createOrganisationGroupDto(int organisationGroupId, String organisationGroupName) {
    var organisationGroupDto = new OrganisationGroupDto();
    organisationGroupDto.setOrganisationGroupId(organisationGroupId);
    organisationGroupDto.setOrganisationGroupName(organisationGroupName);
    return organisationGroupDto;
  }
}
