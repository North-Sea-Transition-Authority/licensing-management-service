package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.licence.LicenceQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@Service
public class LicenceScheduledJobService {

  private final LicenceQueryService licenceQueryService;
  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  public LicenceScheduledJobService(LicenceQueryService licenceQueryService,
                                    LicenceService licenceService,
                                    LicenceResponsibleOrganisationService licenceResponsibleOrganisationService
  ) {
    this.licenceQueryService = licenceQueryService;
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
  }

  @Transactional
  @Scheduled(cron = "0 0 * * * *")
  public void retrieveAndSavePearsLicences() {
    var licenceData = licenceQueryService.getEpaLicenceData();

    var savedLicences = licenceService.saveLicences(licenceData.licences());

    licenceResponsibleOrganisationService.refreshPearsResponsibleOrganisations(
        (List<Licence>) savedLicences,
        licenceData.licenceIdOrgIdMap()
    );
  }

}
