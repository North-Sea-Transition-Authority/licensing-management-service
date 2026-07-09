package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContactRepository;

@Service
public class PearsResponsibleOrganisationRefreshService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PearsResponsibleOrganisationRefreshService.class);

  private final LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;
  private final LicenceContactRepository licenceContactRepository;

  public PearsResponsibleOrganisationRefreshService(
      LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository,
      LicenceContactRepository licenceContactRepository) {
    this.licenceResponsibleOrganisationRepository = licenceResponsibleOrganisationRepository;
    this.licenceContactRepository = licenceContactRepository;
  }

  @Transactional
  public void deleteRemovedResponsibleOrganisationsForLicences(Map<Integer, List<Integer>> licenceIdOrgIdMap) {
    // Convert the licenceIdOrgIdMap to a flat list of LicenceOrganisationDto
    List<LicenceOrganisationDto> licenceOrgsFlatList =
        licenceIdOrgIdMap.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(orgId -> new LicenceOrganisationDto(entry.getKey(), orgId)))
            .toList();

    // Find all existing organisations that are not managed by LMS
    var existingOrganisations = licenceResponsibleOrganisationRepository.findAllByManagedByLmsIsFalse();

    // Find organisations that are in the database but not in the provided licenceIdOrgIdMap
    var removedOrganisations = existingOrganisations.stream()
        .filter(org -> licenceOrgsFlatList.stream()
            .noneMatch(licenceOrg -> Objects.equals(licenceOrg.licenceId(), org.getLicence().getId())
                && Objects.equals(licenceOrg.responsibleOrganisationId(), org.getResponsibleOrganisationId())))
        .toList();

    var removedContacts = licenceContactRepository.findAllByLicenseeIn(removedOrganisations);
    licenceContactRepository.deleteAll(removedContacts);

    // Delete the removed organisations
    licenceResponsibleOrganisationRepository.deleteAll(removedOrganisations);
    LOGGER.info("Deleted {} removed responsible organisations", removedOrganisations.size());
    licenceResponsibleOrganisationRepository.flush();
  }

  @Transactional
  public void saveResponsibleOrganisationsForLicences(
      List<Licence> licences,
      Map<Integer, List<Integer>> licenceIdOrgIdMap
  ) {
    var responsibleOrganisations = licences.stream()
        .map(licence -> createLicenseesForPearsLicence(licence, licenceIdOrgIdMap.get(licence.getId())))
        .flatMap(List::stream)
        .toList();

    licenceResponsibleOrganisationRepository.saveAll(responsibleOrganisations);
    LOGGER.info("Updated PEARS responsible organisations");
  }

  private List<LicenceResponsibleOrganisation> createLicenseesForPearsLicence(
      Licence licence,
      List<Integer> orgIds
  ) {
    return orgIds.stream()
        .map(id -> createPearsLicensee(licence, id))
        .toList();
  }

  private LicenceResponsibleOrganisation createPearsLicensee(Licence licence, Integer orgId) {
    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(licence);
    licensee.setResponsibleOrganisationId(orgId);
    licensee.setManagedByLms(false);
    return licensee;
  }
}
