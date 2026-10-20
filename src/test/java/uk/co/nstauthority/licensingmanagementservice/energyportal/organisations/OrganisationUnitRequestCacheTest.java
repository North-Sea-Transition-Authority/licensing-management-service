package uk.co.nstauthority.licensingmanagementservice.energyportal.organisations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService.ORGANISATION_UNITS_PROJECTION_ROOT;
import static uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRequestCache.GET_ORGANISATION_UNITS_BY_IDS_PURPOSE;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;

@ExtendWith(MockitoExtension.class)
class OrganisationUnitRequestCacheTest {

  @Mock
  private OrganisationApi organisationApi;

  @InjectMocks
  private OrganisationUnitRequestCache organisationUnitRequestCache;

  @BeforeEach
  @AfterEach
  void resetRequestAttributes() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void getOrganisationUnitsByIds_whenSomeIdsNotReturned_thenReturnsTheUnitsFoundInTheOrderAsked() {
    var firstUnit = organisationUnit(1);
    var thirdUnit = organisationUnit(3);
    when(organisationApi.getOrganisationUnitsByIds(
        eq(List.of(3, 2, 1)),
        eq(ORGANISATION_UNITS_PROJECTION_ROOT),
        eq(GET_ORGANISATION_UNITS_BY_IDS_PURPOSE),
        any()
    )).thenReturn(List.of(firstUnit, thirdUnit));

    var result = organisationUnitRequestCache.getOrganisationUnitsByIds(List.of(3, 2, 1));

    assertThat(result).containsExactly(thirdUnit, firstUnit);
  }

  @Test
  void getOrganisationUnitsByIds_whenInWebRequest_thenOnlyFetchesIdsNotAlreadyHeld() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    var firstUnit = organisationUnit(1);
    var secondUnit = organisationUnit(2);
    when(organisationApi.getOrganisationUnitsByIds(
        eq(List.of(1)),
        eq(ORGANISATION_UNITS_PROJECTION_ROOT),
        eq(GET_ORGANISATION_UNITS_BY_IDS_PURPOSE),
        any()
    )).thenReturn(List.of(firstUnit));
    when(organisationApi.getOrganisationUnitsByIds(
        eq(List.of(2, 99)),
        eq(ORGANISATION_UNITS_PROJECTION_ROOT),
        eq(GET_ORGANISATION_UNITS_BY_IDS_PURPOSE),
        any()
    )).thenReturn(List.of(secondUnit));
    organisationUnitRequestCache.getOrganisationUnitsByIds(List.of(1));

    var result = organisationUnitRequestCache.getOrganisationUnitsByIds(List.of(1, 2, 99));
    var repeatedResult = organisationUnitRequestCache.getOrganisationUnitsByIds(List.of(99, 2, 1));

    assertThat(result).containsExactly(firstUnit, secondUnit);
    assertThat(repeatedResult).containsExactly(secondUnit, firstUnit);
    verify(organisationApi, times(1)).getOrganisationUnitsByIds(
        eq(List.of(2, 99)),
        eq(ORGANISATION_UNITS_PROJECTION_ROOT),
        eq(GET_ORGANISATION_UNITS_BY_IDS_PURPOSE),
        any()
    );
  }

  private static OrganisationUnit organisationUnit(int organisationUnitId) {
    var organisationUnit = new OrganisationUnit();
    organisationUnit.setOrganisationUnitId(organisationUnitId);
    organisationUnit.setName("Organisation " + organisationUnitId);
    return organisationUnit;
  }
}
