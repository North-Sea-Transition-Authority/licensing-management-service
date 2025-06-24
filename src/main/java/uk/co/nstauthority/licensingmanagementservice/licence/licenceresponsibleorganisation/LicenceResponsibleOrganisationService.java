package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Service
public class LicenceResponsibleOrganisationService {

  private final LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;

  public LicenceResponsibleOrganisationService(
      LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository
  ) {
    this.licenceResponsibleOrganisationRepository = licenceResponsibleOrganisationRepository;
  }

  @Transactional
  public void refreshPearsResponsibleOrganisations(
      List<Licence> licences,
      Map<Integer, List<Integer>> licenceIdOrgIdMap
  ) {
    var oldOrganisations = licenceResponsibleOrganisationRepository.findAllByManagedByLmsIsFalse();
    licenceResponsibleOrganisationRepository.deleteAll(oldOrganisations);
    licenceResponsibleOrganisationRepository.flush();

    saveResponsibleOrganisationsForLicences(licences, licenceIdOrgIdMap);
  }

  private void saveResponsibleOrganisationsForLicences(
      List<Licence> licences,
      Map<Integer, List<Integer>> licenceIdOrgIdMap
  ) {
    var responsibleOrganisations = licences.stream()
        .map(licence ->  createLicenseesForPearsLicence(licence, licenceIdOrgIdMap.get(licence.getId())))
        .flatMap(List::stream)
        .toList();

    licenceResponsibleOrganisationRepository.saveAll(responsibleOrganisations);
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
