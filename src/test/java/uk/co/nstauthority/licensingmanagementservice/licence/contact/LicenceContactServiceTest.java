package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupDto;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;

@ExtendWith(MockitoExtension.class)
class LicenceContactServiceTest {

  private static final Integer LICENCE_ID = 1;
  private static final Integer ORG_ID = 10;
  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(LICENCE_ID)
      .withLicenceReference("P 123")
      .build();
  public static final String SHELL_U_K_LIMITED = "Shell U.K. Limited";
  private static final OrganisationUnitJson ORG_UNIT = new OrganisationUnitJson(ORG_ID, SHELL_U_K_LIMITED);

  private final ServiceUserDetail user = ServiceUserDetailTestUtil.newBuilder().build();

  @Mock
  private LicenceContactRepository licenceContactRepository;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private LicenceOrganisationService licenceOrganisationService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @InjectMocks
  private LicenceContactService licenceContactService;

  @Captor
  private ArgumentCaptor<LicenceContact> contactCaptor;

  @Test
  void getIndustryContactsTable_whenUserHasNoOrgUnits_returnsEmptyTable() {
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of());

    var tableView = licenceContactService.getIndustryContactsTable(user, true, new LicenceContactFilterForm());

    assertThat(tableView.tableJson()).doesNotContain("licence-contacts/");
    assertThat(tableView.contactCount()).isZero();
  }

  @Test
  void getIndustryContactsTable_whenLicenseeHasNoContact_showsNotAssigned() {
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of(ORG_UNIT));
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(licensee()));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of());

    var tableView = licenceContactService.getIndustryContactsTable(user, true, new LicenceContactFilterForm());

    assertThat(tableView.tableJson())
        .contains("P 123")
        .contains(SHELL_U_K_LIMITED)
        .contains("Not assigned")
        .contains("\"licence-contacts/licence/%d/responsible-organisation/%d\"".formatted(LICENCE_ID, ORG_ID));
    assertThat(tableView.contactCount()).isEqualTo(1);
  }

  @Test
  void getIndustryContactsTable_whenLicenseeHasContact_showsEmail() {
    var licensee = licensee();
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of(ORG_UNIT));
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(licensee));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(contact(licensee, "licensing@example.com")));

    var tableView = licenceContactService.getIndustryContactsTable(user, true, new LicenceContactFilterForm());

    assertThat(tableView.tableJson())
        .contains("licensing@example.com")
        .doesNotContain("Not assigned");
  }

  @Test
  void getIndustryContactsTable_whenUserCannotManage_hasNoActionLink() {
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of(ORG_UNIT));
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(licensee()));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of());

    var tableView = licenceContactService.getIndustryContactsTable(user, false, new LicenceContactFilterForm());

    assertThat(tableView.tableJson())
        .contains("P 123")
        .doesNotContain("licence-contacts/");
  }

  @Test
  void getRegulatorContactsTable_whenNoLicensees_returnsEmptyTable() {
    when(licenceResponsibleOrganisationService.getAll()).thenReturn(List.of());

    var tableView = licenceContactService.getRegulatorContactsTable(new LicenceContactFilterForm());

    assertThat(tableView.tableJson()).doesNotContain("P 123");
    assertThat(tableView.contactCount()).isZero();
  }

  @Test
  void getRegulatorContactsTable_whenLicenseeHasContact_showsEmailAndNoActionColumn() {
    var licensee = licensee();
    when(licenceResponsibleOrganisationService.getAll()).thenReturn(List.of(licensee));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_ID)))
        .thenReturn(Map.of(ORG_ID, SHELL_U_K_LIMITED));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(List.of(ORG_ID)))
        .thenReturn(List.of(contact(licensee, "licensing@example.com")));

    var tableView = licenceContactService.getRegulatorContactsTable(new LicenceContactFilterForm());

    assertThat(tableView.tableJson())
        .contains("P 123")
        .contains(SHELL_U_K_LIMITED)
        .contains("licensing@example.com")
        .doesNotContain("Not assigned");
    assertThat(tableView.contactCount()).isEqualTo(1);
  }

  @Test
  void getRegulatorContactsTable_whenLicenseeHasNoContact_showsNotAssigned() {
    var licensee = licensee();
    when(licenceResponsibleOrganisationService.getAll()).thenReturn(List.of(licensee));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_ID)))
        .thenReturn(Map.of(ORG_ID, SHELL_U_K_LIMITED));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(List.of(ORG_ID)))
        .thenReturn(List.of());

    var tableView = licenceContactService.getRegulatorContactsTable(new LicenceContactFilterForm());

    assertThat(tableView.tableJson())
        .contains("P 123")
        .contains(SHELL_U_K_LIMITED)
        .contains("Not assigned");
  }

  @Test
  void getIndustryContactsTable_whenLicenceReferenceFilter_showsOnlyMatchingRows() {
    var otherOrgLicensee = licenseeOnLicence(otherLicence());
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of(ORG_UNIT));
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(licensee(), otherOrgLicensee));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of());

    var filterForm = new LicenceContactFilterForm();
    filterForm.setLicenceReference("456");

    var tableView = licenceContactService.getIndustryContactsTable(user, true, filterForm);

    assertThat(tableView.tableJson())
        .contains("P 456")
        .doesNotContain("P 123");
    assertThat(tableView.contactCount()).isEqualTo(1);
  }

  @Test
  void getIndustryContactsTable_whenContactEmailFilter_showsOnlyRowsWithMatchingEmail() {
    var licenseeWithContact = licensee();
    var licenseeWithoutContact = licenseeOnLicence(otherLicence());
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of(ORG_UNIT));
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(licenseeWithContact, licenseeWithoutContact));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(contact(licenseeWithContact, "licensing@example.com")));

    var filterForm = new LicenceContactFilterForm();
    filterForm.setContactEmail("licensing@");

    var tableView = licenceContactService.getIndustryContactsTable(user, true, filterForm);

    assertThat(tableView.tableJson())
        .contains("P 123")
        .doesNotContain("P 456");
    assertThat(tableView.contactCount()).isEqualTo(1);
  }

  @Test
  void getIndustryContactsTable_whenNoContactAssignedFilter_showsOnlyRowsWithoutEmail() {
    var licenseeWithContact = licensee();
    var licenseeWithoutContact = licenseeOnLicence(otherLicence());
    when(licenceOrganisationService.getUsersOrgUnits(user)).thenReturn(List.of(ORG_UNIT));
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(licenseeWithContact, licenseeWithoutContact));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(contact(licenseeWithContact, "licensing@example.com")));

    var filterForm = new LicenceContactFilterForm();
    filterForm.setNoContactAssigned(true);

    var tableView = licenceContactService.getIndustryContactsTable(user, true, filterForm);

    assertThat(tableView.tableJson())
        .contains("P 456")
        .doesNotContain("P 123");
    assertThat(tableView.contactCount()).isEqualTo(1);
  }

  @Test
  void getRegulatorContactsTable_whenLicenseeOrgUnitFilter_showsOnlyThatOrganisation() {
    var shellLicensee = licensee();
    var otherOrgLicensee = licenseeForOrg(otherLicence(), 20);
    when(licenceResponsibleOrganisationService.getAll()).thenReturn(List.of(shellLicensee, otherOrgLicensee));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_ID, 20)))
        .thenReturn(Map.of(ORG_ID, SHELL_U_K_LIMITED, 20, "BP Exploration"));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(List.of(ORG_ID, 20)))
        .thenReturn(List.of());

    var filterForm = new LicenceContactFilterForm();
    filterForm.setLicenseeOrgUnitId(20);

    var tableView = licenceContactService.getRegulatorContactsTable(filterForm);

    assertThat(tableView.tableJson())
        .contains("BP Exploration")
        .doesNotContain(SHELL_U_K_LIMITED);
    assertThat(tableView.contactCount()).isEqualTo(1);
  }

  @Test
  void getRegulatorContactsTable_whenLicenseeOrgGroupFilter_showsOnlyGroupMembers() {
    var shellLicensee = licensee();
    var otherOrgLicensee = licenseeForOrg(otherLicence(), 20);
    when(licenceResponsibleOrganisationService.getAll()).thenReturn(List.of(shellLicensee, otherOrgLicensee));
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_ID, 20)))
        .thenReturn(Map.of(ORG_ID, SHELL_U_K_LIMITED, 20, "BP Exploration"));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(List.of(ORG_ID, 20)))
        .thenReturn(List.of());
    when(organisationGroupQueryService.getOrganisationUnitsByOrganisationGroupIds(List.of(55)))
        .thenReturn(List.of(ORG_UNIT));

    var filterForm = new LicenceContactFilterForm();
    filterForm.setLicenseeOrgGroupId(55);

    var tableView = licenceContactService.getRegulatorContactsTable(filterForm);

    assertThat(tableView.tableJson())
        .contains(SHELL_U_K_LIMITED)
        .doesNotContain("BP Exploration");
    assertThat(tableView.contactCount()).isEqualTo(1);
  }

  @Test
  void getPreselectedOrganisationUnit_whenIdProvided_returnsNameMap() {
    when(organisationUnitQueryService.getOrganisationUnitNamesByIds(List.of(ORG_ID)))
        .thenReturn(Map.of(ORG_ID, SHELL_U_K_LIMITED));

    var result = licenceContactService.getPreselectedOrganisationUnit(ORG_ID);

    assertThat(result).isEqualTo(Map.of(ORG_ID.toString(), SHELL_U_K_LIMITED));
  }

  @Test
  void getPreselectedOrganisationUnit_whenNullId_returnsEmptyMap() {
    var result = licenceContactService.getPreselectedOrganisationUnit(null);

    assertThat(result).isEmpty();
    verifyNoInteractions(organisationUnitQueryService);
  }

  @Test
  void getPreselectedOrganisationGroup_whenGroupExists_returnsNameMap() {
    var groupDto = new OrganisationGroupDto();
    groupDto.setOrganisationGroupId(55);
    groupDto.setOrganisationGroupName("Shell Group");
    when(organisationGroupQueryService.getOrganisationGroupById(55)).thenReturn(Optional.of(groupDto));

    var result = licenceContactService.getPreselectedOrganisationGroup(55);

    assertThat(result).isEqualTo(Map.of("55", "Shell Group"));
  }

  @Test
  void getPreselectedOrganisationGroup_whenNullId_returnsEmptyMap() {
    var result = licenceContactService.getPreselectedOrganisationGroup(null);

    assertThat(result).isEmpty();
    verifyNoInteractions(organisationGroupQueryService);
  }

  @Test
  void getLicenceContactFormView_whenOrganisationNotInUsersGroups_throwsForbidden() {
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, 999))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

    assertThatThrownBy(() -> licenceContactService.getLicenceContactFormView(user, LICENCE_ID, 999))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);

    verifyNoInteractions(licenceResponsibleOrganisationService, licenceContactRepository);
  }

  @Test
  void getLicenceContactFormView_whenLicenseeNotFound_throwsNotFound() {
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, ORG_ID)).thenReturn(SHELL_U_K_LIMITED);
    when(licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(LICENCE_ID, ORG_ID))
        .thenThrow(new LmsEntityNotFoundException("licence responsible organisation", "1/10"));

    assertThatThrownBy(() -> licenceContactService.getLicenceContactFormView(user, LICENCE_ID, ORG_ID))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }

  @Test
  void getLicenceContactFormViewWithCurrentEmail() {
    var licensee = licensee();
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, ORG_ID)).thenReturn(SHELL_U_K_LIMITED);
    when(licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(LICENCE_ID, ORG_ID))
        .thenReturn(licensee);
    when(licenceContactRepository.findByLicensee(licensee))
        .thenReturn(Optional.of(contact(licensee, "licensing@example.com")));

    var context = licenceContactService.getLicenceContactFormView(user, LICENCE_ID, ORG_ID);

    assertThat(context.licenceReference()).isEqualTo("P 123");
    assertThat(context.currentEmail()).isEqualTo("licensing@example.com");
    assertThat(context.isUpdate()).isTrue();
  }

  @Test
  void saveContact_whenExistingContact_updatesEmail() {
    var licensee = licensee();
    var existing = contact(licensee, "old@example.com");
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, ORG_ID)).thenReturn(SHELL_U_K_LIMITED);
    when(licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(LICENCE_ID, ORG_ID))
        .thenReturn(licensee);
    when(licenceContactRepository.findByLicensee(licensee)).thenReturn(Optional.of(existing));

    licenceContactService.saveContact(user, LICENCE_ID, ORG_ID, "new@example.com");

    verify(licenceContactRepository).save(existing);
    assertThat(existing.getContactEmail()).isEqualTo("new@example.com");
  }

  @Test
  void saveContact_whenOrganisationNotInUsersGroups_throwsForbiddenAndDoesNotSave() {
    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, 999))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

    assertThatThrownBy(() -> licenceContactService.saveContact(user, LICENCE_ID, 999, "x@example.com"))
        .isInstanceOf(ResponseStatusException.class);

    verifyNoInteractions(licenceResponsibleOrganisationService, licenceContactRepository);
  }

  @Test
  void getOtherLicencesHeldByLicensee_excludesCurrentLicence_andJoinsCurrentEmails() {
    var currentLicensee = licensee();
    var otherLicensee = licenseeOnLicence(otherLicence());

    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, ORG_ID)).thenReturn(SHELL_U_K_LIMITED);
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(currentLicensee, otherLicensee));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(contact(otherLicensee, "other@example.com")));

    var candidates = licenceContactService.getOtherLicencesHeldByLicensee(user, LICENCE_ID, ORG_ID);

    assertThat(candidates)
        .containsExactly(new BulkContactCandidate(2, "P 456", SHELL_U_K_LIMITED, "other@example.com"));
  }

  @Test
  void getAllLicenceIdsHeldByLicensee_returnsCurrentLicenceFirstThenOthers() {
    var currentLicensee = licensee();
    var otherLicensee = licenseeOnLicence(otherLicence());

    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, ORG_ID)).thenReturn(SHELL_U_K_LIMITED);
    when(licenceResponsibleOrganisationService.getAllByResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of(currentLicensee, otherLicensee));
    when(licenceContactRepository.findAllByLicensee_ResponsibleOrganisationIdIn(Set.of(ORG_ID)))
        .thenReturn(List.of());

    var licenceIds = licenceContactService.getAllLicenceIdsHeldByLicensee(user, LICENCE_ID, ORG_ID);

    assertThat(licenceIds).containsExactly(LICENCE_ID, 2);
  }

  @Test
  void applyContactToLicences_savesTheContactForEachLicence() {
    var licensee1 = licensee();
    var licensee2 = licenseeOnLicence(otherLicence());

    when(licenceOrganisationService.getScopedOrgUnitNameOrThrow(user, ORG_ID)).thenReturn(SHELL_U_K_LIMITED);
    when(licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(LICENCE_ID, ORG_ID))
        .thenReturn(licensee1);
    when(licenceResponsibleOrganisationService.getByLicenceIdAndResponsibleOrganisationIdOrThrow(2, ORG_ID))
        .thenReturn(licensee2);
    when(licenceContactRepository.findByLicensee(licensee1)).thenReturn(Optional.empty());
    when(licenceContactRepository.findByLicensee(licensee2)).thenReturn(Optional.empty());

    licenceContactService.applyContactToLicences(user, ORG_ID, "new@example.com", List.of(LICENCE_ID, 2));

    verify(licenceContactRepository, times(2)).save(contactCaptor.capture());
    assertThat(contactCaptor.getAllValues())
        .extracting(LicenceContact::getContactEmail)
        .containsExactly("new@example.com", "new@example.com");
  }

  private static Licence otherLicence() {
    return LicenceTestUtil.builder().withId(2).withLicenceReference("P 456").build();
  }

  private static LicenceResponsibleOrganisation licenseeOnLicence(Licence licence) {
    return licenseeForOrg(licence, ORG_ID);
  }

  private static LicenceResponsibleOrganisation licenseeForOrg(Licence licence, Integer organisationId) {
    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(licence);
    licensee.setResponsibleOrganisationId(organisationId);
    licensee.setManagedByLms(false);
    return licensee;
  }

  private static LicenceResponsibleOrganisation licensee() {
    var licensee = new LicenceResponsibleOrganisation();
    licensee.setLicence(LICENCE);
    licensee.setResponsibleOrganisationId(ORG_ID);
    licensee.setManagedByLms(false);
    return licensee;
  }

  private static LicenceContact contact(LicenceResponsibleOrganisation licensee, String email) {
    var contact = new LicenceContact();
    contact.setLicensee(licensee);
    contact.setContactEmail(email);
    return contact;
  }
}
