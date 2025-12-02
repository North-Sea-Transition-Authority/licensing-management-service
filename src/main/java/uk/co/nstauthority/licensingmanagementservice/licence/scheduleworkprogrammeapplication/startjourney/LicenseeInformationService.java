package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.startjourney;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@Service
public class LicenseeInformationService {

  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceOrganisationService licenceOrganisationService;

  public LicenseeInformationService(LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
                                    LicenceOrganisationService licenceOrganisationService) {
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceOrganisationService = licenceOrganisationService;
  }

  public Map<String, String> getResponsibleOrgUnitOptions(Licence licence, ServiceUserDetail serviceUserDetail) {
    var licenceResponsibleOrganisationIds = licenceResponsibleOrganisationService.getAllByLicence(licence).stream()
        .map(LicenceResponsibleOrganisation::getResponsibleOrganisationId)
        .toList();

    return licenceOrganisationService.getUsersOrgUnits(serviceUserDetail)
        .stream()
        .filter(orgUnit -> licenceResponsibleOrganisationIds.contains(orgUnit.organisationUnitId()))
        .collect(Collectors.toMap(OrganisationUnitJson::getId, OrganisationUnitJson::getName));
  }
}