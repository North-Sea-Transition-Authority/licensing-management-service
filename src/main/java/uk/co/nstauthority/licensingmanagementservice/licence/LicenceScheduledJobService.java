package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.licence.LicenceQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@Service
public class LicenceScheduledJobService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LicenceScheduledJobService.class);

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
  @Scheduled(fixedRateString = "PT1H")
  public void retrieveAndSavePearsLicences() {

    LOGGER.info("Starting update of PEARS licences and responsible organisations");

    var licenceData = licenceQueryService.getEpaLicenceData();

    var savedLicences = licenceService.saveLicences(licenceData.licences());

    LOGGER.info("Updated licences from PEARS");

    licenceResponsibleOrganisationService.refreshPearsResponsibleOrganisations(
        (List<Licence>) savedLicences,
        licenceData.licenceIdOrgIdMap()
    );

    LOGGER.info("Completed updating PEARS licences and responsible organisations");
  }

}
