package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.authentication.TestUserProvider.user;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.Errors;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.fds.table.SortableTableView;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ContextConfiguration(classes = LicenceContactController.class)
class LicenceContactControllerTest extends AbstractControllerTest {

  private static final Integer LICENCE_ID = 1;
  private static final Integer ORG_ID = 10;
  private static final String MANAGE_CONTACTS_ROUTE =
      ReverseRouter.route(on(LicenceContactController.class).renderManageContacts(null, null));
  private static final String CLEAR_FILTERS_ROUTE =
      ReverseRouter.route(on(LicenceContactController.class).clearContactFilters(null, null));

  @MockitoBean
  private LicenceContactService licenceContactService;

  @MockitoBean
  private LicenceContactFormValidator licenceContactFormValidator;

  @MockitoBean
  private RegulatorRoleService regulatorRoleService;

  private ServiceUserDetail contactManager;

  @BeforeEach
  void setUp() {
    contactManager = ServiceUserDetailTestUtil.newBuilder().withWuaId(200L).build();
    when(teamQueryService.userHasRoleInTeamType(
        contactManager.wuaId(), TeamType.ORGANISATION, Set.of(Role.LICENSEE_CONTACTS_MANAGER)))
        .thenReturn(true);
  }

  @Test
  void renderManageContacts_whenNotLoggedIn_redirectsToLogin() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicenceContactController.class).renderManageContacts(null, null))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderManageContacts_whenRegulator_rendersAllContacts() throws Exception {
    var regulator = ServiceUserDetailTestUtil.newBuilder().withWuaId(400L).build();
    var tableJson = SortableTableView.sortableTableBuilder()
        .newWithHeadings("Licence", "Licensee", "Contact email")
        .build()
        .toString();
    var form = new LicenceContactFilterForm();
    var filterSession = new LicenceContactFilterSession(form);
    when(regulatorRoleService.isRegulator(regulator)).thenReturn(true);
    when(licenceContactService.getRegulatorContactsTable(form))
        .thenReturn(new LicenceContactsTableView(tableJson, 2));

    mockMvc.perform(get(ReverseRouter.route(on(LicenceContactController.class).renderManageContacts(null, null)))
            .flashAttr("licenceContactFilterSession", filterSession)
            .with(user(regulator)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/contact/manageContacts"),
            model().attribute("contactsTableJson", tableJson),
            model().attribute("contactCount", 2),
            model().attribute("form", form),
            model().attribute("isRegulatorUser", true),
            model().attribute("clearFilterUrl", CLEAR_FILTERS_ROUTE),
            model().attribute("pageTitle", "Licence contact details"));
  }

  @Test
  void renderManageContacts_whenAuthorised_rendersTable() throws Exception {
    var tableJson = SortableTableView.sortableTableBuilder()
        .newWithHeadings("Licence", "Licensee", "Contact email")
        .build()
        .toString();
    var form = new LicenceContactFilterForm();
    var filterSession = new LicenceContactFilterSession(form);
    when(licenceContactService.getIndustryContactsTable(contactManager, true, form))
        .thenReturn(new LicenceContactsTableView(tableJson, 1));

    mockMvc.perform(get(ReverseRouter.route(on(LicenceContactController.class).renderManageContacts(null, null)))
            .flashAttr("licenceContactFilterSession", filterSession)
            .with(user(contactManager)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/contact/manageContacts"),
            model().attribute("contactsTableJson", tableJson),
            model().attribute("contactCount", 1),
            model().attribute("isRegulatorUser", false),
            model().attribute("pageTitle", "Manage licence contact details"));
  }

  @Test
  void renderManageContacts_whenOrgMemberWithoutManagerRole_canView() throws Exception {
    var viewer = ServiceUserDetailTestUtil.newBuilder().withWuaId(250L).build();
    var tableJson = SortableTableView.sortableTableBuilder()
        .newWithHeadings("Licence", "Licensee", "Contact email")
        .build()
        .toString();
    var form = new LicenceContactFilterForm();
    var filterSession = new LicenceContactFilterSession(form);
    when(licenceContactService.getIndustryContactsTable(viewer, false, form))
        .thenReturn(new LicenceContactsTableView(tableJson, 0));

    mockMvc.perform(get(ReverseRouter.route(on(LicenceContactController.class).renderManageContacts(null, null)))
            .flashAttr("licenceContactFilterSession", filterSession)
            .with(user(viewer)))
        .andExpectAll(
            status().isOk(),
            model().attribute("pageTitle", "Licence contact details"));
  }

  @Test
  void filterContacts_updatesSessionAndRedirects() throws Exception {
    var form = new LicenceContactFilterForm();
    var filterSession = new LicenceContactFilterSession(form);

    mockMvc.perform(post(ReverseRouter.route(on(LicenceContactController.class).filterContacts(null, null)))
            .param("licenceReference", "P 123")
            .flashAttr("licenceContactFilterSession", filterSession)
            .with(user(contactManager))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(MANAGE_CONTACTS_ROUTE));

    assertThat(filterSession.getFilterForm().getLicenceReference()).isEqualTo("P 123");
  }

  @Test
  void clearContactFilters_redirectsToManageContacts() throws Exception {
    mockMvc.perform(get(CLEAR_FILTERS_ROUTE)
            .with(user(contactManager)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(MANAGE_CONTACTS_ROUTE));
  }

  @Test
  void renderUpdateContact_whenNotManager_isForbidden() throws Exception {
    var viewer = ServiceUserDetailTestUtil.newBuilder().withWuaId(250L).build();
    mockMvc.perform(get(ReverseRouter.route(
            on(LicenceContactController.class).renderUpdateContact(LICENCE_ID, ORG_ID, null)))
            .with(user(viewer)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderUpdateContact_whenAuthorised_rendersForm() throws Exception {
    when(licenceContactService.getLicenceContactFormView(contactManager, LICENCE_ID, ORG_ID))
        .thenReturn(new LicenceContactFormView("P 123", "licensing@example.com"));

    mockMvc.perform(get(ReverseRouter.route(
            on(LicenceContactController.class).renderUpdateContact(LICENCE_ID, ORG_ID, null)))
            .with(user(contactManager)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/contact/updateContact"),
            model().attributeExists("form", "isUpdate", "licenceReference"));
  }

  @Test
  void renderUpdateContact_withOtherLicences_rendersApplyToOthersCheckboxes() throws Exception {
    when(licenceContactService.getLicenceContactFormView(contactManager, LICENCE_ID, ORG_ID))
        .thenReturn(new LicenceContactFormView("P 123", "licensing@example.com"));
    when(licenceContactService.getOtherLicencesHeldByLicensee(contactManager, LICENCE_ID, ORG_ID))
        .thenReturn(List.of(new BulkContactCandidate(2, "P 456", "Shell U.K. Limited", "other@example.com")));

    mockMvc.perform(get(ReverseRouter.route(
            on(LicenceContactController.class).renderUpdateContact(LICENCE_ID, ORG_ID, null)))
            .with(user(contactManager)))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/contact/updateContact"),
            model().attributeExists("otherLicences"));
  }

  @Test
  void saveContact_whenValid_savesAndRedirectsToList() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(
            on(LicenceContactController.class).saveContact(LICENCE_ID, ORG_ID, null, null, null, null)))
            .param("contactEmail", "licensing@example.com")
            .with(user(contactManager))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(MANAGE_CONTACTS_ROUTE));

    verify(licenceContactService)
        .applyContactToLicences(contactManager, ORG_ID, "licensing@example.com", List.of(LICENCE_ID));
  }

  @Test
  void saveContact_whenOtherLicencesSelected_appliesToCurrentAndSelected() throws Exception {
    mockMvc.perform(post(ReverseRouter.route(
            on(LicenceContactController.class).saveContact(LICENCE_ID, ORG_ID, null, null, null, null)))
            .param("contactEmail", "licensing@example.com")
            .param("bulkUpdateLicenceIds", "2")
            .with(user(contactManager))
            .with(csrf()))
        .andExpect(status().is3xxRedirection());

    verify(licenceContactService)
        .applyContactToLicences(contactManager, ORG_ID, "licensing@example.com", List.of(LICENCE_ID, 2));
  }

  @Test
  void saveContact_whenInvalid_reRendersFormAndDoesNotSave() throws Exception {
    doAnswer(invocation -> {
      Errors errors = invocation.getArgument(1);
      errors.rejectValue("contactEmail", "contactEmail.invalid", "Enter a valid email");
      return null;
    }).when(licenceContactFormValidator).isValid(any(), any());

    when(licenceContactService.getLicenceContactFormView(contactManager, LICENCE_ID, ORG_ID))
        .thenReturn(new LicenceContactFormView("P 123", null));

    mockMvc.perform(post(ReverseRouter.route(
            on(LicenceContactController.class).saveContact(LICENCE_ID, ORG_ID, null, null, null, null)))
            .param("contactEmail", "not-an-email")
            .with(user(contactManager))
            .with(csrf()))
        .andExpectAll(
            status().isOk(),
            view().name("lms/licence/contact/updateContact"));

    verify(licenceContactService, never()).applyContactToLicences(any(), any(), any(), any());
  }
}
