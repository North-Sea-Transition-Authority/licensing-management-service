package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class LicenceResponsibleOrganisationService {

  private final LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;
  private final PearsResponsibleOrganisationRefreshService pearsResponsibleOrganisationRefreshService;
  private final LicenceOrganisationService licenceOrganisationService;
  private final ApplicationAccessService applicationAccessService;

  public LicenceResponsibleOrganisationService(
      LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository,
      PearsResponsibleOrganisationRefreshService pearsResponsibleOrganisationRefreshService,
      LicenceOrganisationService licenceOrganisationService,
      ApplicationAccessService applicationAccessService
  ) {
    this.licenceResponsibleOrganisationRepository = licenceResponsibleOrganisationRepository;
    this.pearsResponsibleOrganisationRefreshService = pearsResponsibleOrganisationRefreshService;
    this.licenceOrganisationService = licenceOrganisationService;
    this.applicationAccessService = applicationAccessService;
  }

  public List<LicenceResponsibleOrganisation> getAllByLicence(Licence licence) {
    return licenceResponsibleOrganisationRepository.findAllByLicence(licence);
  }

  public List<LicenceResponsibleOrganisation> getAllByLicenceIn(Collection<Licence> licences) {
    return licenceResponsibleOrganisationRepository.findAllByLicenceIn(licences);
  }

  public void refreshPearsResponsibleOrganisations(
      List<Licence> licences,
      Map<Integer, List<Integer>> licenceIdOrgIdMap
  ) {
    pearsResponsibleOrganisationRefreshService.saveResponsibleOrganisationsForLicences(licences, licenceIdOrgIdMap);
    pearsResponsibleOrganisationRefreshService.deleteRemovedResponsibleOrganisationsForLicences(licenceIdOrgIdMap);
  }

  @Transactional
  public void saveLicenseesFromForm(
      Licence licence,
      List<String> organisationUnitIds
  ) {
    // Manually split items to save/delete due to issues with saving and deleting composite keys in the same transaction
    var responsibleOrganisationIdsFromForm = organisationUnitIds.stream()
        .map(Integer::parseInt)
        .toList();

    var existingResponsibleOrganisationIdMap = getAllByLicence(licence).stream()
        .collect(StreamUtil.toLinkedHashMap(LicenceResponsibleOrganisation::getResponsibleOrganisationId, Function.identity()));

    var responsibleOrganisationsToDelete = existingResponsibleOrganisationIdMap.keySet().stream()
        .filter(id -> !responsibleOrganisationIdsFromForm.contains(id))
        .map(existingResponsibleOrganisationIdMap::get)
        .toList();

    var responsibleOrganisationsToSave = responsibleOrganisationIdsFromForm.stream()
        .filter(id -> !existingResponsibleOrganisationIdMap.containsKey(id))
        .map(id -> createManagedLicensee(licence, id))
        .toList();

    licenceResponsibleOrganisationRepository.deleteAll(responsibleOrganisationsToDelete);

    licenceResponsibleOrganisationRepository.flush();

    licenceResponsibleOrganisationRepository.saveAll(responsibleOrganisationsToSave);
  }

  @Transactional
  public void saveLicensees(List<LicenceResponsibleOrganisation> licenceResponsibleOrganisations) {
    licenceResponsibleOrganisationRepository.saveAll(licenceResponsibleOrganisations);
  }

  private LicenceResponsibleOrganisation createManagedLicensee(Licence licence, Integer orgId) {
    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(licence);
    licensee.setResponsibleOrganisationId(orgId);
    licensee.setManagedByLms(true);

    return licensee;
  }

  public Map<String, String> getResponsibleOrgUnitOptionsWithValidRoles(
      Licence licence,
      ServiceUserDetail serviceUserDetail
  ) {

    var licenceResponsibleOrganisationIds = getAllByLicence(licence)
        .stream()
        .map(LicenceResponsibleOrganisation::getResponsibleOrganisationId)
        .toList();

    return licenceOrganisationService
        .getUsersOrgUnits(serviceUserDetail)
        .stream()
        .filter(orgUnit -> licenceResponsibleOrganisationIds.contains(orgUnit.organisationUnitId())
                           && applicationAccessService.userHasEditorOrSubmitterRoleInOrganisationGroup(serviceUserDetail))
        .collect(Collectors.toMap(OrganisationUnitJson::getId, OrganisationUnitJson::getName));
  }

}
