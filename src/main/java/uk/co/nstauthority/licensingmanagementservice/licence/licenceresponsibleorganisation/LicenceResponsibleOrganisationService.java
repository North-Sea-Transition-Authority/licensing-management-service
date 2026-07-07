package uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
public class LicenceResponsibleOrganisationService {

  private final LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository;
  private final PearsResponsibleOrganisationRefreshService pearsResponsibleOrganisationRefreshService;
  private final LicenceOrganisationService licenceOrganisationService;
  private final ApplicationAccessService applicationAccessService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  public LicenceResponsibleOrganisationService(
      LicenceResponsibleOrganisationRepository licenceResponsibleOrganisationRepository,
      PearsResponsibleOrganisationRefreshService pearsResponsibleOrganisationRefreshService,
      LicenceOrganisationService licenceOrganisationService,
      ApplicationAccessService applicationAccessService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.licenceResponsibleOrganisationRepository = licenceResponsibleOrganisationRepository;
    this.pearsResponsibleOrganisationRefreshService = pearsResponsibleOrganisationRefreshService;
    this.licenceOrganisationService = licenceOrganisationService;
    this.applicationAccessService = applicationAccessService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  public List<LicenceResponsibleOrganisation> getAllByLicence(Licence licence) {
    return licenceResponsibleOrganisationRepository.findAllByLicence(licence);
  }

  public List<LicenceResponsibleOrganisation> getAllByLicenceIn(Collection<Licence> licences) {
    return licenceResponsibleOrganisationRepository.findAllByLicenceIn(licences);
  }

  public List<LicenceResponsibleOrganisation> getAllByResponsibleOrganisationIdIn(
      Collection<Integer> responsibleOrganisationIds
  ) {
    return licenceResponsibleOrganisationRepository.findAllByResponsibleOrganisationIdIn(responsibleOrganisationIds);
  }

  public LicenceResponsibleOrganisation getByLicenceIdAndResponsibleOrganisationIdOrThrow(
      Integer licenceId,
      Integer responsibleOrganisationId
  ) {
    return licenceResponsibleOrganisationRepository
        .findByLicence_IdAndResponsibleOrganisationId(licenceId, responsibleOrganisationId)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "licence responsible organisation", "%d/%d".formatted(licenceId, responsibleOrganisationId)));
  }

  public Map<Licence, List<OrganisationUnit>> getResponsibleOrganisationsByLicences(Collection<Licence> licences) {
    var licenceResponsibleOrganisations = getAllByLicenceIn(licences);

    var responsibleOrganisationIds = licenceResponsibleOrganisations.stream()
        .map(LicenceResponsibleOrganisation::getResponsibleOrganisationId)
        .distinct()
        .toList();

    var organisationUnitNames = organisationUnitQueryService.getOrganisationUnitNamesByIds(responsibleOrganisationIds);

    return licenceResponsibleOrganisations.stream()
        .map(lro -> {
          var orgUnitId = lro.getResponsibleOrganisationId();
          var orgUnitName = organisationUnitNames.get(orgUnitId);
          if (orgUnitName == null) {
            return null;
          }

          return Map.entry(lro.getLicence(), new OrganisationUnit(orgUnitId, orgUnitName));
        })
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(
            Map.Entry::getKey,
            Collectors.mapping(Map.Entry::getValue, Collectors.toList())
        ));
  }

  private Map<Integer, Integer> getOrgUnitToGroupIdMap(Collection<Integer> orgUnitIds) {
    return organisationUnitQueryService.findOrganisationGroupIdMapByUnitIds(List.copyOf(orgUnitIds));
  }

  public Map<Integer, Integer> getOrgUnitToGroupIdMap(Licence licence) {
    if (licence == null) {
      return Map.of();
    }
    var responsibleOrganisations = getResponsibleOrganisationsByLicences(List.of(licence));
    var orgUnitIds = getOrganisationUnitIdsFromLicenceOrgUnitMap(responsibleOrganisations, licence);
    return getOrgUnitToGroupIdMap(orgUnitIds);
  }

  public Map<Integer, Integer> getOrgUnitToGroupIdMap(
      Map<Licence, List<OrganisationUnit>> responsibleOrganisations,
      List<? extends LicenceApplicationDetail> applicationDetails
  ) {
    var allOrgUnitIds = Stream.concat(
            responsibleOrganisations.values().stream()
                .flatMap(List::stream)
                .map(OrganisationUnit::organisationUnitId),
            applicationDetails.stream()
                .map(LicenceApplicationDetail::getResponsibleOrganisationUnitId)
                .filter(Objects::nonNull)
        )
        .distinct()
        .toList();

    return getOrgUnitToGroupIdMap(allOrgUnitIds);
  }

  public List<Integer> getOrganisationUnitIdsFromLicenceOrgUnitMap(
      Map<Licence, List<OrganisationUnit>> responsibleOrganisations,
      Licence licence
  ) {
    return responsibleOrganisations.entrySet().stream()
        .filter(entry -> entry.getKey().equals(licence))
        .flatMap(entry -> entry.getValue().stream())
        .map(OrganisationUnit::organisationUnitId)
        .toList();
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
