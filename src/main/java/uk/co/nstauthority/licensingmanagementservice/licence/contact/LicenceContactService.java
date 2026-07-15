package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableRow;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableValue;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableView;
import uk.co.nstauthority.licensingmanagementservice.fds.table.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.table.TagColour;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;

@Service
public class LicenceContactService {

  private final LicenceContactRepository licenceContactRepository;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final LicenceOrganisationService licenceOrganisationService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final OrganisationGroupQueryService organisationGroupQueryService;

  public LicenceContactService(
      LicenceContactRepository licenceContactRepository,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      LicenceOrganisationService licenceOrganisationService,
      OrganisationUnitQueryService organisationUnitQueryService,
      OrganisationGroupQueryService organisationGroupQueryService
  ) {
    this.licenceContactRepository = licenceContactRepository;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.licenceOrganisationService = licenceOrganisationService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.organisationGroupQueryService = organisationGroupQueryService;
  }

  public LicenceContactsTableView getIndustryContactsTable(
      ServiceUserDetail user,
      boolean canManage,
      LicenceContactFilterForm filterForm
  ) {
    var usersOrgUnits = licenceOrganisationService.getUsersOrgUnits(user);
    var nameByOrgUnitId = usersOrgUnits.stream()
        .collect(Collectors.toMap(OrganisationUnitJson::organisationUnitId, OrganisationUnitJson::name, (a, b) -> a));
    var orgUnitIds = nameByOrgUnitId.keySet();
    var licensees = licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(orgUnitIds);
    return buildContactsTable(licensees, nameByOrgUnitId, orgUnitIds, canManage, filterForm);
  }

  public LicenceContactsTableView getRegulatorContactsTable(LicenceContactFilterForm filterForm) {
    var licensees = licenceResponsibleOrganisationService.getAll();
    var orgUnitIds = licensees.stream()
        .map(LicenceResponsibleOrganisation::getResponsibleOrganisationId)
        .distinct()
        .toList();
    var nameByOrgUnitId = organisationUnitQueryService.getOrganisationUnitNamesByIds(orgUnitIds);
    return buildContactsTable(licensees, nameByOrgUnitId, orgUnitIds, false, filterForm);
  }

  private LicenceContactsTableView buildContactsTable(
      List<LicenceResponsibleOrganisation> licensees,
      Map<Integer, String> nameByOrgUnitId,
      Collection<Integer> orgUnitIds,
      boolean canManage,
      LicenceContactFilterForm filterForm
  ) {
    var tableBuilder = SortableTableView.sortableTableBuilder()
        .newWithHeadings("Licence", "Licensee", "Contact email");

    if (canManage) {
      tableBuilder.withActionHeading("Action");
    }

    var emailByLicensee = licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(orgUnitIds)
        .stream()
        .collect(Collectors.toMap(LicenceContact::getLicensee, LicenceContact::getContactEmail));

    var groupOrgUnitIds = getOrgUnitIdsForGroup(filterForm.getLicenseeOrgGroupId());
    var filteredLicensees = licensees.stream()
        .filter(licensee -> matchesFilter(licensee, emailByLicensee, groupOrgUnitIds, filterForm))
        .toList();

    filteredLicensees
        .forEach(licensee -> tableBuilder.addRow(toRow(licensee, nameByOrgUnitId, emailByLicensee, canManage)));

    return new LicenceContactsTableView(tableBuilder.build().toString(), filteredLicensees.size());
  }

  private boolean matchesFilter(
      LicenceResponsibleOrganisation licensee,
      Map<LicenceResponsibleOrganisation, String> emailByLicensee,
      List<Integer> groupOrgUnitIds,
      LicenceContactFilterForm filterForm
  ) {
    var licenseeOrgUnitIds = List.of(licensee.getResponsibleOrganisationId());
    var contactEmail = Objects.toString(emailByLicensee.get(licensee), "");
    return FilterUtil.matchesTextInput(licensee.getLicence().getLicenceReference(), filterForm.getLicenceReference())
        && FilterUtil.matchesIdList(licenseeOrgUnitIds, filterForm.getLicenseeOrgUnitId())
        && FilterUtil.listMatchesIdList(licenseeOrgUnitIds, groupOrgUnitIds)
        && FilterUtil.matchesTextInput(contactEmail, filterForm.getContactEmail())
        && (!Boolean.TRUE.equals(filterForm.getNoContactAssigned()) || StringUtils.isBlank(contactEmail));
  }

