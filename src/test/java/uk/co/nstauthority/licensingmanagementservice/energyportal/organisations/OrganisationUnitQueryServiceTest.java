package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService.ORGANISATION_UNITS_PROJECTION_ROOT;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
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
}