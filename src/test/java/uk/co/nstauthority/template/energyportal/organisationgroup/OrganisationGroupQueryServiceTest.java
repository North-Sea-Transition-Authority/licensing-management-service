package uk.co.nstauthority.template.energyportal.organisationgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.template.energyportal.organisationgroup.OrganisationGroupTestUtil.ORGANISATION_GROUP_1;
import static uk.co.nstauthority.template.energyportal.organisationgroup.OrganisationGroupTestUtil.ORGANISATION_GROUP_1_WITH_EMPTY_ORGANISATION_UNITS;
import static uk.co.nstauthority.template.energyportal.organisationgroup.OrganisationGroupTestUtil.ORGANISATION_GROUP_2;
import static uk.co.nstauthority.template.energyportal.organisationgroup.OrganisationGroupTestUtil.ORGANISATION_GROUP_2_WITH_EMPTY_ORGANISATION_UNITS;
import static uk.co.nstauthority.template.energyportal.organisationgroup.OrganisationGroupTestUtil.ORGANISATION_GROUP_ID_1;
import static uk.co.nstauthority.template.energyportal.organisationgroup.OrganisationGroupTestUtil.ORGANISATION_GROUP_ID_2;
import static uk.co.nstauthority.template.energyportal.organisations.OrganisationUnitTestUtil.ORG_UNIT_1_JSON;
import static uk.co.nstauthority.template.energyportal.organisations.OrganisationUnitTestUtil.ORG_UNIT_2_JSON;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.client.OrganisationGroupProjectionRoot;
import uk.co.fivium.energyportalapi.generated.client.OrganisationGroupsProjectionRoot;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;

@ExtendWith(MockitoExtension.class)
class OrganisationGroupQueryServiceTest {

  @Mock
  private OrganisationApi organisationApi;

  @InjectMocks
  private OrganisationGroupQueryService organisationGroupQueryService;

  private List<OrganisationGroup> groupList;

  @BeforeEach
  void setup() {
    groupList = List.of(
        new OrganisationGroup(1, "Company 1", null, null, null, Collections.emptyList()),
        new OrganisationGroup(2, "Company 2", null, null, null, Collections.emptyList())
    );
  }

  @Test
  void getOrganisationGroupsByName() {
    var searchTerm = "company";

    when(organisationApi.searchOrganisationGroups(
        eq(searchTerm),
        any(OrganisationGroupsProjectionRoot.class),
        any(RequestPurpose.class)
    )).thenReturn(groupList);

    var organisationGroups =
        organisationGroupQueryService.getOrganisationGroupsByName(searchTerm);

    var argumentCaptor = ArgumentCaptor.forClass(OrganisationGroupsProjectionRoot.class);

    verify(organisationApi).searchOrganisationGroups(
        eq(searchTerm),
        argumentCaptor.capture(),
        any(RequestPurpose.class)
    );

    assertThat(argumentCaptor.getValue().getFields()).containsKeys("organisationGroupId", "name");
    assertThat(organisationGroups)
        .extracting(
            OrganisationGroupDto::getOrganisationGroupId,
            OrganisationGroupDto::getOrganisationGroupName
        )
        .containsExactly(
            tuple(
                groupList.get(0).getOrganisationGroupId(),
                groupList.get(0).getName()
            ),
            tuple(
                groupList.get(1).getOrganisationGroupId(),
                groupList.get(1).getName()
            )
        );
  }

  @Test
  void getOrganisationGroupById_verifyCallsApiWithCorrectParameters() {
    var argumentCaptor = ArgumentCaptor
        .forClass(OrganisationGroupProjectionRoot.class);
    var organisationGroup = new OrganisationGroup(
        1,
        "Royal Dutch Shell",
        "Shell",
        "shell.com",
        "ACTIVE",
        Collections.emptyList());

    when(organisationApi.findOrganisationGroup(
        eq(organisationGroup.getOrganisationGroupId()),
        any(OrganisationGroupProjectionRoot.class),
        any(RequestPurpose.class)))
        .thenReturn(Optional.of(organisationGroup));

    organisationGroupQueryService.getOrganisationGroupById(1);

    verify(organisationApi).findOrganisationGroup(
        eq(organisationGroup.getOrganisationGroupId()),
        argumentCaptor.capture(),
        any(RequestPurpose.class));

    assertThat(argumentCaptor.getValue().getFields())
        .containsOnly(
            entry("organisationGroupId", null),
            entry("name", null));
  }

  @Test
  void getOrganisationGroupsByIds_whenOne_verifyApiCallsAndReturn() {
    var organisationGroup = new OrganisationGroup(
        1,
        "Royal Dutch Shell",
        "Shell",
        "shell.com",
        "ACTIVE",
        Collections.emptyList());

    when(organisationApi.getAllOrganisationGroupsByIds(
        eq(List.of(organisationGroup.getOrganisationGroupId())),
        any(OrganisationGroupsProjectionRoot.class),
        any(RequestPurpose.class)))
        .thenReturn(List.of(organisationGroup));

    var returnedOrganisations = organisationGroupQueryService
        .getOrganisationGroupsByIds(List.of(1));

    verify(organisationApi).getAllOrganisationGroupsByIds(
        eq(List.of(organisationGroup.getOrganisationGroupId())),
        any(),
        any(RequestPurpose.class));

    assertThat(returnedOrganisations)
        .isEqualTo(List.of(organisationGroup));
  }

