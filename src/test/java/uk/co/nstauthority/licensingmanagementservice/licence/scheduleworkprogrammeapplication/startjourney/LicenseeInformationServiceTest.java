package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@ExtendWith(MockitoExtension.class)
class LicenseeInformationServiceTest {

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private LicenceOrganisationService licenceOrganisationService;

  @InjectMocks
  private LicenseeInformationService licenseeInformationService;

  @Mock
  private ServiceUserDetail serviceUserDetail;

  @Test
  void getResponsibleOrgUnitOptions_returnsOnlyMatchingOrgUnits() {
    Licence licence = new Licence();
    LicenceResponsibleOrganisation lro = createLicenceResponsibleOrganisation(2);
    when(licenceResponsibleOrganisationService.getAllByLicence(licence)).thenReturn(List.of(lro));

    OrganisationUnitJson ou1 = new OrganisationUnitJson(2, "Org Two");
    OrganisationUnitJson ou2 = new OrganisationUnitJson(3, "Org Three");
    when(licenceOrganisationService.getUsersOrgUnits(serviceUserDetail)).thenReturn(List.of(ou1, ou2));

    var result = licenseeInformationService.getResponsibleOrgUnitOptions(licence, serviceUserDetail);

    assertThat(result)
        .hasSize(1)
        .containsEntry("2", "Org Two");
  }

  @Test
  void getResponsibleOrgUnitOptions_returnsEmptyWhenNoMatches() {
    Licence licence = new Licence();
    LicenceResponsibleOrganisation lro = createLicenceResponsibleOrganisation(99);
    when(licenceResponsibleOrganisationService.getAllByLicence(licence)).thenReturn(List.of(lro));

    OrganisationUnitJson ou1 = new OrganisationUnitJson(2, "Org Two");
    OrganisationUnitJson ou2 = new OrganisationUnitJson(3, "Org Three");
    when(licenceOrganisationService.getUsersOrgUnits(serviceUserDetail)).thenReturn(List.of(ou1, ou2));

    var result = licenseeInformationService.getResponsibleOrgUnitOptions(licence, serviceUserDetail);

    assertThat(result).isEmpty();
  }

  private LicenceResponsibleOrganisation createLicenceResponsibleOrganisation(int responsibleOrganisationId) {
    LicenceResponsibleOrganisation lro = new LicenceResponsibleOrganisation();
    lro.setResponsibleOrganisationId(responsibleOrganisationId);
    return lro;
  }
}