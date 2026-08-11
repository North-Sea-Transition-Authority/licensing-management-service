package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

@Service
public class LicenceFormService {

  private final LicenceRepository licenceRepository;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final LicenceService licenceService;
  private final LicenceStatusService licenceStatusService;

  public LicenceFormService(
      LicenceRepository licenceRepository,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      OrganisationUnitQueryService organisationUnitQueryService,
      LicenceService licenceService,
      LicenceStatusService licenceStatusService
  ) {
    this.licenceRepository = licenceRepository;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.licenceService = licenceService;
    this.licenceStatusService = licenceStatusService;
  }

  @Transactional
  public Licence saveNewLicenceFromForm(NewLicenceForm form) {
    var licence = new Licence();
    licence.setId(licenceService.getNextLicenceId());
    licence.setType(form.getLicenceType());
    licence.setPrefix(form.getLicenceType().getPrefix());
    licence.setLicenceNumber(form.getLicenceNumber());
    licence.setLicenceReference(form.getLicenceType().getPrefix() + form.getLicenceNumber());

    var savedLicence = licenceRepository.save(licence);
    licenceStatusService.recordLicenceStatus(
        savedLicence,
        form.getLicenceStatus(),
        form.getLicenceStatusDate().getAsLocalDate().orElseThrow()
    );
    licenceResponsibleOrganisationService.saveLicenseesFromForm(savedLicence, form.getOrganisationUnitIds());

    return savedLicence;
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

  public EditLicenceDetailsForm getEditLicenceDetailsForm(Licence licence) {
    var form = new EditLicenceDetailsForm();
    licenceStatusService.getLatestLicenceStatus(licence).ifPresent(latestLicenceStatus -> {
      form.setLicenceStatus(latestLicenceStatus.getStatus());
      form.getLicenceStatusDate().setDate(latestLicenceStatus.getStatusDate());
    });
    return form;
  }

  @Transactional
  public void saveEditLicenceDetailsFromForm(Licence licence, EditLicenceDetailsForm form) {
    if (form.getLicenceStatus() != licenceStatusService.getCurrentStatus(licence)) {
      licenceStatusService.recordLicenceStatus(
          licence,
          form.getLicenceStatus(),
          form.getLicenceStatusDate().getAsLocalDate().orElseThrow()
      );
    }
    licenceResponsibleOrganisationService.saveLicenseesFromForm(licence, form.getOrganisationUnitIds());
  }
}
