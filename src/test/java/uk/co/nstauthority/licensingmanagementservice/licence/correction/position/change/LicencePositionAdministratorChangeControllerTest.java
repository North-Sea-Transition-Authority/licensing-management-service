package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static uk.co.nstauthority.licensingmanagementservice.util.NotificationBannerTestUtil.notificationBanner;
import static uk.co.nstauthority.licensingmanagementservice.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindingResult;
import uk.co.nstauthority.licensingmanagementservice.AbstractControllerTest;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.CorrectionHasStatusInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.correction.InvokingUserCanViewCorrectionInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.AdministratorChangeForm;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.AdministratorChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayloadTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.AdministratorOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.AdministratorChangeContext;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@ContextConfiguration(classes = LicencePositionAdministratorChangeController.class)
@ActiveProfiles("test")
class LicencePositionAdministratorChangeControllerTest extends AbstractControllerTest {

  @MockitoBean
  private OrganisationChangeFormValidator organisationChangeFormValidator;

  @MockitoBean
  private OrganisationUnitQueryService organisationUnitQueryService;

  @MockitoBean
  private AdministratorChangeService administratorChangeService;

  @MockitoBean
  private OrganisationChangeService organisationChangeService;

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();
  private static final UUID POSITION_CORRECTION_ID = UUID.randomUUID();
  private static final Integer ADMINISTRATOR_ID = 123;
  private static final Integer PREVIOUS_ADMINISTRATOR_ID = 456;
  private static final Integer CURRENT_ADMINISTRATOR_ID = 789;
  private static final String PAGE_TITLE = "Change licence administrator";
  private static final String OPERATOR_TYPE = "licence administrator";
  private static final String VIEW_NAME = "lms/licence/correction/change/addOrganisationChange";
  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withId(CORRECTION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder()
      .withId(POSITION_ID)
      .withLicence(LICENCE)
      .build();
  private static final LicencePositionCorrection POSITION_CORRECTION = LicencePositionCorrectionTestUtil.newBuilder()
      .withId(POSITION_CORRECTION_ID)
      .withLicenceCorrection(CORRECTION)
      .build();

  private final String executedCancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION, null));

  private final String addedCancelUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION, POSITION_CORRECTION, null));

  private final String executedRedirectUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderLicencePosition(CORRECTION, POSITION, null));

  private final String addedRedirectUrl = ReverseRouter.route(on(LicenceCorrectionController.class)
      .renderAddedPosition(CORRECTION, POSITION_CORRECTION, null));

  @Test
  void renderForExecutedPosition_whenNotLoggedIn() throws Exception {
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION))))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void renderForExecutedPosition_whenNotAllocatedToUser() throws Exception {
    givenCorrectionNotAllocatedToUser();

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            InvokingUserCanViewCorrectionInterceptorRule.notAssignedMessage(CORRECTION_ID, regulatorUser.wuaId()))
        );
  }

  @Test
  void renderForExecutedPosition_whenCarbonStorageLicence_forbidden() throws Exception {
    var carbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(carbonStorageLicence)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(CORRECTION, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForExecutedPosition_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(correction, POSITION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void renderForExecutedPosition_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();
    var form = new AdministratorChangeForm(CURRENT_ADMINISTRATOR_ID.toString());

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(organisationChangeService.getOrganisationChangeContext(correction, position.getId(), AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(
            CURRENT_ADMINISTRATOR_ID, PREVIOUS_ADMINISTRATOR_ID, "Current Admin Org", "Previous Admin Org"));
    when(organisationChangeService.populateExistingOrganisationForm(eq(CURRENT_ADMINISTRATOR_ID), eq(correction), eq(position), any()))
        .thenReturn(form);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForExecutedPosition(correction, position)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("operatorType", OPERATOR_TYPE),
            model().attribute("form", form),
            model().attribute("cancelUrl", executedCancelUrl),
            model().attribute("previousOperatorName", "Previous Admin Org"),
            model().attributeExists("organisationUnitsUrl")
        );
  }

  @Test
  void submitForExecutedPosition_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new AdministratorChangeForm();
    form.getId().setInputValue(ADMINISTRATOR_ID.toString());

    when(organisationChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_ADMINISTRATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(false);
    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(
            CURRENT_ADMINISTRATOR_ID, PREVIOUS_ADMINISTRATOR_ID, "Current Admin Org", "Previous Admin Org"));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForExecutedPosition(correction, position, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedRedirectUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence administrator change added")
                .build())
        );

    var administratorOperation = LicenceOperation.newAdministratorChange()
        .withOperator(ADMINISTRATOR_ID)
        .build();
    verify(organisationChangeService).addOrganisationChangeForExistingLicencePosition(
        position,
        correction,
        AdministratorOperation.class,
        administratorOperation
    );
  }

  @Test
  void submitForExecutedPosition_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new AdministratorChangeForm();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(CURRENT_ADMINISTRATOR_ID, PREVIOUS_ADMINISTRATOR_ID, "", ""));
    when(organisationChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_ADMINISTRATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForExecutedPosition(correction, position, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("cancelUrl", executedCancelUrl)
        );

    verify(organisationChangeService, never()).addOrganisationChangeForExistingLicencePosition(any(), any(), any(), any());
  }

  @Test
  void submitForExecutedPosition_whenCorrectionIsNotInProgress() throws Exception {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withStatus(LicenceCorrectionStatus.COMPLETE)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForExecutedPosition(correction, POSITION, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(status().reason(
            CorrectionHasStatusInterceptorRule.notInExpectedStatusMessage(CORRECTION_ID)
        ));
  }

  @Test
  void renderForAddedPosition_whenCarbonStorageLicence_forbidden() throws Exception {
    var carbonStorageLicence = LicenceTestUtil.builder().withLicenceType(LicenceType.CARBON_STORAGE).build();
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(carbonStorageLicence)
        .build();

    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(CORRECTION, POSITION_CORRECTION)))
            .with(user(regulatorUser)))
        .andExpect(status().isForbidden());
  }

  @Test
  void renderForAddedPosition_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder().withLicencePositionId(POSITION_ID.toString()).build())
        .build();
    var form = new AdministratorChangeForm(CURRENT_ADMINISTRATOR_ID.toString());
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(positionCorrection));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(CURRENT_ADMINISTRATOR_ID, null, "", ""));
    when(organisationChangeService.populateAddedOrganisationForm(eq(CURRENT_ADMINISTRATOR_ID), eq(positionCorrection),  any()))
        .thenReturn(form);

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForAddedPosition(correction, positionCorrection)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attribute("form", form),
            model().attribute("cancelUrl", addedCancelUrl),
            model().attribute("previousOperatorName", ""),
            model().attributeExists("organisationUnitsUrl")
        );
  }

  @Test
  void submitForAddedPosition_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var form = new AdministratorChangeForm();
    form.getId().setInputValue(ADMINISTRATOR_ID.toString());

    when(organisationChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_ADMINISTRATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(false);
    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(positionCorrection));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(CURRENT_ADMINISTRATOR_ID, PREVIOUS_ADMINISTRATOR_ID, "", ""));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(addedRedirectUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence administrator change added")
                .build())
        );

    var administratorOperation = LicenceOperation.newAdministratorChange()
        .withOperator(ADMINISTRATOR_ID)
        .build();
    verify(organisationChangeService).addOrganisationChangeForAddedLicencePosition(
        positionCorrection,
        AdministratorOperation.class,
        administratorOperation
    );
  }

  @Test
  void submitForAddedPosition_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withId(POSITION_CORRECTION_ID)
        .withLicenceCorrection(correction)
        .withPayload(CreateLicencePositionPayloadTestUtil.newBuilder()
            .withLicencePositionId(POSITION_ID.toString())
            .build())
        .build();

    var form = new AdministratorChangeForm();

    when(licencePositionCorrectionService.findById(POSITION_CORRECTION_ID))
        .thenReturn(Optional.of(positionCorrection));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(CURRENT_ADMINISTRATOR_ID, PREVIOUS_ADMINISTRATOR_ID, "", ""));
    when(organisationChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_ADMINISTRATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForAddedPosition(correction, positionCorrection, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("cancelUrl", addedCancelUrl)
        );

    verify(organisationChangeService, never()).addOrganisationChangeForAddedLicencePosition(any(), any(), any());
  }

  @Test
  void renderForCorrectingChange_whenAllocatedToUser() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var change = LicencePositionChangeTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionChangeService.findById(change.getId())).thenReturn(Optional.of(change));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(
            CURRENT_ADMINISTRATOR_ID, PREVIOUS_ADMINISTRATOR_ID, "", "Previous Admin Org"));

    mockMvc.perform(get(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .renderForCorrectingChange(correction, position, change)))
            .with(user(regulatorUser)))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("pageTitle", PAGE_TITLE),
            model().attributeExists("form"),
            model().attribute("cancelUrl", executedCancelUrl),
            model().attribute("previousOperatorName", "Previous Admin Org"),
            model().attributeExists("organisationUnitsUrl")
        );
  }

  @Test
  void submitForCorrectingChange_whenValid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var change = LicencePositionChangeTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new AdministratorChangeForm();
    form.getId().setInputValue(ADMINISTRATOR_ID.toString());

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionChangeService.findById(change.getId())).thenReturn(Optional.of(change));
    when(organisationChangeFormValidator.hasErrors(
        eq(form), any(BindingResult.class), eq(PREVIOUS_ADMINISTRATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(false);
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(CURRENT_ADMINISTRATOR_ID, PREVIOUS_ADMINISTRATOR_ID, "", ""));

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForCorrectingChange(correction, position, change, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().is3xxRedirection(),
            redirectedUrl(executedRedirectUrl),
            notificationBanner(NotificationBanner.newSuccessBanner()
                .withHeadingContent("Licence administrator change corrected")
                .build())
        );

    verify(administratorChangeService)
        .correctExistingAdministratorChange(position, correction, change.getId().toString(), ADMINISTRATOR_ID);
  }

  @Test
  void submitForCorrectingChange_whenInvalid() throws Exception {
    var correction = givenCorrectionAllocatedToUser();
    var change = LicencePositionChangeTestUtil.newBuilder().withId(UUID.randomUUID()).build();
    var position = LicencePositionTestUtil.newBuilder().withId(POSITION_ID).withLicence(LICENCE).build();

    var form = new AdministratorChangeForm();

    when(licencePositionService.getPositionForLicence(LICENCE, POSITION_ID)).thenReturn(position);
    when(licencePositionService.findById(POSITION_ID)).thenReturn(Optional.of(position));
    when(licencePositionChangeService.findById(change.getId())).thenReturn(Optional.of(change));
    when(organisationChangeService.getOrganisationChangeContext(correction, POSITION_ID, AdministratorOperation.class))
        .thenReturn(new AdministratorChangeContext(ADMINISTRATOR_ID, PREVIOUS_ADMINISTRATOR_ID, "", ""));
    when(organisationChangeFormValidator.hasErrors(eq(form), any(BindingResult.class), eq(PREVIOUS_ADMINISTRATOR_ID), eq(OPERATOR_TYPE)))
        .thenReturn(true);

    mockMvc.perform(post(ReverseRouter.route(on(LicencePositionAdministratorChangeController.class)
            .submitForCorrectingChange(correction, position, change, null, null, null)))
            .with(user(regulatorUser))
            .with(csrf())
            .flashAttr("form", form))
        .andExpectAll(
            status().isOk(),
            view().name(VIEW_NAME),
            model().attribute("form", form),
            model().attribute("cancelUrl", executedCancelUrl)
        );

    verify(administratorChangeService, never()).correctExistingAdministratorChange(any(), any(), any(), any());
  }

  private LicenceCorrection givenCorrectionAllocatedToUser() {
    var correction = LicenceCorrectionTestUtil.newBuilder()
        .withId(CORRECTION_ID)
        .withLicence(LICENCE)
        .build();
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.of(correction));
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    return correction;
  }

  private void givenCorrectionNotAllocatedToUser() {
    when(licenceCorrectionService.findByIdAndAllocatedToWuaId(CORRECTION_ID, regulatorUser))
        .thenReturn(Optional.empty());
  }
}