  private List<Integer> getOrgUnitIdsForGroup(Integer organisationGroupId) {
    if (organisationGroupId == null) {
      return null;
    }

    return organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of(organisationGroupId))
        .stream()
        .map(OrganisationUnitJson::organisationUnitId)
        .toList();
  }

  Map<String, String> getPreselectedOrganisationUnit(Integer organisationUnitId) {
    if (organisationUnitId == null) {
      return Collections.emptyMap();
    }

    return organisationUnitQueryService.getOrganisationUnitNamesByIds(Collections.singletonList(organisationUnitId))
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().toString(),
            Map.Entry::getValue
        ));
  }

  Map<String, String> getPreselectedOrganisationGroup(Integer organisationGroupId) {
    if (organisationGroupId == null) {
      return Collections.emptyMap();
    }

    return organisationGroupQueryService.getOrganisationGroupById(organisationGroupId)
        .map(group -> Map.of(group.getOrganisationGroupId().toString(), group.getOrganisationGroupName()))
        .orElse(Collections.emptyMap());
  }

  private SortableTableRow toRow(
      LicenceResponsibleOrganisation licensee,
      Map<Integer, String> nameByOrgUnitId,
      Map<LicenceResponsibleOrganisation, String> emailByLicensee,
      boolean canManage
  ) {
    var licenceId = licensee.getLicence().getId();
    var organisationId = licensee.getResponsibleOrganisationId();
    var licenceReference = licensee.getLicence().getLicenceReference();
    var licenseeName = nameByOrgUnitId.getOrDefault(organisationId, "");
    var contactEmail = emailByLicensee.get(licensee);
    var hasContact = StringUtils.isNotBlank(contactEmail);

    var emailValue = hasContact
        ? new SortableTableValue(contactEmail)
        : new SortableTableValue("", List.of(new Tag("Not assigned", TagColour.GREY)));

    var rowBuilder = SortableTableRow.builder()
        .withValue(licenceReference)
        .withValue(licenseeName)
        .withValue(emailValue);

    if (canManage) {
      var actionLabel = hasContact ? "Update contact email" : "Add contact email";
      var addUpdateContactUrl = StringUtils.removeStart(
          ReverseRouter.route(on(LicenceContactController.class).renderUpdateContact(licenceId, organisationId, null)),
          "/");
      rowBuilder.withAction(actionLabel, addUpdateContactUrl, "for %s".formatted(licenceReference));
    }

    return rowBuilder.build();
  }

  public LicenceContactFormView getLicenceContactFormView(ServiceUserDetail user, Integer licenceId, Integer organisationId) {
    licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, organisationId);
    var licensee = licenceResponsibleOrganisationService
        .getByLicenceIdAndResponsibleOrganisationIdOrThrow(licenceId, organisationId);
    var currentEmail = licenceContactRepository.findByLicensee(licensee)
        .map(LicenceContact::getContactEmail)
        .orElse(null);
    return new LicenceContactFormView(licensee.getLicence().getLicenceReference(), currentEmail);
  }

  @Transactional
  public void saveContact(ServiceUserDetail user, Integer licenceId, Integer organisationId, String contactEmail) {
    licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, organisationId);
    var licensee = licenceResponsibleOrganisationService
        .getByLicenceIdAndResponsibleOrganisationIdOrThrow(licenceId, organisationId);

    var contact = licenceContactRepository.findByLicensee(licensee).orElseGet(LicenceContact::new);
    contact.setLicensee(licensee);
    contact.setContactEmail(contactEmail);
    licenceContactRepository.save(contact);
  }

  public List<BulkContactCandidate> getOtherLicencesHeldByLicensee(
      ServiceUserDetail user,
      Integer currentLicenceId,
      Integer organisationId
  ) {
    var licenseeName = licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, organisationId);

    var emailByLicensee = licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(organisationId))
        .stream()
        .collect(Collectors.toMap(LicenceContact::getLicensee, LicenceContact::getContactEmail));

    return licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(organisationId)).stream()
        .filter(licensee -> !licensee.getLicence().getId().equals(currentLicenceId))
        .map(licensee -> new BulkContactCandidate(
            licensee.getLicence().getId(),
            licensee.getLicence().getLicenceReference(),
            licenseeName,
            emailByLicensee.get(licensee)))
        .toList();
  }

  public List<Integer> getAllLicenceIdsHeldByLicensee(ServiceUserDetail user, Integer currentLicenceId, Integer organisationId) {
    var otherLicenceIds = getOtherLicencesHeldByLicensee(user, currentLicenceId, organisationId)
        .stream()
        .map(BulkContactCandidate::licenceId)
        .toList();

    var licenceIdsHeld = new ArrayList<Integer>();
    licenceIdsHeld.add(currentLicenceId);
    licenceIdsHeld.addAll(otherLicenceIds);
    return licenceIdsHeld;
  }

  @Transactional
  public void applyContactToLicences(
      ServiceUserDetail user,
      Integer organisationId,
      String contactEmail,
      Collection<Integer> licenceIds
  ) {
    licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, organisationId);

    licenceIds.forEach(licenceId -> {
      var licensee = licenceResponsibleOrganisationService
          .getByLicenceIdAndResponsibleOrganisationIdOrThrow(licenceId, organisationId);
      var contact = licenceContactRepository.findByLicensee(licensee).orElseGet(LicenceContact::new);
      contact.setLicensee(licensee);
      contact.setContactEmail(contactEmail);
      licenceContactRepository.save(contact);
    });
  }
}