  @Test
  void getOrganisationGroupsByIds_whenTwo_verifyApiCallsAndReturn() {
    var organisationGroup = new OrganisationGroup(
        1,
        "Royal Dutch Shell",
        "Shell",
        "shell.com",
        "ACTIVE",
        Collections.emptyList());

    var organisationGroup2 = new OrganisationGroup(
        2,
        "Royal Dutch Shell",
        "Shell",
        "shell.com",
        "ACTIVE",
        Collections.emptyList());

    when(organisationApi.getAllOrganisationGroupsByIds(
        eq(List.of(organisationGroup.getOrganisationGroupId(), organisationGroup2.getOrganisationGroupId())),
        any(OrganisationGroupsProjectionRoot.class),
        any(RequestPurpose.class)))
        .thenReturn(List.of(organisationGroup, organisationGroup2));

    var returnedOrganisations = organisationGroupQueryService
        .getOrganisationGroupsByIds(List.of(1, 2));

    verify(organisationApi).getAllOrganisationGroupsByIds(
        eq(List.of(organisationGroup.getOrganisationGroupId(),
            organisationGroup2.getOrganisationGroupId())),
        any(),
        any(RequestPurpose.class));

    assertThat(returnedOrganisations)
        .isEqualTo(List.of(organisationGroup, organisationGroup2));
  }

  @Test
  void getOrganisationUnitsByOrganisationGroupIds_whenOne_verifyApiCallsAndReturn() {
    when(organisationApi.getAllOrganisationGroupsByIds(
        eq(List.of(ORGANISATION_GROUP_ID_1)),
        any(OrganisationGroupsProjectionRoot.class),
        any(RequestPurpose.class)))
        .thenReturn(List.of(ORGANISATION_GROUP_1));

    var returnedOrganisationUnitJsons = organisationGroupQueryService
        .getOrganisationUnitsByOrganisationGroupIds(List.of(ORGANISATION_GROUP_ID_1));

    verify(organisationApi).getAllOrganisationGroupsByIds(
        eq(List.of(ORGANISATION_GROUP_ID_1)),
        any(),
        any(RequestPurpose.class));

    assertThat(returnedOrganisationUnitJsons)
        .isEqualTo(List.of(ORG_UNIT_1_JSON));
  }

  @Test
  void getOrganisationUnitsByOrganisationGroupIds_whenMany_verifyApiCallsAndReturn() {
    when(organisationApi.getAllOrganisationGroupsByIds(
        eq(List.of(ORGANISATION_GROUP_ID_1, ORGANISATION_GROUP_ID_2)),
        any(OrganisationGroupsProjectionRoot.class),
        any(RequestPurpose.class)))
        .thenReturn(List.of(ORGANISATION_GROUP_1, ORGANISATION_GROUP_2));

    var returnedOrganisationUnitJsons = organisationGroupQueryService
        .getOrganisationUnitsByOrganisationGroupIds(List.of(ORGANISATION_GROUP_ID_1, ORGANISATION_GROUP_ID_2));

    verify(organisationApi).getAllOrganisationGroupsByIds(
        eq(List.of(ORGANISATION_GROUP_ID_1, ORGANISATION_GROUP_ID_2)),
        any(),
        any(RequestPurpose.class));

    assertThat(returnedOrganisationUnitJsons)
        .isEqualTo(List.of(ORG_UNIT_1_JSON, ORG_UNIT_2_JSON));
  }

  @Test
  void getOrganisationUnitsByOrganisationGroupIds_whenNone_verifyApiCallsAndReturn() {
    when(organisationApi.getAllOrganisationGroupsByIds(
        eq(List.of(ORGANISATION_GROUP_ID_1, ORGANISATION_GROUP_ID_2)),
        any(OrganisationGroupsProjectionRoot.class),
        any(RequestPurpose.class)))
        .thenReturn(List.of(
            ORGANISATION_GROUP_1_WITH_EMPTY_ORGANISATION_UNITS,
            ORGANISATION_GROUP_2_WITH_EMPTY_ORGANISATION_UNITS
        ));

    var returnedOrganisationUnitJsons = organisationGroupQueryService
        .getOrganisationUnitsByOrganisationGroupIds(List.of(
            ORGANISATION_GROUP_ID_1,
            ORGANISATION_GROUP_ID_2
        ));

    verify(organisationApi).getAllOrganisationGroupsByIds(
        eq(List.of(ORGANISATION_GROUP_ID_1, ORGANISATION_GROUP_ID_2)),
        any(),
        any(RequestPurpose.class));

    assertThat(returnedOrganisationUnitJsons)
        .isEqualTo(Collections.emptyList());
  }

  @Test
  void getOrganisationUnitsByOrganisationGroupIds_whenNoneWithNullOrgUnits_verifyApiCallsAndReturn() {
    when(organisationApi.getAllOrganisationGroupsByIds(
        eq(List.of(ORGANISATION_GROUP_ID_1)),
        any(OrganisationGroupsProjectionRoot.class),
        any(RequestPurpose.class)))
        .thenReturn(List.of(ORGANISATION_GROUP_1_WITH_EMPTY_ORGANISATION_UNITS));

    var returnedOrganisationUnitJsons = organisationGroupQueryService
        .getOrganisationUnitsByOrganisationGroupIds(List.of(ORGANISATION_GROUP_ID_1));

    verify(organisationApi).getAllOrganisationGroupsByIds(
        eq(List.of(ORGANISATION_GROUP_ID_1)),
        any(),
        any(RequestPurpose.class));

    assertThat(returnedOrganisationUnitJsons)
        .isEqualTo(Collections.emptyList());
  }
}
