package uk.co.nstauthority.licensingmanagementservice.energyportal.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.client.licence.licence.LicenceApi;
import uk.co.fivium.energyportalapi.client.licence.licence.LicenceSearchFilter;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSubtype;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;

@ExtendWith(MockitoExtension.class)
class LicenceQueryServiceTest {

  @Mock
  private LicenceApi licenceApi;

  @InjectMocks
  private LicenceQueryService licenceQueryService;

  private OrganisationUnit organisationUnit;
  private OrganisationUnit organisationUnit2;

  @BeforeEach
  void setUp() {
    organisationUnit = OrganisationUnitTestUtil.ORG_UNIT_1;
    organisationUnit2 = OrganisationUnitTestUtil.ORG_UNIT_2;
  }

  @Test
  void getLicencesLicenseesMap() {
    var licence = new Licence();
    licence.setId(1);
    licence.setType(LicenceType.SEAWARD_PRODUCTION);
    licence.setSubtype(LicenceSubtype.FRONTIER);
    licence.setPrefix("P");
    licence.setLicenceNumber("1");

    var licence2 = new Licence();
    licence2.setId(2);
    licence2.setType(LicenceType.LANDWARD_PRODUCTION);
    licence2.setSubtype(null);
    licence2.setPrefix("PEDL");
    licence2.setLicenceNumber("2");

    when(licenceApi.searchLicences(
        any(LicenceSearchFilter.class),
        eq(LicenceQueryService.LICENCE_PROJECTION_ROOT),
        any(),
        any()
    )).thenReturn(createPortalLicences());

    var expectedResult = Map.of(
        licence, List.of(organisationUnit, organisationUnit2),
        licence2, List.of(organisationUnit)
    );

    var result = licenceQueryService.getLicencesLicenseesMap();

    assertThat(result.keySet()).usingRecursiveComparison().isEqualTo(expectedResult.keySet());
    assertThat(result.values()).usingRecursiveComparison().isEqualTo(expectedResult.values());
  }

  private List<uk.co.fivium.energyportalapi.generated.types.Licence> createPortalLicences() {
    var portalLicence = new uk.co.fivium.energyportalapi.generated.types.Licence(
        1,
        "P",
        "frontier",
        1,
        "p1",
        null,
        null,
        null,
        null,
        null,
        List.of(organisationUnit, organisationUnit2)
    );

    var portalLicence2 = new uk.co.fivium.energyportalapi.generated.types.Licence(
        2,
        "PEDL",
        null,
        2,
        "PEDL2",
        null,
        null,
        null,
        null,
        null,
        List.of(organisationUnit)
    );

    return List.of(portalLicence, portalLicence2);
  }
}