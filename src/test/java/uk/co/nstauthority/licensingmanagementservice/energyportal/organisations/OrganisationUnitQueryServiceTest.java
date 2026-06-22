package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService.ORGANISATION_UNITS_PROJECTION_ROOT;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;

@ExtendWith(MockitoExtension.class)
class OrganisationUnitQueryServiceTest {

  @Mock
  private OrganisationApi organisationApi;

  @InjectMocks
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Test
  void searchOrganisationUnitsByIds() {
    var organisationUnit = new OrganisationUnit();
    organisationUnit.setOrganisationUnitId(1);
    organisationUnit.setName("org name");

    var organisationUnit2 = new OrganisationUnit();
    organisationUnit2.setOrganisationUnitId(2);
    organisationUnit2.setName("org name2");

    var orgUnitJson = new OrganisationUnitJson(
        organisationUnit.getOrganisationUnitId(),
        organisationUnit.getName()
    );

    var orgUnitJson2 = new OrganisationUnitJson(
        organisationUnit2.getOrganisationUnitId(),
        organisationUnit2.getName()
    );

    when(organisationApi.getOrganisationUnitsByIds(
        eq(List.of(1,2)),
        eq(ORGANISATION_UNITS_PROJECTION_ROOT),
        any(),
        any()
        )
    ).thenReturn(List.of(organisationUnit, organisationUnit2));

    assertThat(organisationUnitQueryService.getOrganisationUnitsByIds(List.of(1,2)))
        .usingRecursiveComparison()
        .isEqualTo(List.of(orgUnitJson, orgUnitJson2));
  }

  @Test
  void getOrganisationUnitNamesByIds() {
    var organisationUnit = new OrganisationUnit();
    organisationUnit.setOrganisationUnitId(1);
    organisationUnit.setName("org name");

    var organisationUnit2 = new OrganisationUnit();
    organisationUnit2.setOrganisationUnitId(2);
    organisationUnit2.setName("org name2");

    when(organisationApi.getOrganisationUnitsByIds(
        eq(List.of(1,2)),
        eq(ORGANISATION_UNITS_PROJECTION_ROOT),
        any(),
        any()
        )
    ).thenReturn(List.of(organisationUnit, organisationUnit2));

    assertThat(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(1,2)))
        .usingRecursiveComparison()
        .isEqualTo(Map.of(
            organisationUnit.getOrganisationUnitId(), organisationUnit.getName(),
            organisationUnit2.getOrganisationUnitId(), organisationUnit2.getName()
        ));
  }

  @Test
  void searchOrganisationUnitsWithName() {
    var organisationUnit = new OrganisationUnit();
    organisationUnit.setOrganisationUnitId(1);
    organisationUnit.setName("org name");

    var organisationUnit2 = new OrganisationUnit();
    organisationUnit2.setOrganisationUnitId(2);
    organisationUnit2.setName("org name2");

    var orgUnitJson = new OrganisationUnitJson(
        organisationUnit.getOrganisationUnitId(),
        organisationUnit.getName()
    );

    var orgUnitJson2 = new OrganisationUnitJson(
        organisationUnit2.getOrganisationUnitId(),
        organisationUnit2.getName()
    );

    when(organisationApi.searchOrganisationUnits(
            eq("org name"),
            eq(ORGANISATION_UNITS_PROJECTION_ROOT),
            any(),
            any()
        )
    ).thenReturn(List.of(organisationUnit, organisationUnit2));

    assertThat(organisationUnitQueryService.searchOrganisationUnitsWithName("org name"))
        .usingRecursiveComparison()
        .isEqualTo(List.of(orgUnitJson, orgUnitJson2));
  }

  @Test
  void findOrganisationGroupIdByUnitId() {
    var group = new OrganisationGroup();
    group.setOrganisationGroupId(101);

    var organisationUnit = new OrganisationUnit();
    organisationUnit.setOrganisationGroups(List.of(group));

    when(organisationApi.findOrganisationUnit(
        eq(1),
        eq(OrganisationUnitQueryService.ORGANISATION_UNIT_GROUPS_PROJECTION_ROOT),
        any(),
        any()
    )).thenReturn(Optional.of(organisationUnit));

    assertThat(organisationUnitQueryService.findOrganisationGroupIdByUnitId(1))
        .contains(101);
  }

  @Test
  void findOrganisationGroupIdByUnitId_returnsEmptyWhenNotFound() {
    when(organisationApi.findOrganisationUnit(
        eq(99),
        any(),
        any(),
        any()
    )).thenReturn(Optional.empty());

    assertThat(organisationUnitQueryService.findOrganisationGroupIdByUnitId(99))
        .isEmpty();
  }

  @Test
  void findOrganisationGroupIdsByUnitIds_returnsGroupIdsForAllUnits() {
    var group1 = new OrganisationGroup();
    group1.setOrganisationGroupId(101);
    var group2 = new OrganisationGroup();
    group2.setOrganisationGroupId(202);

    var unit1 = new OrganisationUnit();
    unit1.setOrganisationGroups(List.of(group1));
    var unit2 = new OrganisationUnit();
    unit2.setOrganisationGroups(List.of(group2));

    when(organisationApi.getOrganisationUnitsByIds(eq(List.of(1, 2)), eq(OrganisationUnitQueryService.ORGANISATION_UNITS_GROUPS_PROJECTION_ROOT), any(), any()))
        .thenReturn(List.of(unit1, unit2));

    assertThat(organisationUnitQueryService.findOrganisationGroupIdsByUnitIds(List.of(1, 2)))
        .containsExactlyInAnyOrder(101, 202);
  }

  @Test
  void findOrganisationGroupIdsByUnitIds_skipsUnitsWithNullOrganisationGroups() {
    var unitNoGroup = new OrganisationUnit();

    when(organisationApi.getOrganisationUnitsByIds(eq(List.of(1)), eq(OrganisationUnitQueryService.ORGANISATION_UNITS_GROUPS_PROJECTION_ROOT), any(), any()))
        .thenReturn(List.of(unitNoGroup));

    assertThat(organisationUnitQueryService.findOrganisationGroupIdsByUnitIds(List.of(1))).isEmpty();
  }
}