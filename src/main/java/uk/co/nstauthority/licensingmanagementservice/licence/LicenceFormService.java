package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@Service
public class LicenceFormService {

  private final LicenceRepository licenceRepository;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final LicenceService licenceService;

  public LicenceFormService(
      LicenceRepository licenceRepository,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      OrganisationUnitQueryService organisationUnitQueryService,
      LicenceService licenceService
  ) {
    this.licenceRepository = licenceRepository;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.licenceService = licenceService;
  }

  @Transactional
  public void saveNewLicenceFromForm(NewLicenceForm form) {
    var licence = new Licence();
    licence.setId(licenceService.getNextLicenceId());
    licence.setType(form.getLicenceType());
    licence.setPrefix(form.getLicenceType().getPrefix());
    licence.setLicenceNumber(form.getLicenceNumber());
    licence.setLicenceReference(form.getLicenceType().getPrefix() + form.getLicenceNumber());

    var savedLicence = licenceRepository.save(licence);
    licenceResponsibleOrganisationService.saveLicenseesFromForm(savedLicence, form.getOrganisationUnitIds());
  }

  public List<OrganisationUnitJson> getPreselectedOrganisationUnits(List<String> organisationUnitIds) {
    if (organisationUnitIds == null || organisationUnitIds.isEmpty()) {
      return Collections.emptyList();
    }

    var orgUnitIds = organisationUnitIds.stream()
        .map(Integer::valueOf)
        .toList();

    return organisationUnitQueryService.getOrganisationUnitsByIds(orgUnitIds);
  }

  public List<OrganisationUnitJson> getSavedOrganisationUnits(Licence licence) {
    var orgUnitIds = licenceResponsibleOrganisationService.getAllByLicence(licence).stream()
        .map(LicenceResponsibleOrganisation::getResponsibleOrganisationId)
        .map(String::valueOf)
        .toList();
    return getPreselectedOrganisationUnits(orgUnitIds);
  }
}
