package uk.co.nstauthority.licensingmanagementservice.licence;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.licence.EpaLicenceDataDto;
import uk.co.nstauthority.licensingmanagementservice.energyportal.licence.LicenceQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

/**
 * Brings the LMS licences, their statuses and their responsible organisations back into line with what the Energy
 * Portal holds for PEARS.
 *
 * <p>Refreshing every licence is the hourly job's job; refreshing one licence is what an incoming PEARS message
 * needs, so that the message can be acted on against current data rather than against whatever the last hourly run
 * happened to leave behind.</p>
 */
@Service
public class PearsLicenceRefreshService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsLicenceRefreshService.class);

  private final LicenceQueryService licenceQueryService;
  private final LicenceService licenceService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceStatusService licenceStatusService;

  public PearsLicenceRefreshService(
      LicenceQueryService licenceQueryService,
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
  public void refreshAllLicences() {
    var licenceData = licenceQueryService.getEpaLicenceData();
    var savedLicences = saveLicencesAndRecordStatusChanges(licenceData);

    licenceResponsibleOrganisationService
        .refreshPearsResponsibleOrganisations(savedLicences, licenceData.licenceIdOrgIdMap());
  }

  /**
   * Refreshes one licence, leaving every other licence untouched.
   *
   * @return the refreshed licence, or empty when the Energy Portal does not hold the licence - in which case nothing
   *     is changed, so that an Energy Portal that has not yet caught up cannot strip a licence LMS already holds
   */
  @Transactional
  public Optional<Licence> refreshLicence(Integer licenceId) {
    var licenceData = licenceQueryService.getEpaLicenceData(List.of(licenceId));

    if (licenceData.licences().isEmpty()) {
      LOGGER.warn("The Energy Portal holds no licence {}, so it has not been refreshed", licenceId);
      return Optional.empty();
    }

    var savedLicences = saveLicencesAndRecordStatusChanges(licenceData);

    savedLicences.forEach(licence -> licenceResponsibleOrganisationService.refreshPearsResponsibleOrganisationsForLicence(
        licence,
        licenceData.licenceIdOrgIdMap().getOrDefault(licence.getId(), List.of())
    ));

    LOGGER.info("Refreshed licence {} and its responsible organisations", licenceId);

    return savedLicences.stream().findFirst();
  }

  private List<Licence> saveLicencesAndRecordStatusChanges(EpaLicenceDataDto licenceData) {
    var incomingLicenceIds = licenceData.licences().stream().map(Licence::getId).toList();
    var existingLicenceIds = licenceService.getExistingLicenceIds(incomingLicenceIds);

    var savedLicences = licenceService.saveLicences(licenceData.licences());

    var currentStatusesByLicenceId = licenceStatusService.getCurrentStatusesByLicenceId(savedLicences);

    savedLicences.forEach(licence -> {
      var incomingStatus = licenceData.licenceIdStatusMap().get(licence.getId());
      var isNewLicence = !existingLicenceIds.contains(licence.getId());
      var hasStatusChanged = !Objects.equals(incomingStatus, currentStatusesByLicenceId.get(licence.getId()));

      if (isNewLicence || hasStatusChanged) {
        licenceStatusService.recordLicenceStatus(licence, incomingStatus);
      }
    });

    return savedLicences;
  }
}
