package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.licence.LicenceQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

@Service
public class LicenceScheduledJobService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LicenceScheduledJobService.class);

  private final LicenceQueryService licenceQueryService;
  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceStatusService licenceStatusService;

  public LicenceScheduledJobService(LicenceQueryService licenceQueryService,
                                    LicenceService licenceService,
                                    LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
                                    LicenceStatusService licenceStatusService
  ) {
    this.licenceQueryService = licenceQueryService;
    this.licenceService = licenceService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceStatusService = licenceStatusService;
  }

  @Transactional
  @Scheduled(fixedRateString = "PT1H", initialDelayString = "PT1H")
  public void retrieveAndSavePearsLicences() {

    LOGGER.info("Starting update of PEARS licences and responsible organisations");

    var licenceData = licenceQueryService.getEpaLicenceData();

    var incomingLicenceIds = licenceData.licences().stream().map(Licence::getId).toList();
    var existingLicenceIds = licenceService.getExistingLicenceIds(incomingLicenceIds);

    var savedLicences = (List<Licence>) licenceService.saveLicences(licenceData.licences());

    LOGGER.info("Updated licences from PEARS");

    var currentStatusesByLicenceId = licenceStatusService.getCurrentStatusesByLicenceId(savedLicences);

    savedLicences.forEach(licence -> {
      var incomingStatus = licenceData.licenceIdStatusMap().get(licence.getId());
      var isNewLicence = !existingLicenceIds.contains(licence.getId());
      var hasStatusChanged = !Objects.equals(incomingStatus, currentStatusesByLicenceId.get(licence.getId()));

      if (isNewLicence || hasStatusChanged) {
        licenceStatusService.recordLicenceStatus(licence, incomingStatus);
      }
    });

    licenceResponsibleOrganisationService.refreshPearsResponsibleOrganisations(
        savedLicences,
        licenceData.licenceIdOrgIdMap()
    );

    LOGGER.info("Completed updating PEARS licences and responsible organisations");
  }

}
